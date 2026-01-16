# Development Workflow

## Local Development Setup

### Prerequisites

```bash
# Java 21
java -version

# Node.js (LTS)
node -v
npm -v
```

### Initial Setup

```bash
# Backend
cd backend
mvn -v

# Frontend
cd ../frontend
npm install
```

### Development Commands

```bash
# Start all services (root)
./startup.sh

# Start frontend only
cd frontend
npm run dev -- --port 5173

# Start backend only
cd backend
mvn spring-boot:run

# Run tests
cd backend
mvn test
cd ../frontend
npm run test
```

## Environment Configuration

### Required Environment Variables

```bash
# Frontend (.env.local)
VITE_API_BASE=http://localhost:8080

# Backend (.env)
APP_STORAGE_ROOT=./data/videos
APP_DB_PATH=./data/app.db
APP_HLS_SEGMENT_SECONDS=2
APP_UPLOAD_MAX_BYTES=5368709120
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=5GB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=5GB
SPRING_SERVLET_MULTIPART_FILE_SIZE_THRESHOLD=1MB
SPRING_SERVLET_MULTIPART_LOCATION=./data/tmp
SERVER_TOMCAT_MAX_SWALLOW_SIZE=5GB
SERVER_TOMCAT_CONNECTION_TIMEOUT=600000
SERVER_TOMCAT_KEEP_ALIVE_TIMEOUT=600000

# Shared
# (none)
```
