require('dotenv').config();
const express    = require('express');
const cors       = require('cors');
const WebSocket  = require('ws');
const mongoose   = require('mongoose');
const bcrypt     = require('bcryptjs');
const jwt        = require('jsonwebtoken');
const multer     = require('multer');
const path       = require('path');
const { spawn }  = require('child_process');
const fs         = require('fs');
const crypto     = require('crypto');
const os         = require('os');

const User = require('./models/User');

// ── Config ───────────────────────────────────────────────────────────────────
const HTTP_PORT  = process.env.HTTP_PORT  || 3000;
const WS_PORT    = process.env.WS_PORT    || 8080;
const MONGODB_URI = process.env.MONGODB_URI || '';
const MONGODB_DB  = process.env.MONGODB_DB  || 'intellinflate_db';
const JWT_SECRET  = process.env.JWT_SECRET  || 'supersecretkey';
const PYTHON_SERVICES_DIR = process.env.PYTHON_SERVICES_DIR || path.join(__dirname, 'python-services');
let currentHttpPort = Number(HTTP_PORT);
let currentWsPort = Number(WS_PORT);

let mongoReady = false;
const memoryUsers = new Map();

function log(level, message, meta = {}) {
    const payload = {
        level,
        message,
        ts: new Date().toISOString(),
        ...meta
    };
    const line = JSON.stringify(payload);
    if (level === 'error') {
        console.error(line);
        return;
    }
    if (level === 'warn') {
        console.warn(line);
        return;
    }
    console.log(line);
}

function getNetworkIPv4Addresses() {
    const interfaces = os.networkInterfaces();
    const ips = [];

    for (const ifaceName of Object.keys(interfaces)) {
        const entries = interfaces[ifaceName] || [];
        for (const addr of entries) {
            if (addr && addr.family === 'IPv4' && !addr.internal) {
                ips.push(addr.address);
            }
        }
    }

    return [...new Set(ips)];
}

function logServiceUrls(httpPort, wsPort) {
    const networkIps = getNetworkIPv4Addresses();
    const frontendUrl = `http://localhost:${httpPort}`;
    const apiBaseUrl = `http://localhost:${httpPort}/api`;
    const healthUrl = `http://localhost:${httpPort}/health`;
    const websocketUrl = `ws://localhost:${wsPort}`;
    const networkFrontendUrls = networkIps.map((ip) => `http://${ip}:${httpPort}`);
    const networkApiUrls = networkIps.map((ip) => `http://${ip}:${httpPort}/api`);

    log('info', 'Service URLs', {
        frontendUrl,
        apiBaseUrl,
        healthUrl,
        websocketUrl,
        networkFrontendUrls,
        networkApiUrls
    });
}

function getVisionModelStack() {
    return {
        project: 'intellinflate',
        tasks: {
            numberPlateUserDetection: {
                endpoint: '/api/detect',
                mode: 'PLATE',
                approach: 'Contour-based plate region + EasyOCR (pretrained OCR)'
            },
            tireSideAngleCrackDetection: {
                endpoint: '/api/analyze-tire',
                mode: 'SIDE',
                approach: 'MobileNetV2/EfficientNet transfer-learning classifier (fallback: OpenCV contour heuristics)'
            },
            tireFrontAngleTreadAlignment: {
                endpoint: '/api/analyze-tire',
                mode: 'FRONT',
                approach: 'MobileNetV2/ResNet tread classifier + OpenCV strip-based misalignment score'
            }
        },
        recommendation: {
            crackModel: 'MobileNetV2/EfficientNet transfer learning',
            treadModel: 'MobileNetV2/ResNet transfer learning',
            plateDetection: 'YOLOv5/YOLOv8 + EasyOCR',
            note: 'Run inference on server; ESP32-CAM captures images only.'
        }
    };
}

// ── MongoDB (Mongoose) ────────────────────────────────────────────────────────
async function connectMongo() {
    if (!MONGODB_URI || MONGODB_URI.includes('<username>')) {
        log('warn', 'MongoDB URI not set/invalid, using in-memory fallback');
        return;
    }
    try {
        await mongoose.connect(MONGODB_URI, {
            dbName: MONGODB_DB,
            family: 4,
            serverSelectionTimeoutMS: 30000,
            connectTimeoutMS: 30000
        });
        mongoReady = true;
        log('info', 'MongoDB connected', { db: MONGODB_DB });
    } catch (err) {
        mongoReady = false;
        log('error', 'MongoDB connection failed, using in-memory fallback', { error: err.message });
    }
}

// ── Multer Config (for uploads) ────────────────────────────────────────────────
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => cb(null, `${Date.now()}-${file.originalname}`)
});
const upload = multer({
    storage,
    limits: { fileSize: 10 * 1024 * 1024 },
    fileFilter: (req, file, cb) => {
        if (!file.mimetype || !file.mimetype.startsWith('image/')) {
            return cb(new Error('Only image uploads are supported.'));
        }
        cb(null, true);
    }
});

// ── Express HTTP server ───────────────────────────────────────────────────────
const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use('/uploads', express.static(uploadDir));

app.use((req, res, next) => {
    req.requestId = crypto.randomUUID();
    res.setHeader('x-request-id', req.requestId);
    req.startTs = Date.now();
    next();
});

app.use((req, res, next) => {
    res.on('finish', () => {
        log('info', 'HTTP request', {
            requestId: req.requestId,
            method: req.method,
            path: req.path,
            status: res.statusCode,
            ms: Date.now() - req.startTs
        });
    });
    next();
});

// Serve frontend static files
const frontendDir = path.join(__dirname, 'public');
app.use(express.static(frontendDir));

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        db: mongoose.connection.readyState === 1 ? 'connected' : 'fallback-memory',
        memoryUsers: memoryUsers.size,
        ts: Date.now()
    });
});

app.get('/api/model-stack', (req, res) => {
    res.json({ success: true, ...getVisionModelStack() });
});

function validateRequired(fields) {
    return (req, res, next) => {
        const missing = fields.filter((field) => !req.body?.[field] || String(req.body[field]).trim() === '');
        if (missing.length > 0) {
            return res.status(400).json({ error: `Missing required fields: ${missing.join(', ')}` });
        }
        next();
    };
}

function normalizePlate(value = '') {
    return value.toUpperCase().replace(/\s/g, '');
}

async function findUserByEmailOrPlate(email, numberPlate) {
    const normalizedPlate = normalizePlate(numberPlate);
    if (mongoReady) {
        return User.findOne({ $or: [{ email }, { numberPlate: normalizedPlate }] });
    }
    for (const user of memoryUsers.values()) {
        if (user.email === email || user.numberPlate === normalizedPlate) {
            return user;
        }
    }
    return null;
}

async function saveUser(userPayload) {
    if (mongoReady) {
        const doc = new User(userPayload);
        await doc.save();
        return doc;
    }
    const id = crypto.randomUUID();
    const user = { ...userPayload, _id: id };
    memoryUsers.set(id, user);
    return user;
}

/**
 * POST /api/register
 * User registration with number plate and essential details.
 */
app.post('/api/register', async (req, res) => {
    try {
        const { username, email, password, numberPlate, vehicleModel, phone } = req.body;
        log('info', 'Registration attempt', { requestId: req.requestId, email, numberPlate });
        
        // Basic validation
        if (!username || !email || !password || !numberPlate) {
            return res.status(400).json({ error: 'All essential details are required.' });
        }

        const normalizedPlate = normalizePlate(numberPlate);

        // Check for existing user
        const existingUser = await findUserByEmailOrPlate(email, normalizedPlate);
        if (existingUser) {
            return res.status(400).json({ error: 'User with this email or number plate already exists.' });
        }

        // Hash password
        const hashedPassword = await bcrypt.hash(password, 10);

        // Create user
        const newUser = await saveUser({
            username,
            email,
            password: hashedPassword,
            numberPlate: normalizedPlate,
            vehicleModel,
            phone
        });

        res.status(201).json({ 
            message: 'User registered successfully!', 
            user: {
                id: newUser._id,
                username: newUser.username,
                email: newUser.email,
                numberPlate: newUser.numberPlate,
                vehicleModel: newUser.vehicleModel,
                phone: newUser.phone
            }
        });
    } catch (err) {
        log('error', 'Registration error', { requestId: req.requestId, error: err.message });
        res.status(500).json({ error: 'Internal server error.' });
    }
});

/**
 * POST /api/login
 * User login with email or number plate.
 */
app.post('/api/login', validateRequired(['identifier', 'password']), async (req, res) => {
    try {
        const { identifier, password } = req.body;

        const normalizedIdentifier = identifier.includes('@') ? identifier : normalizePlate(identifier);

        let user;
        if (mongoReady) {
            user = await User.findOne({ 
                $or: [
                    { email: identifier }, 
                    { numberPlate: normalizedIdentifier }
                ] 
            });
        } else {
            for (const candidate of memoryUsers.values()) {
                if (candidate.email === identifier || candidate.numberPlate === normalizedIdentifier) {
                    user = candidate;
                    break;
                }
            }
        }

        if (!user) {
            return res.status(401).json({ error: 'Invalid credentials.' });
        }

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(401).json({ error: 'Invalid credentials.' });
        }

        const token = jwt.sign({ id: user._id }, JWT_SECRET, { expiresIn: '1d' });

        res.json({
            message: 'Login successful',
            token,
            user: {
                id: user._id,
                username: user.username,
                email: user.email,
                numberPlate: user.numberPlate,
                vehicleModel: user.vehicleModel,
                phone: user.phone
            }
        });
    } catch (err) {
        log('error', 'Login error', { requestId: req.requestId, error: err.message });
        res.status(500).json({ error: 'Internal server error.' });
    }
});

function runPythonScript(scriptName, args, onComplete, onFailure) {
    const scriptPath = path.join(PYTHON_SERVICES_DIR, scriptName);
    const preferredPython = process.env.PYTHON_EXECUTABLE || '';
    const venv311Python = path.join(PYTHON_SERVICES_DIR, '.venv311', 'bin', 'python');
    const legacyVenvPython = path.join(PYTHON_SERVICES_DIR, 'venv', 'bin', 'python3');
    const pythonCandidates = [preferredPython, venv311Python, legacyVenvPython, 'python3'].filter(Boolean);
    const pythonCmd = pythonCandidates.find((candidate) => candidate === 'python3' || fs.existsSync(candidate)) || 'python3';

    const pythonProcess = spawn(pythonCmd, [scriptPath, ...args]);
    let outputData = '';
    let errorData = '';

    pythonProcess.stdout.on('data', (data) => {
        outputData += data.toString();
    });

    pythonProcess.stderr.on('data', (data) => {
        errorData += data.toString();
    });

    pythonProcess.on('close', (code) => {
        if (code !== 0) {
            onFailure(new Error(errorData || `Python process exited with code ${code}`));
            return;
        }
        onComplete(outputData);
    });
}

/**
 * POST /api/analyze-tire
 * Endpoint to perform specific tire health analysis using Python.
 * Supports modes: SIDE (for cracks), FRONT (for tread/alignment).
 */
app.post('/api/analyze-tire', upload.single('image'), async (req, res) => {
    if (!req.file) return res.status(400).json({ error: 'Image file is required.' });
    
    const mode = (req.body.mode || 'FRONT').toUpperCase();
    if (!['FRONT', 'SIDE'].includes(mode)) {
        return res.status(400).json({ error: 'Invalid mode. Use FRONT or SIDE.' });
    }

    const imagePath = req.file.path;

    runPythonScript(
        'tire_analysis.py',
        ['-i', imagePath, '-m', mode],
        (outputData) => {
            try {
                const result = JSON.parse(outputData);
                return res.json(result);
            } catch (e) {
                log('error', 'Failed to parse tire analysis output', {
                    requestId: req.requestId,
                    mode,
                    outputPreview: outputData.slice(0, 300)
                });
                return res.status(500).json({ error: 'Failed to parse analysis results.' });
            }
        },
        (error) => {
            log('error', 'Python tire analysis failed', { requestId: req.requestId, mode, error: error.message });
            return res.status(500).json({ error: `Python ${mode} analysis script failed.` });
        }
    );
});

/**
 * POST /api/vehicle/detect
 * Mobile app endpoint to trigger vehicle detection via Python.
 */
app.post('/api/vehicle/detect', upload.single('image'), async (req, res) => {
    // Reuse logic from /api/detect
    detectVehicle(req, res);
});

/**
 * POST /api/detect
 * Legacy/Alternative endpoint for vehicle detection.
 */
app.post('/api/detect', upload.single('image'), async (req, res) => {
    detectVehicle(req, res);
});

async function detectVehicle(req, res) {
    if (!req.file) return res.status(400).json({ error: 'Image file is required.' });

    const imagePath = req.file.path;

    runPythonScript(
        'number_plate_detection.py',
        ['-i', imagePath],
        async (outputData) => {
            let detectedPlate = null;
            let confidence = 0;

            try {
                const parsed = JSON.parse(outputData);
                if (parsed?.success && parsed?.text) {
                    detectedPlate = normalizePlate(parsed.text);
                    confidence = Number(parsed.confidence || 0);
                }
            } catch (err) {
                const match = outputData.match(/Detected: ([\w\s-]+) \(Confidence: ([\d.]+)\)/);
                if (match) {
                    detectedPlate = normalizePlate(match[1]);
                    confidence = parseFloat(match[2]);
                }
            }

            if (!detectedPlate) {
                return res.status(404).json({
                    success: false,
                    message: 'No number plate detected.',
                    rawOutput: outputData.trim()
                });
            }

            let user = null;
            if (mongoReady) {
                user = await User.findOne({ numberPlate: detectedPlate });
            } else {
                for (const candidate of memoryUsers.values()) {
                    if (candidate.numberPlate === detectedPlate) {
                        user = candidate;
                        break;
                    }
                }
            }

            return res.json({
                stationId: "STATION-001",
                sessionId: crypto.randomUUID(),
                success: true,
                detectedPlate,
                confidence,
                user: user ? {
                    username: user.username,
                    email: user.email,
                    vehicleModel: user.vehicleModel,
                    phone: user.phone
                } : null
            });
        },
        (error) => {
            log('error', 'Python vehicle detection failed', { requestId: req.requestId, error: error.message });
            return res.status(500).json({ error: 'Python detection script failed.' });
        }
    );
}

/**
 * GET /api/station/info
 * Mobile app connection test endpoint.
 */
app.get('/api/station/info', (req, res) => {
    res.json({
        stationId: "STATION-001",
        name: "IntellInflate Hub",
        location: "Main Workshop",
        ipAddress: req.ip,
        status: "ACTIVE",
        capabilities: ["VEHICLE_DETECTION", "TIRE_SCAN"]
    });
});

// ── Legacy API routes ─────────────────────────────────────────────────────────

app.post('/api/scan', async (req, res) => {
    const data = req.body;
    // ... logic for tire scan ...
    res.status(201).json({ ok: true });
});

app.get('/api/ping', (req, res) => {
    res.json({ pong: true, timestamp: Date.now() });
});

// ── Server Listeners with Fallback ───────────────────────────────────────────

function startExpressServer(port) {
    const server = app.listen(port, '0.0.0.0', () => {
        currentHttpPort = Number(port);
        log('info', 'HTTP server started', { port });
        logServiceUrls(currentHttpPort, currentWsPort);
    });

    server.on('error', (err) => {
        if (err.code === 'EADDRINUSE') {
            log('warn', 'HTTP port in use, trying next port', { port, nextPort: Number(port) + 1 });
            startExpressServer(Number(port) + 1);
        } else {
            log('error', 'Express server error', { error: err.message });
        }
    });
}

let connectedClients = new Set();
let wss;

function startWebSocketServer(port) {
    wss = new WebSocket.Server({ port });

    wss.on('listening', () => {
        currentWsPort = Number(port);
        log('info', 'WebSocket server started', { port });
        logServiceUrls(currentHttpPort, currentWsPort);
    });

    wss.on('error', (err) => {
        if (err.code === 'EADDRINUSE') {
            log('warn', 'WebSocket port in use, trying next port', { port, nextPort: Number(port) + 1 });
            startWebSocketServer(Number(port) + 1);
        } else {
            log('error', 'WebSocket server error', { error: err.message });
        }
    });

    wss.on('connection', (ws) => {
        log('info', 'WS client connected');
        connectedClients.add(ws);
        ws.send(JSON.stringify({ type: 'connection', message: 'Connected to IntellInflate Server', ts: Date.now() }));
        
        ws.on('close', () => connectedClients.delete(ws));
        ws.on('error', () => connectedClients.delete(ws));
    });
}

// Start Servers
startExpressServer(HTTP_PORT);
startWebSocketServer(WS_PORT);

function broadcastToClients(data) {
    if (!wss) return;
    const msg = JSON.stringify(data);
    connectedClients.forEach(c => { if (c.readyState === WebSocket.OPEN) c.send(msg); });
}

// ── Simulation mode (no hardware) ─────────────────────────────────────────────
log('info', 'Simulation mode active', { source: 'simulated-esp32' });
const positions = ['FRONT_LEFT', 'FRONT_RIGHT', 'REAR_LEFT', 'REAR_RIGHT'];
setInterval(() => {
    broadcastToClients({
        type: 'demo',
        tires: positions.map(pos => ({
            pos,
            pressure: +(30 + Math.random() * 5).toFixed(1),
            temp: +(25 + Math.random() * 15).toFixed(1)
        })),
        ts: Date.now()
    });
}, 2000);

// ── Startup ───────────────────────────────────────────────────────────────────
connectMongo();

app.use((err, req, res, next) => {
    log('error', 'Unhandled express error', {
        requestId: req?.requestId,
        error: err?.message || 'Unknown error'
    });
    if (res.headersSent) {
        return next(err);
    }
    return res.status(500).json({ error: 'Unexpected server error.' });
});

process.on('SIGINT', () => {
    log('info', 'Server shutting down (SIGINT)');
    process.exit(0);
});

