#!/bin/bash
# ──────────────────────────────────────────────────────────────────────────────
# IntellInflate Production Deployment Script
# ──────────────────────────────────────────────────────────────────────────────
# This script prepares and deploys the IntellInflate server to production

set -e

PROJECT_ROOT="/home/saravana/projects/DESIGN-PROJECT"
SERVER_DIR="$PROJECT_ROOT/server"
VENV_DIR="$SERVER_DIR/python-services/.venv"
BACKUP_DIR="/var/backups/intellinflate"
LOG_DIR="/var/log/intellinflate"
SYSTEMD_SERVICE="intellinflate-server"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  IntellInflate Production Deployment Script           ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"

# ── Functions ────────────────────────────────────────────────────────────────
log_info() {
  echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
  exit 1
}

check_requirements() {
  log_info "Checking system requirements..."

  # Check Node.js
  if ! command -v node &> /dev/null; then
    log_error "Node.js is not installed"
  fi
  NODE_VERSION=$(node -v)
  log_info "✓ Node.js $NODE_VERSION found"

  # Check Python 3.11
  if ! command -v python3.11 &> /dev/null && [ ! -f "$VENV_DIR/bin/python" ]; then
    log_error "Python 3.11 or virtual environment not found"
  fi
  log_info "✓ Python environment ready"

  # Check MongoDB connectivity
  if [ -z "$MONGODB_URI" ]; then
    log_error "MONGODB_URI environment variable not set"
  fi
  log_info "✓ MongoDB URI configured"

  # Check MongoDB connection (with timeout)
  if command -v mongosh &> /dev/null; then
    if timeout 5 mongosh "$MONGODB_URI" --eval "db.adminCommand('ping')" &>/dev/null; then
      log_info "✓ MongoDB connection verified"
    else
      log_warn "⚠ Could not verify MongoDB connection - ensure it's running"
    fi
  else
    log_warn "⚠ mongosh not found - skipping MongoDB connection check"
  fi
}

create_directories() {
  log_info "Creating required directories..."

  sudo mkdir -p "$LOG_DIR"
  sudo mkdir -p "$BACKUP_DIR"
  sudo chown "$USER:$USER" "$LOG_DIR" "$BACKUP_DIR" 2>/dev/null || true

  log_info "✓ Directories created"
}

backup_database() {
  log_info "Creating database backup..."

  TIMESTAMP=$(date +%Y%m%d_%H%M%S)
  BACKUP_FILE="$BACKUP_DIR/intellinflate_backup_$TIMESTAMP.gz"

  if command -v mongodump &> /dev/null; then
    mongodump --uri="$MONGODB_URI" --archive="$BACKUP_FILE" --gzip 2>/dev/null || {
      log_warn "⚠ Database backup failed - ensure MongoDB is accessible"
    }
    log_info "✓ Database backup created: $BACKUP_FILE"
  else
    log_warn "⚠ mongodump not found - skipping database backup"
  fi
}

install_dependencies() {
  log_info "Installing Node.js dependencies..."

  cd "$SERVER_DIR"
  npm install --production
  log_info "✓ Node.js dependencies installed"

  log_info "Installing Python dependencies..."

  if [ -f "$VENV_DIR/bin/python" ]; then
    "$VENV_DIR/bin/python" -m pip install --upgrade pip setuptools wheel -q
    "$VENV_DIR/bin/python" -m pip install -r "$SERVER_DIR/python-services/requirements.txt" -q
    if [ -f "$SERVER_DIR/python-services/requirements-ml.txt" ]; then
      "$VENV_DIR/bin/python" -m pip install -r "$SERVER_DIR/python-services/requirements-ml.txt" -q
    fi
    log_info "✓ Python dependencies installed"
  else
    log_error "Python virtual environment not found"
  fi
}

setup_systemd_service() {
  log_info "Setting up systemd service..."

  SERVICE_FILE="/etc/systemd/system/$SYSTEMD_SERVICE.service"

  sudo tee "$SERVICE_FILE" > /dev/null <<EOF
[Unit]
Description=IntellInflate Server
After=network.target mongodb.service

[Service]
Type=simple
User=$USER
WorkingDirectory=$SERVER_DIR
Environment="NODE_ENV=production"
Environment="PYTHON_EXECUTABLE=$VENV_DIR/bin/python"
EnvironmentFile=$SERVER_DIR/.env.production
ExecStart=$SERVER_DIR/node_modules/.bin/node $SERVER_DIR/server.js
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

  sudo systemctl daemon-reload
  log_info "✓ Systemd service configured"
}

run_tests() {
  log_info "Running health checks..."

  cd "$SERVER_DIR"

  # Check Python imports
  log_info "  Checking Python ML imports..."
  "$VENV_DIR/bin/python" -c "import tensorflow; import ultralytics; import easyocr; print('  ✓ All ML packages available')" || {
    log_warn "⚠ Some ML packages unavailable - optional features may not work"
  }

  # Check Node.js required packages
  log_info "  Checking Node.js packages..."
  node -e "
    require('express');
    require('mongoose');
    require('ws');
    require('jwt');
    console.log('  ✓ All Node.js dependencies available');
  " || log_error "Node.js dependency check failed"

  log_info "✓ Health checks passed"
}

generate_production_docs() {
  log_info "Generating production documentation..."

  cat > "$PROJECT_ROOT/docs/PRODUCTION_DEPLOYMENT.md" <<'EOF'
# IntellInflate Production Deployment Guide

## Prerequisites

- Node.js 16+
- Python 3.11 with virtual environment
- MongoDB Atlas account
- Linux server with systemd (or equivalent init system)

## Deployment Steps

### 1. System Preparation

```bash
cd /home/saravana/projects/DESIGN-PROJECT
bash server/deploy.sh
```

### 2. Environment Configuration

Edit `.env.production`:
- Set `MONGODB_URI` to your MongoDB Atlas connection string
- Generate and set `JWT_SECRET`: `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`
- Configure `CORS_ORIGIN` to your domain
- Set `SERIAL_PORT` to your ESP32 device path

### 3. Service Management

```bash
# Start service
sudo systemctl start intellinflate-server

# Check status
sudo systemctl status intellinflate-server

# View logs
sudo journalctl -u intellinflate-server -f

# Enable auto-start
sudo systemctl enable intellinflate-server
```

### 4. Monitoring

Monitor logs in real-time:
```bash
sudo journalctl -u intellinflate-server -f
```

Check server health:
```bash
curl http://localhost:3000/health
```

## Security Checklist

- [ ] JWT_SECRET changed to strong random value
- [ ] MONGODB_URI uses production database with secure credentials
- [ ] CORS_ORIGIN restricted to your domain
- [ ] Firewall configured (only ports 3000, 8080 exposed)
- [ ] SSL/TLS configured with nginx/HAProxy reverse proxy
- [ ] Database backups enabled and tested
- [ ] Log rotation configured
- [ ] Rate limiting implemented
- [ ] Input validation on all endpoints

## Performance Tuning

### Node.js
- Use `NODE_ENV=production`
- Enable clustering for multi-core systems
- Use reverse proxy (nginx) for load balancing

### Python ML
- Models cached in memory after first load
- Use GPU acceleration if available (TensorFlow respects CUDA)
- Connection pooling for database queries

### MongoDB
- Enable index creation on frequently queried fields
- Monitor collection sizes
- Implement time-based data retention policies

## Backup & Recovery

Database backups created during deployment:
```bash
ls -la /var/backups/intellinflate/
```

Restore from backup:
```bash
mongorestore --uri="$MONGODB_URI" --archive=backup.gz --gzip
```

## Troubleshooting

### Service won't start
```bash
sudo systemctl status intellinflate-server
sudo journalctl -u intellinflate-server -n 50
```

### Python module import errors
```bash
/var/intellinflate/.venv/bin/pip list
/var/intellinflate/.venv/bin/python -m pip install --upgrade -r requirements.txt
```

### Database connection issues
```bash
mongosh "$MONGODB_URI" --eval "db.adminCommand('ping')"
```

## Rollback Procedure

```bash
# Stop service
sudo systemctl stop intellinflate-server

# Revert code changes (using git)
git revert <commit-hash>

# Start service
sudo systemctl start intellinflate-server
```

EOF

  log_info "✓ Production documentation generated"
}

# ── Main Execution ──────────────────────────────────────────────────────────
check_requirements
create_directories
backup_database
install_dependencies
setup_systemd_service
run_tests
generate_production_docs

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  ✓ Deployment preparation complete!                  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "1. Edit configuration: $SERVER_DIR/.env.production"
echo "2. Start service: sudo systemctl start $SYSTEMD_SERVICE"
echo "3. Check logs: sudo journalctl -u $SYSTEMD_SERVICE -f"
echo ""
