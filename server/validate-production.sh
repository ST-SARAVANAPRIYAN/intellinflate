#!/bin/bash
# ──────────────────────────────────────────────────────────────────────────────
# IntellInflate Production Testing & Validation Script
# ──────────────────────────────────────────────────────────────────────────────

set -e

PROJECT_ROOT="/home/saravana/projects/DESIGN-PROJECT"
SERVER_DIR="$PROJECT_ROOT/server"
VENV_DIR="$SERVER_DIR/python-services/.venv"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  IntellInflate Production Validation                   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"

# ── Test Counters ────────────────────────────────────────────────────────────
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

test_result() {
  if [ $1 -eq 0 ]; then
    echo -e "${GREEN}✓ $2${NC}"
    ((TESTS_PASSED++))
  else
    echo -e "${RED}✗ $2${NC}"
    ((TESTS_FAILED++))
  fi
}

skip_test() {
  echo -e "${YELLOW}⊘ $1 (skipped)${NC}"
  ((TESTS_SKIPPED++))
}

# ── Tests ────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BLUE}[1/5]${NC} System Dependencies"
echo "───────────────────────────────────────────────"

# Node.js check
node --version &>/dev/null
test_result $? "Node.js installed"

# npm check
npm --version &>/dev/null
test_result $? "npm installed"

# Git check
git --version &>/dev/null
test_result $? "Git installed"

# Python 3.11 check
"$VENV_DIR/bin/python" --version &>/dev/null
test_result $? "Python 3.11 virtual environment available"

# Docker check (optional)
if command -v docker &>/dev/null; then
  docker --version &>/dev/null
  test_result $? "Docker installed (optional)"
else
  skip_test "Docker (optional for containerization)"
fi

echo ""
echo -e "${BLUE}[2/5]${NC} Node.js Modules"
echo "───────────────────────────────────────────────"

cd "$SERVER_DIR"

# Check npm dependencies
npm ls express &>/dev/null
test_result $? "Express module installed"

npm ls mongoose &>/dev/null
test_result $? "Mongoose module installed"

npm ls ws &>/dev/null
test_result $? "WebSocket module installed"

npm ls cors &>/dev/null
test_result $? "CORS module installed"

npm ls bcryptjs &>/dev/null
test_result $? "bcryptjs module installed"

npm ls jsonwebtoken &>/dev/null
test_result $? "JWT module installed"

npm ls multer &>/dev/null
test_result $? "Multer module installed"

echo ""
echo -e "${BLUE}[3/5]${NC} Python ML Stack"
echo "───────────────────────────────────────────────"

# Test Python imports
"$VENV_DIR/bin/python" -c "import tensorflow; print(tensorflow.__version__)" &>/dev/null
test_result $? "TensorFlow installed and importable"

"$VENV_DIR/bin/python" -c "import ultralytics; print(ultralytics.__version__)" &>/dev/null
test_result $? "Ultralytics (YOLOv8) installed and importable"

"$VENV_DIR/bin/python" -c "import easyocr; print('EasyOCR OK')" &>/dev/null
test_result $? "EasyOCR installed and importable"

"$VENV_DIR/bin/python" -c "import cv2; print(cv2.__version__)" &>/dev/null
test_result $? "OpenCV installed and importable"

"$VENV_DIR/bin/python" -c "import numpy; print(numpy.__version__)" &>/dev/null
test_result $? "NumPy installed and importable"

echo ""
echo -e "${BLUE}[4/5]${NC} Python Services"
echo "───────────────────────────────────────────────"

# Check service files exist
[ -f "$SERVER_DIR/python-services/tire_analysis.py" ]
test_result $? "tire_analysis.py exists"

[ -f "$SERVER_DIR/python-services/number_plate_detection.py" ]
test_result $? "number_plate_detection.py exists"

[ -f "$SERVER_DIR/python-services/requirements.txt" ]
test_result $? "requirements.txt exists"

# Test Python service imports
"$VENV_DIR/bin/python" -c "import sys; sys.path.insert(0, '$SERVER_DIR/python-services'); from tire_analysis import analyze_image" &>/dev/null
test_result $? "tire_analysis module imports successfully"

"$VENV_DIR/bin/python" -c "import sys; sys.path.insert(0, '$SERVER_DIR/python-services'); from number_plate_detection import detect_plate" &>/dev/null
test_result $? "number_plate_detection module imports successfully"

echo ""
echo -e "${BLUE}[5/5]${NC} Configuration Files"
echo "───────────────────────────────────────────────"

[ -f "$SERVER_DIR/.env" ]
test_result $? ".env file present"

[ -f "$SERVER_DIR/.env.production" ]
test_result $? ".env.production file present"

[ -f "$SERVER_DIR/config.js" ]
test_result $? "config.js module present"

[ -f "$SERVER_DIR/middleware.js" ]
test_result $? "middleware.js module present"

[ -f "$PROJECT_ROOT/docs/API_REFERENCE.md" ]
test_result $? "API_REFERENCE.md documentation present"

[ -f "$PROJECT_ROOT/docs/PRODUCTION_DEPLOYMENT.md" ]
test_result $? "PRODUCTION_DEPLOYMENT.md guide present"

# ── Test Summary ─────────────────────────────────────────────────────────────
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Test Results Summary                                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "Passed:  ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed:  ${RED}$TESTS_FAILED${NC}"
echo -e "Skipped: ${YELLOW}$TESTS_SKIPPED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
  echo -e "${GREEN}✓ All tests passed! System is production-ready.${NC}"
  exit 0
else
  echo -e "${RED}✗ Some tests failed. Please resolve issues before production deployment.${NC}"
  exit 1
fi
