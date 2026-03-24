// TireGuard AI - Central Monitoring Dashboard Logic

const API_URL = 'http://localhost:3000';
let activeDetection = null;
let isLiveScanning = false;
let liveScanInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    updateTime();
    setInterval(updateTime, 1000);

    // Simulation Trigger (Select Image)
    const testUpload = document.getElementById('test-upload');
    const triggerBtn = document.getElementById('trigger-test');

    triggerBtn.addEventListener('click', () => {
        stopLiveScan();
        testUpload.click();
    });
    
    testUpload.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            processVehicleScan(e.target.files[0]);
        }
    });

    // Add Live Scan Button to UI dynamically if not present
    setupLiveScanUI();

    // Initialize Dashboard
    addLog('System initialized. Monitoring camera feeds...', 'system');
});

function setupLiveScanUI() {
    const topActions = document.querySelector('.top-actions');
    const liveBtn = document.createElement('button');
    liveBtn.id = 'live-scan-btn';
    liveBtn.className = 'status-badge';
    liveBtn.style.backgroundColor = 'var(--accent)';
    liveBtn.style.color = 'white';
    liveBtn.style.border = 'none';
    liveBtn.style.cursor = 'pointer';
    liveBtn.textContent = 'Start Live Scan';
    liveBtn.onclick = toggleLiveScan;
    topActions.prepend(liveBtn);
}

function updateTime() {
    const now = new Date();
    document.getElementById('current-time').textContent = now.toLocaleTimeString();
}

async function toggleLiveScan() {
    const btn = document.getElementById('live-scan-btn');
    if (!isLiveScanning) {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: true });
            const video = document.createElement('video');
            video.id = 'live-video';
            video.srcObject = stream;
            video.play();
            
            document.getElementById('camera-placeholder').style.display = 'none';
            const feedImg = document.getElementById('detected-vehicle-img');
            feedImg.style.display = 'none';
            
            const cameraFeed = document.querySelector('.camera-feed');
            cameraFeed.appendChild(video);
            video.style.width = '100%';
            video.style.height = '100%';
            video.style.objectFit = 'cover';
            
            isLiveScanning = true;
            btn.textContent = 'Stop Live Scan';
            btn.style.backgroundColor = 'var(--danger)';
            addLog('Live scan started.', 'system');
            
            // Capture frame every 3 seconds for analysis
            liveScanInterval = setInterval(() => captureAndAnalyze(video), 5000);
        } catch (err) {
            addLog('Camera access denied.', 'alert');
        }
    } else {
        stopLiveScan();
    }
}

function stopLiveScan() {
    if (!isLiveScanning) return;
    
    const btn = document.getElementById('live-scan-btn');
    const video = document.getElementById('live-video');
    if (video) {
        const stream = video.srcObject;
        stream.getTracks().forEach(track => track.stop());
        video.remove();
    }
    
    clearInterval(liveScanInterval);
    isLiveScanning = false;
    btn.textContent = 'Start Live Scan';
    btn.style.backgroundColor = 'var(--accent)';
    document.getElementById('camera-placeholder').style.display = 'flex';
    addLog('Live scan stopped.', 'system');
}

async function captureAndAnalyze(video) {
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    
    canvas.toBlob(async (blob) => {
        const file = new File([blob], "live-frame.jpg", { type: "image/jpeg" });
        await processVehicleScan(file, true);
    }, 'image/jpeg');
}

async function processVehicleScan(file, isLive = false) {
    const mode = document.getElementById('analysis-mode').value;
    if (!isLive) addLog(`Starting ${mode} analysis...`, 'system');
    
    const placeholder = document.getElementById('camera-placeholder');
    const feedImg = document.getElementById('detected-vehicle-img');
    const plateDisplay = document.getElementById('detected-plate');
    const confidenceBar = document.querySelector('#plate-confidence .fill');
    const confidenceText = document.querySelector('#plate-confidence .text');

    if (!isLiveScanning) {
        placeholder.style.display = 'none';
        feedImg.src = URL.createObjectURL(file);
        feedImg.style.display = 'block';
    }

    const formData = new FormData();
    formData.append('image', file);
    formData.append('mode', mode);

    try {
        if (mode === 'PLATE') {
            // Number Plate Recognition
            const response = await fetch(`${API_URL}/api/detect`, {
                method: 'POST',
                body: formData
            });
            const data = await response.json();

            if (data.success) {
                plateDisplay.textContent = data.detectedPlate;
                const conf = Math.round(data.confidence * 100);
                confidenceBar.style.width = `${conf}%`;
                confidenceText.textContent = `${conf}%`;
                
                if (!isLive || activeDetection !== data.detectedPlate) {
                    addLog(`Number plate recognized: ${data.detectedPlate}`, 'detect');
                    activeDetection = data.detectedPlate;
                    if (data.user) displayUserDetails(data.user);
                    else displayUnknownUser();
                }
            } else {
                if (!isLive) addLog('Plate detection failed', 'alert');
            }
        } else {
            // Specialized Tire Analysis (Front/Side)
            const response = await fetch(`${API_URL}/api/analyze-tire`, {
                method: 'POST',
                body: formData
            });
            const data = await response.json();

            if (data.success) {
                if (mode === 'FRONT') {
                    addLog(`Front view analysis complete: Tread ${data.tread.value}, Alignment ${data.alignment.value}`, 'system');
                    updateHealthUI({
                        score: data.overall_score,
                        tread: data.tread,
                        alignment: data.alignment,
                        cracks: { status: 'Not Scanned', value: '--', level: 0 },
                        leakage: { status: '32.0 PSI', value: 'Stable', level: 0 }
                    });
                } else if (mode === 'SIDE') {
                    addLog(`Side view analysis complete: ${data.cracks.status} (${data.cracks.count} cracks)`, 'system');
                    updateHealthUI({
                        score: 100 - (data.cracks.level * 25),
                        tread: { status: 'Not Scanned', value: '--', level: 0 },
                        alignment: { status: 'Not Scanned', value: '--', level: 0 },
                        cracks: data.cracks,
                        leakage: { status: '32.0 PSI', value: 'Stable', level: 0 }
                    });
                }
            }
        }
    } catch (err) {
        if (!isLive) addLog('Error communicating with backend', 'alert');
        console.error(err);
    }
}

function displayUserDetails(user) {
    const container = document.getElementById('user-details');
    container.innerHTML = `
        <div class="user-item">
            <label>Full Name</label>
            <span>${user.username}</span>
        </div>
        <div class="user-item">
            <label>Email Address</label>
            <span>${user.email}</span>
        </div>
        <div class="user-item">
            <label>Vehicle Model</label>
            <span>${user.vehicleModel || 'N/A'}</span>
        </div>
        <div class="user-item">
            <label>Phone Number</label>
            <span>${user.phone || 'N/A'}</span>
        </div>
    `;
    addLog(`User profile loaded for ${user.username}`, 'system');
}

function displayUnknownUser() {
    const container = document.getElementById('user-details');
    container.innerHTML = `
        <div class="user-item">
            <label>Status</label>
            <span style="color: var(--danger)">UNREGISTERED VEHICLE</span>
        </div>
        <p style="font-size: 0.813rem; color: var(--text-secondary); margin-top: 1rem;">
            This vehicle is not in the system. Suggest mobile registration.
        </p>
    `;
    addLog('Warning: Unregistered vehicle detected', 'warn');
}

function performTireHealthCheck(plate) {
    addLog('Running AI Tire Health Diagnostics...', 'system');
    
    // Simulate AI analysis delay
    setTimeout(() => {
        // Generate results based on the plate or randomly for demo
        const results = generateMockHealthData();
        updateHealthUI(results);
        addLog('Tire health analysis complete.', 'system');
    }, 1500);
}

function generateMockHealthData() {
    // Logic to create realistic varied data
    const treadLevels = ['Healthy', 'Moderate Wear', 'Replace Soon', 'Worn Out'];
    const alignmentLevels = ['Balanced', 'Minor Imbalance', 'Misaligned'];
    const binary = ['None Detected', 'Detected'];
    const leakLevels = ['None', 'Slow Leak', 'Significant Drop'];

    const treadIdx = Math.floor(Math.random() * 3); // Avoid 'Worn Out' usually
    const alignmentIdx = Math.floor(Math.random() * 2);
    const crackIdx = Math.random() > 0.8 ? 1 : 0;
    const leakIdx = Math.random() > 0.85 ? 1 : 0;

    // Calculate overall score
    let score = 100;
    score -= (treadIdx * 15);
    score -= (alignmentIdx * 10);
    score -= (crackIdx * 25);
    score -= (leakIdx * 20);

    return {
        score: Math.max(0, score),
        tread: { status: treadLevels[treadIdx], value: `${(8 - treadIdx * 2).toFixed(1)} mm`, level: treadIdx },
        alignment: { status: alignmentLevels[alignmentIdx], value: alignmentIdx === 0 ? '0.05°' : '1.2°', level: alignmentIdx },
        cracks: { status: binary[crackIdx], value: crackIdx === 0 ? 'N/A' : 'Sidewall', level: crackIdx * 2 },
        leakage: { status: leakLevels[leakIdx], value: leakIdx === 0 ? '32.5 PSI' : '28.1 PSI', level: leakIdx * 2 }
    };
}

function updateHealthUI(data) {
    const scoreEl = document.getElementById('overall-health-score');
    scoreEl.textContent = data.score;
    
    if (data.score > 80) scoreEl.style.color = 'var(--success)';
    else if (data.score > 50) scoreEl.style.color = 'var(--warning)';
    else scoreEl.style.color = 'var(--danger)';

    updateCard('tread', data.tread);
    updateCard('alignment', data.alignment);
    updateCard('cracks', data.cracks);
    updateCard('leakage', data.leakage);

    if (data.tread.level >= 2) addLog('Alert: Tread depth below safety limit', 'alert');
    if (data.alignment.level >= 1) addLog('Warning: Possible wheel misalignment', 'warn');
    if (data.cracks.level >= 1) addLog('CRITICAL: Sidewall cracks detected!', 'alert');
    if (data.leakage.level >= 1) addLog('Alert: Abnormal pressure drop detected', 'alert');
}

function updateCard(id, info) {
    const statusEl = document.getElementById(`status-${id}`);
    const valEl = document.getElementById(`val-${id}`);
    const cardEl = document.getElementById(`card-${id}`);

    statusEl.textContent = info.status;
    valEl.textContent = info.value;

    statusEl.className = 'card-status';
    if (info.level === 0) statusEl.classList.add('status-good');
    else if (info.level === 1) statusEl.classList.add('status-warn');
    else statusEl.classList.add('status-danger');
}

function addLog(message, type = 'system') {
    const container = document.getElementById('health-logs');
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    const time = new Date().toLocaleTimeString([], { hour12: false });
    entry.textContent = `[${time}] ${message}`;
    container.appendChild(entry);
    container.scrollTop = container.scrollHeight;
}
