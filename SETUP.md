# Complete Setup Guide - TT Crypto Trading Platform

This guide provides step-by-step instructions to get the TT Crypto Trading Platform running on your local machine.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Backend Setup](#backend-setup)
4. [Frontend Setup](#frontend-setup)
5. [Running the Applications](#running-the-applications)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements
- **OS**: Windows, macOS, or Linux
- **RAM**: Minimum 4GB (8GB recommended)
- **Disk Space**: Minimum 5GB

### Software Requirements

#### Java (Backend)
```bash
# Check Java version (should be 17+)
java -version

# If not installed:
# Windows: Download from https://www.oracle.com/java/technologies/downloads/
# macOS: brew install openjdk@17
# Linux: sudo apt-get install openjdk-17-jdk
```

#### Maven (Backend Build)
```bash
# Check Maven version
mvn -version

# If not installed:
# Windows: https://maven.apache.org/download.cgi
# macOS: brew install maven
# Linux: sudo apt-get install maven
```

#### Node.js & npm (Frontend)
```bash
# Check versions (Node 16+, npm 8+)
node --version
npm --version

# If not installed:
# Download from https://nodejs.org/
# Or use package managers:
# macOS: brew install node
# Linux: sudo apt-get install nodejs npm
```

#### MySQL (Database)
```bash
# Check MySQL version
mysql --version

# If not installed:
# Download from https://www.mysql.com/downloads/
# Or:
# macOS: brew install mysql
# Linux: sudo apt-get install mysql-server
# Windows: Download from https://dev.mysql.com/downloads/mysql/

# Start MySQL service
# Windows: net start MySQL80
# macOS: brew services start mysql
# Linux: sudo service mysql start
```

Verify installation:
```bash
# Test connection to MySQL
mysql -u root

# You should see the MySQL prompt: mysql>
# Exit with: exit
```

## Database Setup

### 1. Create Database User (Optional but Recommended)

```bash
# Connect to MySQL as root
mysql -u root -p

# Then run these commands:
CREATE USER 'ttcrypto'@'localhost' IDENTIFIED BY 'secure_password_123';
GRANT ALL PRIVILEGES ON ttcrypto_db.* TO 'ttcrypto'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 2. Import Database Schema

```bash
# From the project root directory
cd database

# Import schema and sample data
mysql -u root -p ttcrypto_db < schema.sql

# Or if using custom user:
mysql -u ttcrypto -p ttcrypto_db < schema.sql
```

### 3. Verify Database

```bash
# Connect to verify
mysql -u root -p

# Run these queries:
USE ttcrypto_db;
SHOW TABLES;
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM wallets;
EXIT;
```

**Expected Output:**
- 5 tables created (users, wallets, orders, trades, transactions)
- 3 users in the users table
- Multiple wallets for demo users

## Backend Setup

### 1. Navigate to Backend Directory

```bash
cd backend
```

### 2. Configure Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/ttcrypto_db
spring.datasource.username=root
spring.datasource.password=your_mysql_password

# OR if using custom user:
spring.datasource.username=ttcrypto
spring.datasource.password=secure_password_123

# JWT Configuration (keep as is or change secret)
jwt.secret=mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForHS256Algorithm123456789
jwt.expiration=86400000

# CORS Configuration (adjust for production)
cors.allowed-origins=http://localhost:3000,http://localhost:5173
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true

# WebSocket
websocket.allowed-origins=http://localhost:3000,http://localhost:5173
```

### 3. Build Backend

```bash
# Clean and build
mvn clean package

# This will:
# - Download all dependencies
# - Compile Java code
# - Run tests (if any)
# - Create JAR file in target/ directory

# Build time: ~2-5 minutes (depending on internet speed)
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time:  XX.XXs
```

### 4. Fix Port Conflicts (if needed)

If port 8080 is already in use:

```properties
# In application.properties, change:
server.port=8081
```

## Frontend Setup

### 1. Navigate to Frontend Directory

```bash
cd frontend
```

### 2. Create Environment File

```bash
# Copy the example file
cp .env.example .env

# Edit .env if needed (usually defaults work fine)
cat .env
```

Expected content:
```env
VITE_API_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws/trading
```

### 3. Install Dependencies

```bash
# Install npm packages
npm install

# This will:
# - Download all dependencies from npm registry
# - Create node_modules/ directory
# - Generate package-lock.json

# Installation time: ~1-3 minutes
```

### 4. Verify Installation

```bash
# Check if all packages installed correctly
npm list | head -20

# Verify specific packages
npm list react react-router-dom axios recharts zustand
```

## Running the Applications

### Start MySQL (if not already running)

```bash
# Windows
net start MySQL80

# macOS
brew services start mysql

# Linux
sudo service mysql start

# Verify connection
mysql -u root -p -e "SELECT 1;"
```

### Start Backend Server

```bash
# From backend directory
cd backend

# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using Java JAR (after build)
java -jar target/ttcrypto-trading-platform-1.0.0.jar

# Expected output:
# Started TtcryptoApplication in X.XXX seconds
# Server running on http://localhost:8080
```

Keep this terminal open.

### Start Frontend Development Server

```bash
# From frontend directory (new terminal)
cd frontend

# Start development server
npm run dev

# Expected output:
# VITE v5.0.0  ready in XXX ms
# ➜  Local:   http://localhost:5173/
# ➜  press h to show help
```

Keep this terminal open.

### Access the Application

Open your browser and navigate to:
```
http://localhost:5173
```

You should see the login page.

## Testing

### Test Demo User Login

1. Navigate to http://localhost:5173
2. Use these credentials:
   - Email: `demo@example.com`
   - Password: `password`
3. Click "Login"

Expected: Should redirect to dashboard

### Test Trading Flow

1. Go to Dashboard (should see portfolio summary)
2. Click on "Start Trading" or navigate to a trading pair
3. View:
   - Price chart
   - Order book
   - Recent trades
4. Try placing an order:
   - Select BUY
   - Type: MARKET
   - Quantity: 0.1
   - Click "BUY BTC/USDT"

### Test API Endpoints

```bash
# Get current price
curl http://localhost:8080/api/v1/market/price/BTC/USDT

# Get order book
curl http://localhost:8080/api/v1/market/orderbook/BTC/USDT

# Login and get token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"demo","password":"password"}'

# Should return JWT token in response
```

### Check Logs

**Backend Logs:**
- Check console where `mvn spring-boot:run` is running
- Look for any ERROR messages
- Check database connection logs

**Frontend Logs:**
1. Open browser DevTools (F12)
2. Check Console tab for JavaScript errors
3. Check Network tab for API calls

## Troubleshooting

### Database Connection Issues

**Error: "Access denied for user 'root'@'localhost'"**
```bash
# Solution 1: Verify MySQL is running
mysql -u root -p

# Solution 2: Check password
# Update in application.properties with correct password

# Solution 3: Reset MySQL password
# macOS: mysql_secure_installation
# Windows: Navigate to MySQL installation and run password utility
```

**Error: "Database 'ttcrypto_db' doesn't exist"**
```bash
# Create database
mysql -u root -p -e "CREATE DATABASE ttcrypto_db;"

# Then import schema
mysql -u root -p ttcrypto_db < database/schema.sql
```

### Backend Won't Start

**Error: "Port 8080 already in use"**
```bash
# Option 1: Kill process using port
# Windows: netstat -ano | findstr :8080
# macOS/Linux: lsof -i :8080

# Option 2: Change port in application.properties
server.port=8081
```

**Error: "Maven build failure"**
```bash
# Clear Maven cache and rebuild
mvn clean package -DskipTests

# If still failing, check Java version
java -version  # Should be 17+
```

### Frontend Won't Load

**Error: "Cannot find module" or npm errors**
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

**Error: "API Connection Failed"**
```bash
# Check backend is running on :8080
curl http://localhost:8080/api/v1/market/price/BTC/USDT

# Check API_URL in .env
cat frontend/.env

# Check browser console (F12) for CORS errors
```

**Error: "WebSocket connection failed"**
```bash
# Verify WebSocket endpoint is accessible
# Check that backend is running
# Look for error in browser console
```

### Login Issues

**Error: "Invalid credentials"**
- Verify correct demo credentials:
  - Email: `demo@example.com`
  - Password: `password`
- Check database has sample data:
  ```sql
  SELECT email, username FROM users;
  ```

**Error: "Cannot GET /dashboard" after login**
- Verify frontend is running properly
- Check React Router configuration
- Clear browser cache (Ctrl+Shift+Delete)

### Performance Issues

**Slow API responses**
```bash
# Check database indexes
mysql -u root -p
USE ttcrypto_db;
SHOW INDEX FROM orders;
SHOW INDEX FROM trades;
```

**High CPU usage**
- Reduce logging level: Change `logging.level.com.ttcrypto=INFO`
- Check for infinite loops in code
- Restart services

## Quick Reference Commands

```bash
# Start all services (3 terminals needed)
# Terminal 1: MySQL
mysql -u root -p

# Terminal 2: Backend
cd backend && mvn spring-boot:run

# Terminal 3: Frontend
cd frontend && npm run dev

# Stop services
# Ctrl+C in each terminal

# View logs
tail -f backend.log
tail -f frontend.log

# Test API
curl -I http://localhost:8080/api/v1/auth/me

# Access database
mysql -u root -p ttcrypto_db

# Check ports
netstat -tupln | grep -E "8080|5173|3306"
```

## Next Steps

1. **Explore the Dashboard**: Familiarize yourself with the UI
2. **Try Trading**: Place market and limit orders
3. **Check Portfolio**: View your holdings and P&L
4. **Read Documentation**: Check [backend/README.md](backend/README.md) and [frontend/README.md](frontend/README.md)
5. **Review Code**: Understand the architecture and implementation
6. **Customize**: Modify colors, trading pairs, or features as needed

## Support

- Check the main [README.md](README.md) for architecture overview
- Check [backend/README.md](backend/README.md) for backend-specific issues
- Check [frontend/README.md](frontend/README.md) for frontend-specific issues
- Review browser console (F12) for client-side errors
- Check backend console for server-side errors

## Production Deployment

Once you're comfortable with the local setup:
1. See [Docker Deployment](README.md#-production-deployment) section in README
2. Configure proper environment variables
3. Set up HTTPS/TLS
4. Configure database backups
5. Set up monitoring and alerting

---

**You're all set!** The platform should now be running on your local machine. 🚀
