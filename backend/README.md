# TT Crypto Trading Platform - Backend

A production-ready Spring Boot backend for a real-time cryptocurrency trading platform with WebSocket support, order matching engine, and comprehensive API endpoints.

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17+
- **Database**: MySQL 8.0+
- **Build Tool**: Maven
- **Authentication**: JWT (JSON Web Tokens)
- **Real-time Communication**: WebSocket
- **ORM**: Spring Data JPA/Hibernate

## Project Structure

```
backend/
├── src/main/java/com/ttcrypto/
│   ├── config/          # Configuration classes (Security, CORS, WebSocket)
│   ├── controller/      # REST API endpoints
│   ├── dto/            # Data Transfer Objects
│   ├── entity/         # JPA entities
│   ├── exception/      # Custom exceptions and handlers
│   ├── repository/     # Data access layer (JPA Repositories)
│   ├── security/       # JWT and authentication
│   ├── service/        # Business logic layer
│   ├── websocket/      # WebSocket controllers
│   └── TtcryptoApplication.java
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/
├── pom.xml
└── README.md
```

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- MySQL 8.0+
- Git

## Setup Instructions

### 1. Database Setup

```bash
# Create database and tables
mysql -u root -p < database/schema.sql
```

The script creates:
- `users` table
- `wallets` table
- `orders` table
- `trades` table
- `transactions` table
- Sample data for testing

### 2. Configure Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Update these values for your environment
spring.datasource.url=jdbc:mysql://localhost:3306/ttcrypto_db
spring.datasource.username=root
spring.datasource.password=your_password

# JWT Configuration
jwt.secret=mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForHS256Algorithm123456789
jwt.expiration=86400000

# CORS & WebSocket Origins
cors.allowed-origins=http://localhost:3000,http://localhost:5173
websocket.allowed-origins=http://localhost:3000,http://localhost:5173
```

### 3. Build the Application

```bash
cd backend
mvn clean package
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login user
- `GET /api/v1/auth/me` - Get current user info

### Market Data
- `GET /api/v1/market/price/{tradingPair}` - Get current price
- `GET /api/v1/market/orderbook/{tradingPair}` - Get order book
- `GET /api/v1/market/candlesticks/{tradingPair}` - Get candlestick data
- `GET /api/v1/market/supported-pairs` - Get supported trading pairs

### Orders
- `POST /api/v1/orders` - Create new order
- `GET /api/v1/orders` - Get user's orders
- `GET /api/v1/orders/{orderId}` - Get specific order
- `DELETE /api/v1/orders/{orderId}` - Cancel order
- `GET /api/v1/orders/pair/{tradingPair}` - Get orders for trading pair

### Trades
- `GET /api/v1/trades` - Get user's trades
- `GET /api/v1/trades/pair/{tradingPair}` - Get trades for pair
- `GET /api/v1/trades/pair/{tradingPair}/recent` - Get recent trades

### Portfolio
- `GET /api/v1/portfolio` - Get user portfolio
- `GET /api/v1/portfolio/value` - Get portfolio total value

## WebSocket Endpoints

### Connect
```
ws://localhost:8080/ws/trading
```

### Topics
- `/topic/price/{tradingPair}` - Price updates
- `/topic/orders/{tradingPair}` - Order book updates
- `/topic/trades` - New trades
- `/topic/market` - General market data

## Core Services

### AuthService
Handles user registration, login, and JWT token generation.

**Key Methods:**
- `register(RegisterRequest)` - Create new user account
- `login(LoginRequest)` - Authenticate and generate token
- `getCurrentUser(String username)` - Fetch user details

### OrderService
Manages order creation, cancellation, and retrieval.

**Key Methods:**
- `createOrder(Long userId, CreateOrderRequest)` - Place new order
- `cancelOrder(Long orderId, Long userId)` - Cancel order
- `getUserOrders(Long userId, Pageable)` - Get user's orders

### OrderMatchingService
Implements order matching engine for automatic trade execution.

**Key Methods:**
- `matchOrders(String tradingPair)` - Match buy and sell orders
- `executeTrade(Order, Order, BigDecimal)` - Execute matched trade

### MarketDataService
Provides market data and price simulations.

**Key Methods:**
- `getCurrentPrice(String tradingPair)` - Get current price
- `getOrderBook(String tradingPair)` - Get order book
- `getCandlesticks(...)` - Get candlestick data

### WalletService
Manages user balances and fund transfers.

**Key Methods:**
- `getWallet(Long userId, String currency)` - Get wallet
- `addBalance(Long userId, String currency, BigDecimal amount)`
- `deductBalance(Long userId, String currency, BigDecimal amount)`
- `lockBalance(Long userId, String currency, BigDecimal amount)`

### PortfolioService
Calculates portfolio value and holdings.

**Key Methods:**
- `getUserPortfolio(Long userId)` - Get portfolio overview
- `getPortfolioValue(Long userId)` - Get total portfolio value

## Database Schema

### Users Table
- `id` - User ID (PK)
- `email` - Unique email
- `username` - Unique username
- `password` - Hashed password
- `first_name, last_name` - User name
- `total_balance` - Total USDT balance
- `available_balance` - Available for trading
- `role` - USER or ADMIN
- `is_active` - Account status

### Wallets Table
- `id` - Wallet ID (PK)
- `user_id` - Reference to user (FK)
- `currency` - Cryptocurrency code (BTC, ETH, etc.)
- `balance` - Available balance
- `locked_balance` - Balance locked in open orders

### Orders Table
- `id` - Order ID (PK)
- `user_id` - Reference to user (FK)
- `trading_pair` - Trading pair (BTC/USDT)
- `type` - MARKET or LIMIT
- `side` - BUY or SELL
- `quantity` - Order quantity
- `price` - Order price (for limit orders)
- `filled_quantity` - Amount already filled
- `status` - PENDING, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED

### Trades Table
- `id` - Trade ID (PK)
- `buyer_id, seller_id` - User references (FK)
- `buy_order_id, sell_order_id` - Order references (FK)
- `quantity, price, total_value` - Trade details

### Transactions Table
- `id` - Transaction ID (PK)
- `user_id` - Reference to user (FK)
- `currency` - Affected currency
- `transaction_type` - BUY, SELL, DEPOSIT, WITHDRAWAL, TRADING_FEE
- `amount` - Transaction amount
- `balance_before, balance_after` - Balance snapshot

## Testing

### Create Sample Orders

```bash
# Login first to get token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"demo","password":"password"}'

# Place a buy order
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tradingPair": "BTC/USDT",
    "type": "LIMIT",
    "side": "BUY",
    "quantity": 0.5,
    "price": 40000
  }'
```

## Key Features Implemented

✅ User Authentication (JWT-based)
✅ Secure Password Hashing (BCrypt)
✅ Order Management (Create, Cancel, View)
✅ Order Matching Engine
✅ Wallet & Balance Management
✅ Trade Execution & Recording
✅ Transaction History
✅ Real-time WebSocket Updates
✅ Order Book Snapshots
✅ Portfolio Tracking
✅ CORS Support
✅ Exception Handling
✅ Input Validation
✅ Database Indexing
✅ Production-ready Structure

## Production Checklist

- [ ] Enable HTTPS/TLS
- [ ] Rotate JWT secret
- [ ] Set strong database password
- [ ] Configure proper CORS origins
- [ ] Enable database backups
- [ ] Set up monitoring and logging
- [ ] Configure rate limiting
- [ ] Add API documentation (Swagger/OpenAPI)
- [ ] Implement transaction replays
- [ ] Add audit logging
- [ ] Configure connection pooling
- [ ] Performance tuning

## Common Issues

### Database Connection Failed
- Verify MySQL is running
- Check credentials in application.properties
- Create database: `CREATE DATABASE ttcrypto_db;`

### JWT Token Invalid
- Token may have expired (default 24 hours)
- Verify JWT secret is consistent
- Check Authorization header format: `Bearer TOKEN`

### WebSocket Connection Failed
- Ensure WebSocket URL is correct
- Verify CORS origins are configured
- Check browser console for errors

## License

Proprietary - TT Crypto Trading Platform

## Support

For issues and questions, please contact the development team.
