# TT Crypto Trading Platform

A production-ready full-stack cryptocurrency trading platform built with Spring Boot, React, and Aiven Cloud MySQL. Real-time order matching, portfolio tracking, and WebSocket-powered live market data.

**Live Features**: Real-time charting, limit & market orders, order matching engine, wallet management, portfolio tracking, JWT authentication

## Quick Start (5 Minutes)

### Option 1: Using Docker (Recommended)

```bash
# Clone repository
git clone https://github.com/yourusername/ttcrypto.git
cd ttcrypto

# Start full stack
docker-compose -f docker-compose.dev.yml up

# Access:
# Frontend: http://localhost:5173
# Backend: http://localhost:8080/api/v1
# Database: http://localhost:8081 (PhpMyAdmin)
```

### Option 2: Local Development

```bash
# Terminal 1: Database
docker run -d -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=ttcrypto_db \
  -p 3306:3306 mysql:8.0
mysql -h localhost -u root -p < database/aiven_schema.sql

# Terminal 2: Backend
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Terminal 3: Frontend
cd frontend
npm install && npm run dev
```

**Demo Credentials**:
- Email: `demo@example.com`
- Password: `password`

## Project Overview

### Technology Stack

**Backend**:
- Framework: Spring Boot 3.2.0 (Java 17+)
- Authentication: JWT (io.jsonwebtoken)
- Real-time: Spring WebSocket + SockJS/STOMP
- Database: MySQL 8.0 (Aiven Cloud managed)
- ORM: Spring Data JPA + Hibernate
- Build: Maven 3.9+

**Frontend**:
- Framework: React 18.2.0
- Build Tool: Vite 5.0.0
- Styling: Tailwind CSS 3.4.0
- State Management: Zustand 4.4.0
- Charts: Recharts 2.10.0
- HTTP Client: Axios 1.6.0
- WebSocket: SockJS + Stomp.js

**Cloud Infrastructure**:
- Database: Aiven Cloud MySQL (managed service)
- Deployment: Docker + Docker Compose / Kubernetes
- CDN: CloudFront / Netlify (frontend)
- Monitoring: Prometheus + Grafana (optional)

### Architecture

```
┌─ Frontend (React + Vite) ─┐
│  - Trading Dashboard      │ ──┐
│  - Portfolio Page         │   │
│  - Order Management       │   │
│  - Real-time Charts       │   │ HTTP/WebSocket
│  - Authentication UI      │   │
└───────────────────────────┘   │
                                 ▼
                        ┌─ Backend (Spring Boot) ─┐
                        │  - REST API              │
                        │  - WebSocket Server      │
                        │  - JWT Authentication    │
                        │  - Order Matching Engine │
                        │  - Portfolio Calculation │
                        │  - Wallet Management     │
                        └──────────────┬───────────┘
                                       │ JDBC
                                       ▼
                        ┌─ Aiven MySQL Database ──┐
                        │  - Users & Wallets       │
                        │  - Orders & Trades       │
                        │  - Transactions History  │
                        │  - Market Snapshots      │
                        └──────────────────────────┘
```

## Core Features

### 🔐 Authentication & Security
- JWT token-based authentication (24-hour expiration)
- BCrypt password hashing
- Role-based access control (USER, ADMIN)
- CORS configuration for cross-origin requests
- Spring Security integration

### 💰 Trading Features
- **Order Types**: Market orders (instant execution) and Limit orders (price-triggered)
- **Order Sides**: Buy and Sell orders
- **Order Matching Engine**: Automatic matching of buy/sell orders
  - Highest buy price matches lowest sell price
  - Partial order filling support
  - FIFO queue for same-price orders
- **Trading Pairs**: BTC/USDT, ETH/USDT, BNB/USDT, XRP/USDT

### 📊 Real-Time Market Data
- **Candlestick Charts**: 5-minute, 15-minute, 1-hour, 4-hour, 1-day intervals
- **Order Book**: Real-time buy/sell order aggregation (top 20 levels)
- **Recent Trades**: Live trade execution feed (WebSocket)
- **Price Ticks**: Real-time price updates

### 👛 Wallet Management
- Multi-currency support (USDT, BTC, ETH, BNB, XRP)
- Balance tracking (available + locked balance)
- Atomic balance operations
- Transaction history with audit trail
- Automatic balance locking for open orders

### 📈 Portfolio Management
- Real-time portfolio value calculation
- Holdings aggregation by currency
- Profit/Loss (P&L) calculation
- Asset allocation visualization
- Performance metrics

### 📱 User Interface
- Dark theme (professional trading platform aesthetic)
- Responsive design (desktop, tablet, mobile)
- Real-time chart updates via WebSocket
- Order book depth visualization
- Responsive order form with validation

## File Structure

```
ttcrypto/
├── backend/                                  # Spring Boot API
│   ├── src/main/java/com/ttcrypto/
│   │   ├── config/                          # Security, CORS, WebSocket
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── WebSocketConfig.java
│   │   │   └── JwtTokenProvider.java
│   │   ├── controller/                      # REST Endpoints
│   │   │   ├── AuthController.java
│   │   │   ├── OrderController.java
│   │   │   ├── MarketController.java
│   │   │   ├── TradeController.java
│   │   │   ├── PortfolioController.java
│   │   │   └── WebSocketController.java
│   │   ├── service/                         # Business Logic
│   │   │   ├── AuthService.java
│   │   │   ├── OrderService.java
│   │   │   ├── OrderMatchingService.java
│   │   │   ├── WalletService.java
│   │   │   ├── MarketDataService.java
│   │   │   ├── TradeService.java
│   │   │   └── PortfolioService.java
│   │   ├── entity/                          # JPA Entities
│   │   │   ├── User.java
│   │   │   ├── Wallet.java
│   │   │   ├── Order.java
│   │   │   ├── Trade.java
│   │   │   └── Transaction.java
│   │   ├── dto/                             # Data Transfer Objects
│   │   │   ├── AuthDto.java
│   │   │   ├── OrderDto.java
│   │   │   ├── TradeDto.java
│   │   │   ├── PriceTickDto.java
│   │   │   └── PortfolioDto.java
│   │   ├── repository/                      # Data Access
│   │   └── enums/                           # Domain Enums
│   ├── src/main/resources/
│   │   └── application*.properties          # Configuration
│   └── pom.xml
│
├── frontend/                                # React Application
│   ├── src/
│   │   ├── pages/
│   │   │   ├── LoginPage.jsx
│   │   │   ├── RegisterPage.jsx
│   │   │   ├── DashboardPage.jsx
│   │   │   ├── TradingPage.jsx
│   │   │   └── PortfolioPage.jsx
│   │   ├── components/
│   │   │   ├── Navbar.jsx
│   │   │   ├── ProtectedRoute.jsx
│   │   │   ├── CandlestickChart.jsx
│   │   │   ├── OrderBook.jsx
│   │   │   ├── OrderForm.jsx
│   │   │   └── RecentTrades.jsx
│   │   ├── stores/                          # Zustand State
│   │   │   ├── authStore.js
│   │   │   ├── marketStore.js
│   │   │   └── portfolioStore.js
│   │   ├── services/
│   │   │   ├── api.js                       # Axios client
│   │   │   └── websocket.js                 # STOMP client
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── tailwind.config.js
│
├── database/
│   ├── aiven_schema.sql                     # Production schema
│   └── schema.sql                           # Development schema
│
├── .env.prod.example                        # Environment template
├── .env.dev.example
├── Dockerfile                               # Development build
├── Dockerfile.prod                          # Production build
├── docker-compose.yml                       # Development compose
├── docker-compose.dev.yml                   # Local dev environment
├── docker-compose.prod.yml                  # Production compose
│
├── QUICKREF.md                              # Quick reference guide
├── README.md                                # This file
├── SETUP.md                                 # Initial setup guide
├── AIVEN_SETUP.md                           # Aiven cloud setup
└── DEPLOYMENT.md                            # Production deployment
```

## API Endpoints

### Authentication
```
POST   /api/v1/auth/register          - Register new user
POST   /api/v1/auth/login            - Login (returns JWT)
GET    /api/v1/auth/me               - Get current user (requires auth)
```

### Orders
```
POST   /api/v1/orders                - Create new order
GET    /api/v1/orders                - List user's orders (paginated)
GET    /api/v1/orders/{orderId}      - Get single order
DELETE /api/v1/orders/{orderId}      - Cancel order
```

### Market Data
```
GET    /api/v1/market/prices        - Get real-time prices
GET    /api/v1/market/orderbook      - Get order book for pair
GET    /api/v1/market/candles        - Get candlestick data
GET    /api/v1/market/trades         - Get recent trades
```

### Trading
```
GET    /api/v1/trades                - Get user's trade history
GET    /api/v1/trades/{tradeId}      - Get single trade
```

### Portfolio
```
GET    /api/v1/portfolio             - Get user portfolio
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  username VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  total_balance DECIMAL(18,8),
  available_balance DECIMAL(18,8),
  role ENUM('USER', 'ADMIN'),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### Wallets Table
```sql
CREATE TABLE wallets (
  id BIGINT PRIMARY KEY,
  user_id BIGINT FOREIGN KEY,
  currency VARCHAR(10),
  balance DECIMAL(18,8),
  locked_balance DECIMAL(18,8),
  UNIQUE(user_id, currency)
);
```

### Orders Table
```sql
CREATE TABLE orders (
  id BIGINT PRIMARY KEY,
  user_id BIGINT FOREIGN KEY,
  trading_pair VARCHAR(20),
  type ENUM('MARKET', 'LIMIT'),
  side ENUM('BUY', 'SELL'),
  quantity DECIMAL(18,8),
  price DECIMAL(18,8),
  filled_quantity DECIMAL(18,8),
  status ENUM('PENDING', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED'),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### Trades Table
```sql
CREATE TABLE trades (
  id BIGINT PRIMARY KEY,
  buyer_id BIGINT FOREIGN KEY,
  seller_id BIGINT FOREIGN KEY,
  buy_order_id BIGINT FOREIGN KEY,
  sell_order_id BIGINT FOREIGN KEY,
  quantity DECIMAL(18,8),
  price DECIMAL(18,8),
  created_at TIMESTAMP
);
```

### Transactions Table
```sql
CREATE TABLE transactions (
  id BIGINT PRIMARY KEY,
  user_id BIGINT FOREIGN KEY,
  currency VARCHAR(10),
  transaction_type ENUM('BUY', 'SELL', 'DEPOSIT', 'WITHDRAWAL'),
  amount DECIMAL(18,8),
  description VARCHAR(255),
  created_at TIMESTAMP
);
```

## Setup Instructions

### Prerequisites
- Docker & Docker Compose (recommended)
- Java 17+ & Maven 3.9+ (for local development)
- Node.js 18+ & npm (for local frontend development)
- MySQL 8.0 (if running locally without Docker)

### Local Development Setup

#### 1. Clone Repository
```bash
git clone https://github.com/yourusername/ttcrypto.git
cd ttcrypto
```

#### 2. Start with Docker (Easiest)
```bash
docker-compose -f docker-compose.dev.yml up
```

#### 3. Manual Setup
```bash
# Start MySQL
docker run -d -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=ttcrypto_db \
  -p 3306:3306 mysql:8.0

# Initialize database
mysql -h localhost -u root -p < database/aiven_schema.sql

# Backend (Terminal 1)
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Frontend (Terminal 2)
cd frontend
npm install
npm run dev
```

**Access Application**:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api/v1
- PhpMyAdmin: http://localhost:8081

### Production Deployment

For production deployment to Aiven Cloud, see [DEPLOYMENT.md](DEPLOYMENT.md)

**Quick Summary**:
1. Set up Aiven MySQL instance
2. Initialize database schema: `mysql < database/aiven_schema.sql`
3. Update `.env.prod` with credentials
4. Deploy: `docker-compose -f docker-compose.prod.yml up -d`

## Development Workflow

### Backend Development

```bash
cd backend

# Build project
mvn clean install

# Run with local MySQL
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Run tests
mvn test

# Create JAR for production
mvn clean package -DskipTests
```

### Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start dev server (with hot reload)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Database Management

```bash
# PhpMyAdmin GUI: http://localhost:8081

# Or MySQL CLI
mysql -h localhost -u ttcrypto -p ttcrypto_db

# Common queries
SELECT * FROM users;
SELECT * FROM orders WHERE status != 'FILLED';
SELECT COUNT(*) FROM trades;
```

## Testing

### Manual Testing with curl

```bash
# Register user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","username":"testuser","password":"password"}'

# Login
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' | jq -r '.token')

# Get user portfolio
curl -X GET http://localhost:8080/api/v1/portfolio \
  -H "Authorization: Bearer $TOKEN"
```

### Automated Tests

```bash
# Backend tests
cd backend && mvn test

# Frontend tests
cd frontend && npm test
```

## Troubleshooting

### Issue: Port 8080 Already in Use
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in properties
SPRING_SERVER_PORT=8090
```

### Issue: Database Connection Failed
```bash
# Verify MySQL is running
docker ps | grep mysql

# Check connection string
mysql -h localhost -u root -p -e "SELECT version();"
```

### Issue: WebSocket Connection Error
- Check browser console for errors
- Verify WebSocket URL in frontend .env
- Ensure backend is running and WebSocket is enabled

### Issue: JWT Token Expired
- Re-login to get new token
- Token expiration is 24 hours by default
- Can be changed in `application.properties`: `jwt.expiration-ms`

## Performance Tips

### Backend
- Connection pool size: 20 (production)
- Batch size: 20 queries
- Caching: Enable Redis for frequent queries
- Load balancing: Use 3+ replicas in production

### Frontend
- Code splitting: Built-in with Vite
- Asset optimization: Images optimized, CSS minified
- Lazy loading: Pages loaded on-demand
- CDN: Recommended for static assets

### Database
- Indexes on: email, username, user_id, trading_pair, status
- Batch inserts: Use for trades and transactions
- Connection pooling: HikariCP with 20 max connections
- Backups: Daily via Aiven (automatic)

## Contributing

1. Fork repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open Pull Request

## License

This project is licensed under the MIT License - see LICENSE file for details.

## Support

- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions
- **Documentation**: See [AIVEN_SETUP.md](AIVEN_SETUP.md) and [DEPLOYMENT.md](DEPLOYMENT.md)
- **Quick Reference**: See [QUICKREF.md](QUICKREF.md)

## Project Status

✅ **Production Ready** - All core features implemented and tested

### Completed Features
- [x] Backend API with Spring Boot
- [x] Frontend UI with React
- [x] JWT Authentication
- [x] Order Management
- [x] Order Matching Engine
- [x] Portfolio Tracking
- [x] Real-time WebSocket
- [x] Wallet Management
- [x] Aiven Cloud Integration
- [x] Production Deployment Guide
- [x] Docker Support
- [x] Comprehensive Documentation

### Future Enhancements
- [ ] Email notifications
- [ ] Mobile app (React Native)
- [ ] Advanced charting (Lightweight Charts)
- [ ] Margin trading
- [ ] Staking features
- [ ] API rate limiting
- [ ] Analytics dashboard

## Version History

**v1.0.0** (2024)
- Initial release with all core trading features
- Aiven Cloud MySQL integration
- Production-ready deployment

---

**Built with ❤️ for cryptocurrency traders**

For detailed setup and deployment instructions, see [SETUP.md](SETUP.md), [AIVEN_SETUP.md](AIVEN_SETUP.md), and [DEPLOYMENT.md](DEPLOYMENT.md).
