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

const User = require('./models/User');

// ── Config ───────────────────────────────────────────────────────────────────
const HTTP_PORT  = process.env.HTTP_PORT  || 3000;
const WS_PORT    = process.env.WS_PORT    || 8080;
const MONGODB_URI = process.env.MONGODB_URI || '';
const MONGODB_DB  = process.env.MONGODB_DB  || 'intellinflate_db';
const JWT_SECRET  = process.env.JWT_SECRET  || 'supersecretkey';

// ── MongoDB (Mongoose) ────────────────────────────────────────────────────────
async function connectMongo() {
    if (!MONGODB_URI || MONGODB_URI.includes('<username>')) {
        console.warn('⚠️  MongoDB URI not set or invalid in .env — database writes disabled');
        return;
    }
    try {
        await mongoose.connect(MONGODB_URI, {
            dbName: MONGODB_DB,
            family: 4,
            serverSelectionTimeoutMS: 30000,
            connectTimeoutMS: 30000
        });
        console.log(`✅ MongoDB (Mongoose) connected → ${MONGODB_DB}`);
    } catch (err) {
        console.error('❌ MongoDB connection failed:', err.message);
    }
}

// ── Multer Config (for uploads) ────────────────────────────────────────────────
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir);

const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => cb(null, `${Date.now()}-${file.originalname}`)
});
const upload = multer({ storage });

// ── Express HTTP server ───────────────────────────────────────────────────────
const app = express();
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(uploadDir));

// Serve frontend static files
const frontendDir = path.join(__dirname, '..', 'frontend', 'public');
app.use(express.static(frontendDir));

// Health check
app.get('/health', (req, res) => {
    res.json({ status: 'ok', db: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected', ts: Date.now() });
});

/**
 * POST /api/register
 * User registration with number plate and essential details.
 */
app.post('/api/register', async (req, res) => {
    try {
        const { username, email, password, numberPlate, vehicleModel, phone } = req.body;
        console.log(`📩 Registration attempt: ${email}, Plate: ${numberPlate}`);
        
        // Basic validation
        if (!username || !email || !password || !numberPlate) {
            return res.status(400).json({ error: 'All essential details are required.' });
        }

        const normalizedPlate = numberPlate.toUpperCase().replace(/\s/g, '');

        // Check for existing user
        const existingUser = await User.findOne({ $or: [{ email }, { numberPlate: normalizedPlate }] });
        if (existingUser) {
            return res.status(400).json({ error: 'User with this email or number plate already exists.' });
        }

        // Hash password
        const hashedPassword = await bcrypt.hash(password, 10);

        // Create user
        const newUser = new User({
            username,
            email,
            password: hashedPassword,
            numberPlate: normalizedPlate,
            vehicleModel,
            phone
        });

        await newUser.save();
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
        console.error('Registration error:', err);
        res.status(500).json({ error: 'Internal server error.' });
    }
});

/**
 * POST /api/login
 * User login with email or number plate.
 */
app.post('/api/login', async (req, res) => {
    try {
        const { identifier, password } = req.body;

        if (!identifier || !password) {
            return res.status(400).json({ error: 'Identifier and password are required.' });
        }

        const normalizedIdentifier = identifier.includes('@') ? identifier : identifier.toUpperCase().replace(/\s/g, '');

        const user = await User.findOne({ 
            $or: [
                { email: identifier }, 
                { numberPlate: normalizedIdentifier }
            ] 
        });

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
        console.error('Login error:', err);
        res.status(500).json({ error: 'Internal server error.' });
    }
});

/**
 * POST /api/analyze-tire
 * Endpoint to perform specific tire health analysis using Python.
 * Supports modes: SIDE (for cracks), FRONT (for tread/alignment).
 */
app.post('/api/analyze-tire', upload.single('image'), async (req, res) => {
    if (!req.file) return res.status(400).json({ error: 'Image file is required.' });
    
    const mode = req.body.mode || 'FRONT'; // Default to FRONT if not specified
    const imagePath = req.file.path;
    const pythonScript = path.join(__dirname, 'python-services', 'tire_analysis.py');
    const venvPython = path.join(__dirname, 'python-services', 'venv', 'bin', 'python3');

    // Call Python script with mode
    const pythonProcess = spawn(venvPython, [pythonScript, '-i', imagePath, '-m', mode]);

    let outputData = '';
    pythonProcess.stdout.on('data', (data) => {
        outputData += data.toString();
    });

    pythonProcess.stderr.on('data', (data) => {
        console.error(`Python Error (${mode} Analysis): ${data}`);
    });

    pythonProcess.on('close', (code) => {
        if (code !== 0) {
            return res.status(500).json({ error: `Python ${mode} analysis script failed.` });
        }

        try {
            const result = JSON.parse(outputData);
            res.json(result);
        } catch (e) {
            console.error('Failed to parse Python output:', outputData);
            res.status(500).json({ error: 'Failed to parse analysis results.' });
        }
    });
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
    const pythonScript = path.join(__dirname, 'python-services', 'number_plate_detection.py');
    const venvPython = path.join(__dirname, 'python-services', 'venv', 'bin', 'python3');

    // Call Python script using the virtual environment
    const pythonProcess = spawn(venvPython, [pythonScript, '-i', imagePath]);

    let outputData = '';
    pythonProcess.stdout.on('data', (data) => {
        outputData += data.toString();
    });

    pythonProcess.stderr.on('data', (data) => {
        console.error(`Python Error: ${data}`);
    });

    pythonProcess.on('close', async (code) => {
        if (code !== 0) {
            return res.status(500).json({ error: 'Python detection script failed.' });
        }

        const match = outputData.match(/Detected: ([\w\s-]+) \(Confidence: ([\d.]+)\)/);
        
        if (match) {
            const detectedPlate = match[1].toUpperCase().replace(/\s/g, '');
            const confidence = parseFloat(match[2]);

            const user = await User.findOne({ numberPlate: detectedPlate });

            const result = {
                stationId: "STATION-001",
                sessionId: require('crypto').randomUUID(),
                success: true,
                detectedPlate,
                confidence,
                user: user ? {
                    username: user.username,
                    email: user.email,
                    vehicleModel: user.vehicleModel,
                    phone: user.phone
                } : null
            };
            
            // Format for mobile app (if needed)
            res.json(result);
        } else {
            res.status(404).json({
                success: false,
                message: 'No number plate detected.',
                rawOutput: outputData.trim()
            });
        }
    });
}

/**
 * GET /api/station/info
 * Mobile app connection test endpoint.
 */
app.get('/api/station/info', (req, res) => {
    res.json({
        stationId: "STATION-001",
        name: "IntelliInflate Hub",
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

app.listen(HTTP_PORT, '0.0.0.0', () => {
    console.log(`🚀 Node.js Backend running on http://0.0.0.0:${HTTP_PORT}`);
});

// ── WebSocket server ──────────────────────────────────────────────────────────
const wss = new WebSocket.Server({ port: WS_PORT });
let connectedClients = new Set();

wss.on('connection', (ws) => {
    console.log('✅ WS client connected');
    connectedClients.add(ws);
    ws.send(JSON.stringify({ type: 'connection', message: 'Connected to IntelliInflate Server', ts: Date.now() }));
    
    ws.on('close', () => connectedClients.delete(ws));
    ws.on('error', () => connectedClients.delete(ws));
});

function broadcastToClients(data) {
    const msg = JSON.stringify(data);
    connectedClients.forEach(c => { if (c.readyState === WebSocket.OPEN) c.send(msg); });
}

// ── Simulation mode (no hardware) ─────────────────────────────────────────────
console.log('🎮 Simulation mode active (simulated ESP32 data)');
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

process.on('SIGINT', () => {
    console.log('\n🛑 Shutting down...');
    process.exit(0);
});

