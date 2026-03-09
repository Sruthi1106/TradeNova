# TT Crypto Platform - Quick Reference Guide

## Project Structure

```
ttcrypto/
â”œâ”€â”€ backend/                      # Spring Boot API
â”‚   â”œâ”€â”€ src/main/java/com/ttcrypto/
â”‚   â”‚   â”œâ”€â”€ config/              # Security, CORS, WebSocket configs
â”‚   â”‚   â”œâ”€â”€ controller/          # REST endpoints
â”‚   â”‚   â”œâ”€â”€ service/             # Business logic
â”‚   â”‚   â”œâ”€â”€ repository/          # Data access
â”‚   â”‚   â”œâ”€â”€ entity/              # JPA entities
â”‚   â”‚   â”œâ”€â”€ dto/                 # Data transfer objects
â”‚   â”‚   â”œâ”€â”€ enums/               # Domain enums
â”‚   â”‚   â””â”€â”€ security/            # JWT & Auth
â”‚   â”œâ”€â”€ src/main/resources/
â”‚   â”‚   â”œâ”€â”€ application.properties         # Production (Aiven)
â”‚   â”‚   â”œâ”€â”€ application-dev.properties     # Local development
â”‚   â”‚   â””â”€â”€ application-prod.properties    # Production (optimized)
â”‚   â””â”€â”€ pom.xml                  # Maven dependencies
â”œâ”€â”€ frontend/                    # React + Vite
â”‚   â”œâ”€â”€ src/
â”‚   â”‚   â”œâ”€â”€ pages/              # Page components (Login, Dashboard, etc.)
â”‚   â”‚   â”œâ”€â”€ components/         # Reusable UI components
â”‚   â”‚   â”œâ”€â”€ services/           # API client, WebSocket
â”‚   â”‚   â”œâ”€â”€ stores/             # Zustand state management
â”‚   â”‚   â””â”€â”€ App.jsx             # Main app component
â”‚   â”œâ”€â”€ package.json            # NPM dependencies
â”‚   â”œâ”€â”€ vite.config.js          # Build configuration
â”‚   â””â”€â”€ tailwind.config.js       # CSS framework
â”œâ”€â”€ database/
â”‚   â”œâ”€â”€ aiven_schema.sql        # Database schema (idempotent)
â”‚   â””â”€â”€ schema.sql              # Original schema
â”œâ”€â”€ .env.prod.example           # Environment template
â”œâ”€â”€ Dockerfile                  # Development Dockerfile
â”œâ”€â”€ Dockerfile.prod             # Production Dockerfile
â”œâ”€â”€ docker-compose.yml          # Local development
â”œâ”€â”€ docker-compose.prod.yml     # Production compose
â”œâ”€â”€ AIVEN_SETUP.md             # Aiven setup guide
â”œâ”€â”€ DEPLOYMENT.md              # Production deployment guide
â”œâ”€â”€ SETUP.md                   # Initial setup guide
â””â”€â”€ README.md                  # Project overview
```

## Quick Commands

### Initial Setup

```bash
# Clone and navigate
cd ttcrypto

# Frontend setup
cd frontend && npm install && npm run dev

# Backend setup (new terminal)
cd backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Database setup
mysql -h localhost -u root -p < database/aiven_schema.sql
```

### Development

**Backend (Spring Boot)**:
```bash
cd backend
mvn clean package                    # Build
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"  # Run (local MySQL)
mvn test                             # Tests
mvn clean package -DskipTests        # Build without tests
```

**Frontend (React)**:
```bash
cd frontend
npm install                          # Dependencies
npm run dev                          # Start dev server (port 5173)
npm run build                        # Production build
npm run preview                      # Preview built app
npm run lint                         # Lint code
```

### Database

```bash
# Local MySQL (Docker)
docker run -d -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=ttcrypto_db \
  -p 3306:3306 mysql:8.0

# Initialize schema
mysql -h localhost -u root -p < database/aiven_schema.sql

# Connect to Aiven
mysql -h <AIVEN_HOST> -P <AIVEN_PORT> \
      -u avnadmin -p"<AIVEN_PASSWORD>"

# Backup database
mysqldump -h localhost -u root -p ttcrypto_db > backup.sql
```

### Docker & Production

```bash
# Build images
docker build -f Dockerfile.prod -t ttcrypto-backend:latest .
docker build -t ttcrypto-frontend:latest ./frontend

# Run with Docker Compose (Local)
docker-compose up -d

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Stop services
docker-compose down

# Production deployment
docker-compose -f docker-compose.prod.yml up -d
```

## Key Endpoints

### Authentication

```bash
# Register
POST /api/v1/auth/register
Content-Type: application/json
{
  "email": "user@example.com",
  "username": "username",
  "password": "password",
  "firstName": "John",
  "lastName": "Doe"
}

# Login
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "password"
}

# Get current user
GET /api/v1/auth/me
Authorization: Bearer JWT_TOKEN
```

### Orders

```bash
# Create order
POST /api/v1/orders
Authorization: Bearer JWT_TOKEN
{
  "tradingPair": "BTC/USDT",
  "type": "LIMIT",
  "side": "BUY",
  "quantity": 0.5,
  "price": 45000
}

# Get orders
GET /api/v1/orders?page=0&size=20
Authorization: Bearer JWT_TOKEN

# Cancel order
DELETE /api/v1/orders/{orderId}
Authorization: Bearer JWT_TOKEN
```

### Market Data

```bash
# Get prices
GET /api/v1/market/prices?pairs=BTC/USDT,ETH/USDT

# Get order book
GET /api/v1/market/orderbook?pair=BTC/USDT&limit=20

# Get candlestick data
GET /api/v1/market/candles?pair=BTC/USDT&interval=5m
```

### Portfolio

```bash
# Get portfolio
GET /api/v1/portfolio
Authorization: Bearer JWT_TOKEN
```

### Trades

```bash
# Get trade history
GET /api/v1/trades?page=0&size=20
Authorization: Bearer JWT_TOKEN
```

## Demo Credentials

```
User 1 - Regular Trader
Email: demo@example.com
Password: password
Wallet: 10,000 USDT, 0.5 BTC, 10 ETH, 50 BNB, 5,000 XRP

User 2 - Admin
Email: admin@example.com
Password: password
Wallet: 50,000 USDT, 5 BTC, 100 ETH, 500 BNB, 50,000 XRP

User 3 - Trader
Email: trader@example.com
Password: password
Wallet: 15,000 USDT, 1 BTC, 25 ETH, 100 BNB, 10,000 XRP
```

## Configuration

### Spring Boot Profiles

```properties
# Development (local MySQL)
-Dspring.profiles.active=dev

# Production (Aiven Cloud)
-Dspring.profiles.active=prod

# Test
-Dspring.profiles.active=test
```

### Environment Variables

```bash
# Set profile
export SPRING_PROFILES_ACTIVE=prod

# Database (if not in .env.prod)
export SPRING_DATASOURCE_URL=jdbc:mysql://...
export SPRING_DATASOURCE_USERNAME=avnadmin
export SPRING_DATASOURCE_PASSWORD=password

# JVM
export JVM_OPTS="-Xmx512m -Xms256m"
```

## Ports & Services

| Service | Development | Production |
|---------|-------------|------------|
| Backend API | 8080 | 8080 |
| Frontend | 5173 | 80/443 |
| MySQL | 3306 | Aiven (<AIVEN_PORT>) |
| PhpMyAdmin | 8081 | - |
| WebSocket | ws://localhost:8080/ws | wss://api.domain.com/ws |

## Testing

### Manual API Testing

```bash
# Using curl
curl -X GET http://localhost:8080/api/v1/market/prices?pairs=BTC/USDT

# Using Postman
# Import collection from postman/ folder
# Set environment variables for token, base_url, etc.
```

### Unit Tests

```bash
cd backend
mvn test
mvn test -Dtest=OrderServiceTest
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 8080 in use | `lsof -i :8080` then kill process or change port |
| Database connection error | Check MySQL running, verify credentials |
| JWT token expired | Re-login to get new token |
| CORS error | Verify frontend URL in SPRING_WEB_CORS_ALLOWED_ORIGINS |
| WebSocket won't connect | Check browser console, verify WSS for HTTPS |
| Out of memory | Increase JVM heap: `JVM_OPTS="-Xmx2g"` |

## Useful Links

- **Aiven Console**: https://console.aiven.io
- **Local: Frontend**: http://localhost:5173
- **Local: Backend**: http://localhost:8080/api/v1
- **Local: PhpMyAdmin**: http://localhost:8081
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **React Docs**: https://react.dev
- **WebSocket Docs**: https://spring.io/guides/gs/messaging-stomp-websocket/

## Performance Monitoring

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# JVM info
curl http://localhost:8080/actuator/info
```

## Security Checklist

- [ ] JWT Secret generated (32+ characters)
- [ ] CORS origins properly configured
- [ ] SSL/TLS enabled for production
- [ ] Database credentials not in code
- [ ] API rate limiting enabled
- [ ] HTTPS enforced (redirect HTTP â†’ HTTPS)
- [ ] Security headers set (X-Frame-Options, CSP, etc.)
- [ ] Authentication required for sensitive endpoints
- [ ] Sensitive data logged securely

## Deployment Quick Reference

```bash
# 1. Prepare
cp .env.prod.example .env.prod
# Edit .env.prod with actual credentials

# 2. Initialize Database
mysql -h aiven-host -u avnadmin -p < database/aiven_schema.sql

# 3. Build & Deploy
docker build -f Dockerfile.prod -t ttcrypto-backend:latest .
docker-compose -f docker-compose.prod.yml up -d

# 4. Verify
curl http://localhost:8080/actuator/health
```

## Release Checklist

- [ ] Update version in pom.xml
- [ ] Update version in package.json
- [ ] Run full test suite
- [ ] Update CHANGELOG.md
- [ ] Tag release in Git: `git tag -a v1.0.0 -m "Release v1.0.0"`
- [ ] Build Docker images with version tag
- [ ] Push to registry
- [ ] Update deployment manifests
- [ ] Deploy to production
- [ ] Verify all endpoints working
- [ ] Monitor application logs

## Emergency Contacts

- **On-Call Backend Engineer**: [contact]
- **On-Call DevOps**: [contact]
- **Aiven Support**: support@aiven.io
- **AWS Support**: https://console.aws.amazon.com/support

---

**Last Updated**: 2024 | **Version**: 1.0.0

