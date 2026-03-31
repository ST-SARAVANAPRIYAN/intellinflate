/**
 * Production Server Enhancement Module
 * Provides middleware and utilities for production-grade operation
 */

const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');

/**
 * Security Middleware
 * Adds security headers and protection against common attacks
 */
function setupSecurityMiddleware(app) {
  // Helmet.js middleware for security headers
  app.use(
    helmet({
      contentSecurityPolicy: {
        directives: {
          defaultSrc: ["'self'"],
          scriptSrc: ["'self'", "'unsafe-inline'"],
          styleSrc: ["'self'", "'unsafe-inline'"],
          imgSrc: ["'self'", 'data:', 'https:'],
        },
      },
    })
  );

  // Disable X-Powered-By header
  app.disable('x-powered-by');

  // Custom security headers
  app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    next();
  });
}

/**
 * Rate Limiting Middleware
 * Protects against brute force and DDoS attacks
 */
function setupRateLimiting(app) {
  const limiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 100, // limit each IP to 100 requests per windowMs
    message: 'Too many requests from this IP address, please try again later.',
    standardHeaders: true,
    legacyHeaders: false,
    skip: (req) => {
      // Skip rate limiting for health checks
      return req.path === '/health';
    },
  });

  app.use(limiter);

  // Stricter rate limiting for auth endpoints
  const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 5, // limit each IP to 5 login attempts per windowMs
    message: 'Too many login attempts, please try again later.',
    standardHeaders: true,
    legacyHeaders: false,
  });

  app.use('/auth/', authLimiter);
}

/**
 * Error Handling Middleware
 * Centralized error handling for production
 */
function setupErrorHandling(app) {
  // 404 handler
  app.use((req, res) => {
    res.status(404).json({
      success: false,
      error: {
        code: 'NOT_FOUND',
        message: `Endpoint ${req.method} ${req.path} not found`,
      },
      timestamp: new Date().toISOString(),
    });
  });

  // Global error handler (must be last)
  app.use((err, req, res, next) => {
    const status = err.status || err.statusCode || 500;
    const isDevelopment = process.env.NODE_ENV !== 'production';

    console.error({
      level: 'error',
      message: err.message,
      status,
      path: req.path,
      method: req.method,
      stack: isDevelopment ? err.stack : undefined,
      timestamp: new Date().toISOString(),
    });

    res.status(status).json({
      success: false,
      error: {
        code: err.code || 'INTERNAL_SERVER_ERROR',
        message: isDevelopment ? err.message : 'Internal server error',
      },
      timestamp: new Date().toISOString(),
    });
  });
}

/**
 * MongoDB Connection with Retry Logic
 */
async function connectToMongoDB(uri, options = {}) {
  const mongoose = require('mongoose');
  const maxRetries = 5;
  let retryCount = 0;

  while (retryCount < maxRetries) {
    try {
      await mongoose.connect(uri, {
        ...options,
        useNewUrlParser: true,
        useUnifiedTopology: true,
      });

      console.log({
        level: 'info',
        message: 'MongoDB connected successfully',
        timestamp: new Date().toISOString(),
      });

      return true;
    } catch (error) {
      retryCount++;
      console.log({
        level: 'warn',
        message: `MongoDB connection attempt ${retryCount}/${maxRetries} failed`,
        error: error.message,
        nextRetryIn: `${Math.pow(2, retryCount)}s`,
        timestamp: new Date().toISOString(),
      });

      if (retryCount < maxRetries) {
        await new Promise((resolve) =>
          setTimeout(resolve, Math.pow(2, retryCount) * 1000)
        );
      }
    }
  }

  throw new Error('Failed to connect to MongoDB after maximum retries');
}

/**
 * Health Check Endpoint
 */
function setupHealthCheck(app, config) {
  app.get('/health', async (req, res) => {
    const mongoose = require('mongoose');
    const fs = require('fs');
    const path = require('path');

    const health = {
      status: 'healthy',
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
      environment: process.env.NODE_ENV || 'development',
      server: {
        httpPort: config.server.httpPort,
        wsPort: config.server.wsPort,
      },
      database: {
        status: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected',
        mongodb: mongoose.connection.name || 'unknown',
      },
      python: {
        status: 'ready',
        executable: config.python.executable,
        timeout: config.python.timeout,
      },
      memory: {
        heapUsed: Math.round(process.memoryUsage().heapUsed / 1024 / 1024),
        heapTotal: Math.round(process.memoryUsage().heapTotal / 1024 / 1024),
      },
    };

    // Check Python services directory
    if (!fs.existsSync(config.python.servicesDir)) {
      health.python.status = 'directory_not_found';
      health.status = 'degraded';
    }

    // Check required Python modules
    try {
      const tirePyPath = path.join(
        config.python.servicesDir,
        'tire_analysis.py'
      );
      const platePyPath = path.join(
        config.python.servicesDir,
        'number_plate_detection.py'
      );

      health.python.modules = {
        tireAnalysis: fs.existsSync(tirePyPath),
        plateDetection: fs.existsSync(platePyPath),
      };
    } catch (e) {
      health.python.modules = { error: e.message };
    }

    const statusCode = health.status === 'healthy' ? 200 : 503;
    res.status(statusCode).json(health);
  });
}

/**
 * Request Logging Middleware
 */
function setupRequestLogging(app) {
  app.use((req, res, next) => {
    const start = Date.now();

    res.on('finish', () => {
      const duration = Date.now() - start;
      console.log({
        level: res.statusCode >= 400 ? 'warn' : 'info',
        message: `${req.method} ${req.path}`,
        method: req.method,
        path: req.path,
        status: res.statusCode,
        duration: `${duration}ms`,
        ip: req.ip || req.connection.remoteAddress,
        userAgent: req.get('user-agent'),
        timestamp: new Date().toISOString(),
      });
    });

    next();
  });
}

/**
 * CORS Configuration
 */
function setupCORS(app, corsOrigin) {
  const cors = require('cors');

  const corsOptions = {
    origin: corsOrigin === false ? undefined : corsOrigin,
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    optionsSuccessStatus: 200,
  };

  app.use(cors(corsOptions));
  app.options('*', cors(corsOptions));
}

/**
 * Graceful Shutdown Handler
 */
function setupGracefulShutdown(server, wsServer) {
  const signals = ['SIGTERM', 'SIGINT'];

  signals.forEach((signal) => {
    process.on(signal, async () => {
      console.log({
        level: 'info',
        message: `${signal} received, starting graceful shutdown`,
        timestamp: new Date().toISOString(),
      });

      // Close HTTP server
      server.close(() => {
        console.log({
          level: 'info',
          message: 'HTTP server closed',
          timestamp: new Date().toISOString(),
        });
      });

      // Close WebSocket server
      if (wsServer) {
        wsServer.clients.forEach((client) => {
          if (client.readyState === require('ws').OPEN) {
            client.close(1000, 'Server shutting down');
          }
        });

        wsServer.close(() => {
          console.log({
            level: 'info',
            message: 'WebSocket server closed',
            timestamp: new Date().toISOString(),
          });
        });
      }

      // Close database connection
      try {
        const mongoose = require('mongoose');
        await mongoose.connection.close();
        console.log({
          level: 'info',
          message: 'Database connection closed',
          timestamp: new Date().toISOString(),
        });
      } catch (err) {
        console.error({
          level: 'error',
          message: 'Error closing database connection',
          error: err.message,
          timestamp: new Date().toISOString(),
        });
      }

      process.exit(0);
    });
  });
}

module.exports = {
  setupSecurityMiddleware,
  setupRateLimiting,
  setupErrorHandling,
  connectToMongoDB,
  setupHealthCheck,
  setupRequestLogging,
  setupCORS,
  setupGracefulShutdown,
};
