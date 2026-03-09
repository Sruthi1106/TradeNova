# Aiven Cloud Database Setup Guide

This guide explains how to set up and deploy the TT Crypto Trading Platform on Aiven Cloud MySQL.

## Prerequisites

- Aiven account with MySQL 8.0 instance created
- MySQL CLI installed locally (or use Aiven's web console)
- Database credentials provided by Aiven
- Spring Petals project files

## Your Aiven Connection Details

```
Host:     <AIVEN_HOST>
Port:     <AIVEN_PORT>
Username: avnadmin
Password: <AIVEN_PASSWORD>
Database: defaultdb
SSL:      Required
```

## Step 1: Verify Connection

### Using MySQL CLI

```bash
# Test connection to Aiven MySQL
mysql -h <AIVEN_HOST> \
      -P <AIVEN_PORT> \
      -u avnadmin \
      -p <AIVEN_PASSWORD> \
      -e "SELECT VERSION();"
```

### Using Aiven Console

1. Log in to https://console.aiven.io
2. Navigate to your MySQL service
3. Click "Query editor" (if available in your plan)
4. Test with: `SELECT VERSION();`

## Step 2: Initialize Database Schema

### Method 1: Using MySQL CLI

```bash
# Option A: Execute schema file directly
mysql -h <AIVEN_HOST> \
      -P <AIVEN_PORT> \
      -u avnadmin \
      -p"<AIVEN_PASSWORD>" \
      defaultdb < database/aiven_schema.sql

# Option B: Use source command (if in MySQL CLI)
# mysql> source database/aiven_schema.sql;
```

### Method 2: Using Aiven Console Web UI

1. Open Aiven Console â†’ Your MySQL Service â†’ Query editor
2. Copy and paste contents of `database/aiven_schema.sql`
3. Execute the SQL statements

### Method 3: Using MySQL Workbench

1. Open MySQL Workbench
2. Create new MySQL connection:
   - Hostname: `<AIVEN_HOST>`
   - Port: `<AIVEN_PORT>`
   - Username: `avnadmin`
   - Password: `<AIVEN_PASSWORD>`
3. Open and execute `database/aiven_schema.sql`

## Step 3: Verify Schema Creation

After running the schema script, verify all tables were created:

```bash
mysql -h <AIVEN_HOST> \
      -P <AIVEN_PORT> \
      -u avnadmin \
      -p"<AIVEN_PASSWORD>" \
      defaultdb -e "SHOW TABLES;"
```

Expected output:
```
+------------------------+
| Tables_in_defaultdb    |
+------------------------+
| orders                 |
| trades                 |
| transactions           |
| users                  |
| wallets                |
+------------------------+
```

## Step 4: Verify Sample Data

```bash
mysql -h <AIVEN_HOST> \
      -P <AIVEN_PORT> \
      -u avnadmin \
      -p"<AIVEN_PASSWORD>" \
      defaultdb -e "SELECT COUNT(*) as user_count FROM users;"

# Expected: 3 sample users
```

## Step 5: Run Spring Boot Application

### Option 1: Local Development (Using dev profile with local MySQL)

```bash
cd backend

# Ensure local MySQL is running with 'ttcrypto_db' database
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Option 2: Production (Using Aiven Cloud)

```bash
cd backend

# Connect to Aiven Cloud
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

### Option 3: Using Environment Variable

```bash
# Linux/Mac
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run

# Windows (PowerShell)
$env:SPRING_PROFILES_ACTIVE = "prod"
mvn spring-boot:run
```

### Option 4: Using Java Command (Production JAR)

```bash
# Build the application
mvn clean package

# Run with production profile
java -Dspring.profiles.active=prod \
     -jar target/ttcrypto-backend-1.0.0.jar
```

## Step 6: Run React Frontend

```bash
cd frontend

# Install dependencies
npm install

# Create .env file
cat > .env << EOF
VITE_API_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws/trading
EOF

# Start development server
npm run dev
```

Access at: http://localhost:5173

## Step 7: Test Trading Platform

### Demo Credentials

```
User 1:
  Email: demo@example.com
  Password: password
  
User 2:
  Email: admin@example.com
  Password: password
  
User 3:
  Email: trader@example.com
  Password: password
```

### Test API Endpoints

```bash
# 1. Register new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "username": "newuser",
    "password": "secure_password",
    "firstName": "New",
    "lastName": "User"
  }'

# 2. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "password"
  }'

# 3. Get portfolio
curl -X GET http://localhost:8080/api/v1/portfolio \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 4. Get market data
curl -X GET "http://localhost:8080/api/v1/market/prices?pairs=BTC/USDT,ETH/USDT"
```

## Troubleshooting

### Connection Error: "Can't connect to MySQL server"

- Verify Aiven service is running: https://console.aiven.io
- Check firewall allows outbound connection on port <AIVEN_PORT>
- Verify credentials are correct
- Ensure SSL is enabled in connection string

### SSL/TLS Connection Error

- Connection string in `application-prod.properties` already has SSL enabled
- Ensure: `useSSL=true&allowPublicKeyRetrieval=false`
- If using older MySQL drivers, add: `requireSSL=true`

### Authentication Error (403 / 401)

- Verify JWT token is being sent in `Authorization: Bearer <token>` header
- Check token hasn't expired (24-hour expiration)
- Ensure user exists in database

### No Data in Database

- Verify schema initialization completed successfully
- Check SHOW TABLES returns 5 tables
- Run sample data insert queries manually if needed

### WebSocket Connection Issues

- Frontend .env must have correct VITE_WS_URL
- Verify WebSocket is enabled in SecurityConfig
- Check browser console for connection errors

## Production Deployment

### Using Docker

```bash
# Build Docker image
docker build -t ttcrypto-backend:latest -f Dockerfile .

# Run with Aiven configuration
docker run -e SPRING_PROFILES_ACTIVE=prod \
           -p 8080:8080 \
           ttcrypto-backend:latest
```

### Using Docker Compose

```bash
# Ensure application-prod.properties is configured with Aiven details
docker-compose up
```

### Kubernetes Deployment

Create a `deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ttcrypto-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ttcrypto-backend
  template:
    metadata:
      labels:
        app: ttcrypto-backend
    spec:
      containers:
      - name: backend
        image: ttcrypto-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: aiven-mysql-secret
              key: url
        resources:
          limits:
            memory: "512Mi"
            cpu: "500m"
          requests:
            memory: "256Mi"
            cpu: "250m"
```

## Monitoring

### Enable Application Insights

Add to `application-prod.properties`:

```properties
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG
```

### Monitor Aiven Database

1. Log in to https://console.aiven.io
2. Navigate to your MySQL service
3. View metrics:
   - CPU usage
   - Disk usage
   - Connection count
   - Query performance

### Application Logs

```bash
# View logs from running container
docker logs ttcrypto-backend

# Or from Kubernetes
kubectl logs -f deployment/ttcrypto-backend
```

## Maintenance

### Backup Database (Aiven)

Aiven provides automatic backups. To restore:

1. Console â†’ Your MySQL Service â†’ Backups
2. Select backup point and restore

### Clear Test Data

```bash
mysql -h <AIVEN_HOST> \
      -P <AIVEN_PORT> \
      -u avnadmin \
      -p"<AIVEN_PASSWORD>" \
      defaultdb << EOF
DELETE FROM transactions;
DELETE FROM trades;
DELETE FROM orders;
DELETE FROM wallets;
DELETE FROM users;
EOF
```

Then re-run the schema script to reinitialize sample data.

## Performance Tuning

### Connection Pool Settings (in application-prod.properties)

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Query Optimization

Current database has indexes on:
- `users.email`, `users.username`
- `wallets.user_id`
- `orders.user_id`, `orders.trading_pair`, `orders.status`
- `trades.trading_pair`, `trades.buyer_id`, `trades.seller_id`
- `transactions.user_id`, `transactions.transaction_type`

### Batch Processing

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

## Security Checklist

- [x] SSL/TLS enabled for database connection
- [x] Password secured (never commit to repository)
- [x] JWT token expiration set to 24 hours
- [x] CORS configured for allowed origins
- [x] Spring Security enabled with authentication
- [x] Error messages sanitized in production
- [x] Logging configured with no sensitive data

## Support

For Aiven support: https://aiven.io/support

## Next Steps

1. Initialize database schema using provided SQL script
2. Configure Spring Boot profiles based on environment (dev vs prod)
3. Deploy backend to production environment
4. Deploy frontend to CDN or static hosting
5. Set up monitoring and alerting
6. Configure CI/CD pipeline for automated deployments

