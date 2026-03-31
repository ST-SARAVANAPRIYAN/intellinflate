/**
 * Production Configuration Module
 * Handles environment-based configuration and validation
 */

const fs = require('fs');
const path = require('path');

// ── Environment Detection ────────────────────────────────────────────────────
const NODE_ENV = process.env.NODE_ENV || 'development';
const IS_PRODUCTION = NODE_ENV === 'production';

// ── Configuration Object ─────────────────────────────────────────────────────
const config = {
  env: NODE_ENV,
  isProduction: IS_PRODUCTION,
  isDevelopment: NODE_ENV === 'development',

  // Server Configuration
  server: {
    httpPort: Number(process.env.HTTP_PORT) || 3000,
    wsPort: Number(process.env.WS_PORT) || 8080,
    heartbeatInterval: Number(process.env.WS_HEARTBEAT_INTERVAL) || 30,
  },

  // Database Configuration
  mongodb: {
    uri: process.env.MONGODB_URI || 'mongodb://localhost:27017',
    db: process.env.MONGODB_DB || 'intellinflate_db',
    options: {
      retryWrites: true,
      w: 'majority',
      maxPoolSize: IS_PRODUCTION ? 20 : 5,
      minPoolSize: IS_PRODUCTION ? 5 : 1,
      socketTimeoutMS: 45000,
      serverSelectionTimeoutMS: 5000,
      connectTimeoutMS: 10000,
    },
  },

  // Security Configuration
  security: {
    jwtSecret:
      process.env.JWT_SECRET || 'dev-secret-do-not-use-in-production',
    jwtExpiry: '7d',
    bcryptRounds: IS_PRODUCTION ? 12 : 10,
    corsOrigin:
      process.env.CORS_ORIGIN ||
      (IS_PRODUCTION ? false : 'http://localhost:*'),
  },

  // Python Services Configuration
  python: {
    servicesDir:
      process.env.PYTHON_SERVICES_DIR ||
      path.join(__dirname, 'python-services'),
    executable: process.env.PYTHON_EXECUTABLE || 'python3',
    timeout: Number(process.env.PYTHON_TIMEOUT) || 60000,
  },

  // ML Model Configuration
  models: {
    cracksModel:
      process.env.INTELLINFLATE_CRACK_MODEL || null,
    treadModel:
      process.env.INTELLINFLATE_TREAD_MODEL || null,
    yoloModel:
      process.env.INTELLINFLATE_YOLO_MODEL || null,
  },

  // Serial Port Configuration
  serial: {
    port: process.env.SERIAL_PORT || '/dev/ttyUSB0',
    baudRate: Number(process.env.BAUD_RATE) || 115200,
  },

  // Logging Configuration
  logging: {
    level: process.env.LOG_LEVEL || 'info',
    file: process.env.LOG_FILE || null,
  },

  // Alert Configuration
  alerts: {
    emailEnabled:
      Boolean(process.env.SMTP_HOST) && Boolean(process.env.SMTP_USER),
    email: process.env.ALERT_EMAIL || null,
  },
};

// ── Validation Function ──────────────────────────────────────────────────────
function validateConfig() {
  const errors = [];

  // Validate critical production settings
  if (IS_PRODUCTION) {
    if (
      config.security.jwtSecret === 'dev-secret-do-not-use-in-production'
    ) {
      errors.push('JWT_SECRET must be configured for production');
    }
    if (
      !process.env.MONGODB_URI ||
      process.env.MONGODB_URI.includes('localhost')
    ) {
      errors.push('MONGODB_URI must be configured for production');
    }
    if (config.security.corsOrigin === false) {
      errors.push('CORS_ORIGIN must be configured for production');
    }
  }

  // Validate paths
  if (!fs.existsSync(config.python.servicesDir)) {
    errors.push(
      `Python services directory not found: ${config.python.servicesDir}`
    );
  }

  if (errors.length > 0) {
    console.error('Configuration Validation Errors:');
    errors.forEach((err) => console.error(`  ✗ ${err}`));
    return false;
  }

  return true;
}

// ── Export ───────────────────────────────────────────────────────────────────
module.exports = {
  config,
  validateConfig,
  IS_PRODUCTION,
  NODE_ENV,
};
