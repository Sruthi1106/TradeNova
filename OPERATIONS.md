# TT Crypto Platform - Operations Playbook

This playbook provides step-by-step instructions for common operational tasks.

## Table of Contents
1. [Starting Services](#starting-services)
2. [Monitoring](#monitoring)
3. [Troubleshooting](#troubleshooting)
4. [Database Operations](#database-operations)
5. [Deployment](#deployment)
6. [Incident Response](#incident-response)
7. [Performance Tuning](#performance-tuning)

## Starting Services

### Development Environment

```bash
# Start all services with Docker Compose
cd /path/to/ttcrypto
docker-compose -f docker-compose.dev.yml up -d

# Verify services are running
docker-compose ps

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql

# Stop services
docker-compose down
```

### Production Environment

```bash
# Load environment variables
export $(cat .env.prod | grep -v '^#' | xargs)

# Start services
docker-compose -f docker-compose.prod.yml up -d

# Scale backend (requires load balancer)
docker-compose -f docker-compose.prod.yml up -d --scale backend=3

# Verify deployment
docker-compose ps
docker-compose logs backend
```

### Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace production

# Create secrets
kubectl create secret generic aiven-mysql \
  --from-literal=url='jdbc:mysql://...' \
  -n production

# Deploy application
kubectl apply -f backend-deployment.yaml
kubectl apply -f ingress.yaml

# Verify deployment
kubectl get pods -n production
kubectl get services -n production
kubectl get ingress -n production
```

## Monitoring

### Health Checks

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Full health details (requires auth)
curl http://localhost:8080/actuator/health/details

# Check metrics
curl http://localhost:8080/actuator/metrics

# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.usage
curl http://localhost:8080/actuator/metrics/jvm.threads.live
```

### Kubernetes Monitoring

```bash
# Pod health status
kubectl get pods -n production -o wide

# Pod logs
kubectl logs -f deployment/ttcrypto-backend -n production --tail=100

# Resource usage
kubectl top nodes
kubectl top pods -n production

# Events
kubectl describe pod <pod-name> -n production
```

### Docker Container Monitoring

```bash
# View container status
docker ps
docker stats

# Container logs
docker logs -f ttcrypto-backend-local

# Resource limits
docker inspect ttcrypto-backend-local | grep -i memory
```

### Database Monitoring

```bash
# Connection count
mysql -h localhost -u ttcrypto -p ttcrypto_db -e "SHOW PROCESSLIST;"

# Current queries
mysql -h localhost -u ttcrypto -p ttcrypto_db -e "SHOW ENGINE INNODB STATUS\G"

# Table sizes
mysql -h localhost -u ttcrypto -p ttcrypto_db -e "
  SELECT table_name, ROUND(((data_length + index_length) / 1024 / 1024), 2) as size_mb
  FROM information_schema.TABLES
  WHERE table_schema = 'ttcrypto_db'
  ORDER BY size_mb DESC;"

# Slow queries (if enabled)
mysql -h localhost -u ttcrypto -p ttcrypto_db -e "SELECT * FROM mysql.slow_log LIMIT 10;"
```

### Application Metrics

```bash
# View all available metrics
curl http://localhost:8080/actuator/metrics | jq

# Specific metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus format (for Prometheus scraping)
curl http://localhost:8080/actuator/prometheus
```

## Troubleshooting

### Service Won't Start

**Backend (Spring Boot)**:
```bash
# Check logs
docker logs ttcrypto-backend-local

# Verify database connectivity
docker exec ttcrypto-backend-local bash -c "nc -zv mysql 3306"

# Check port availability
lsof -i :8080

# Test configuration
mvn spring-boot:run -Dspring-boot.run.arguments="--debug"
```

**Frontend (React)**:
```bash
# Check logs
docker logs ttcrypto-frontend-local

# Verify port availability
lsof -i :5173

# Clear npm cache and reinstall
npm cache clean --force
npm install
```

**MySQL**:
```bash
# Check MySQL logs
docker logs ttcrypto-mysql-local

# Verify database accessibility
mysql -h localhost -u ttcrypto -p ttcrypto_db -e "SELECT VERSION();"

# Check disk space
docker exec ttcrypto-mysql-local df -h
```

### Performance Issues

**High Memory Usage**:
```bash
# Check JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.usage | jq

# Increase heap size
export JVM_OPTS="-Xmx2g -Xms1g"
docker-compose -f docker-compose.dev.yml down
docker-compose -f docker-compose.dev.yml up -d

# Check garbage collection
curl http://localhost:8080/actuator/metrics/jvm.gc.pause | jq
```

**Slow Queries**:
```bash
# Enable slow query log
mysql -h localhost -u root -p -e "SET GLOBAL slow_query_log = 'ON';"
mysql -h localhost -u root -p -e "SET GLOBAL long_query_time = 2;"

# Check slow queries
mysql -h localhost -u root -p -e "SELECT * FROM mysql.slow_log LIMIT 10;"

# Analyze query performance
EXPLAIN SELECT * FROM orders WHERE user_id = 1 AND status = 'PENDING';
```

**Connection Pool Exhaustion**:
```bash
# Check active connections
mysql -h localhost -u root -p -e "SHOW PROCESSLIST;"

# Kill idle connections
mysql -h localhost -u root -p -e "KILL CONNECTION <ID>;"

# Increase pool size in application-prod.properties
spring.datasource.hikari.maximum-pool-size=50
```

### Network/Connectivity Issues

**DNS Resolution**:
```bash
# Test DNS (for Aiven from production)
nslookup <AIVEN_HOST>
dig <AIVEN_HOST>
```

**SSL/TLS Issues**:
```bash
# Test SSL connection to Aiven
openssl s_client -connect <AIVEN_HOST>:<AIVEN_PORT>

# Check certificate validity
echo | openssl s_client -connect <AIVEN_HOST>:<AIVEN_PORT> 2>/dev/null | openssl x509 -noout -dates
```

**Firewall**:
```bash
# Test connectivity
telnet <AIVEN_HOST> <AIVEN_PORT>

# Check iptables rules
sudo iptables -L -n

# Allow port (if needed)
sudo iptables -A INPUT -i eth0 -p tcp --dport 8080 -j ACCEPT
```

### Authentication Issues

**JWT Token Expired**:
```bash
# Test token expiration
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"password"}'

# Decode token (online: jwt.io)
# Check 'exp' field for expiration time
```

**CORS Issues**:
```bash
# Check CORS headers
curl -i -X OPTIONS http://localhost:8080/api/v1/market/prices \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET"

# Should return: Access-Control-Allow-Origin: http://localhost:5173
```

## Database Operations

### Backup & Restore

**Manual Backup**:
```bash
# Full backup
mysqldump -h localhost -u root -p ttcrypto_db > backup-$(date +%Y%m%d).sql

# Backup with gzip compression
mysqldump -h localhost -u root -p ttcrypto_db | gzip > backup-$(date +%Y%m%d).sql.gz

# Aiven backup
mysqldump -h <AIVEN_HOST> -P <AIVEN_PORT> \
          -u avnadmin -p < database/aiven_schema.sql
```

**Restore from Backup**:
```bash
# Simple restore
mysql -h localhost -u root -p ttcrypto_db < backup-20240101.sql

# Restore with progress
pv backup-20240101.sql | mysql -h localhost -u root -p ttcrypto_db

# Restore compressed
gunzip -c backup-20240101.sql.gz | mysql -h localhost -u root -p ttcrypto_db
```

### Data Cleanup

**Clear Test Data**:
```bash
mysql -h localhost -u ttcrypto -p ttcrypto_db << EOF
DELETE FROM transactions;
DELETE FROM trades;
DELETE FROM orders;
DELETE FROM wallets;
DELETE FROM users;
EOF
```

**Reset Auto Increment**:
```bash
mysql -h localhost -u ttcrypto -p ttcrypto_db << EOF
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE trades AUTO_INCREMENT = 1;
EOF
```

### Database Optimization

**Analyze Tables**:
```bash
mysql -h localhost -u ttcrypto -p ttcrypto_db << EOF
ANALYZE TABLE users;
ANALYZE TABLE orders;
ANALYZE TABLE trades;
ANALYZE TABLE wallets;
ANALYZE TABLE transactions;
EOF
```

**Optimize Tables**:
```bash
mysql -h localhost -u ttcrypto -p ttcrypto_db << EOF
OPTIMIZE TABLE users;
OPTIMIZE TABLE orders;
OPTIMIZE TABLE trades;
OPTIMIZE TABLE wallets;
OPTIMIZE TABLE transactions;
EOF
```

**Check Table Integrity**:
```bash
mysql -h localhost -u ttcrypto -p ttcrypto_db << EOF
CHECK TABLE users;
CHECK TABLE orders;
REPAIR TABLE orders;
EOF
```

## Deployment

### Deploying a New Version

**1. Prepare Release**:
```bash
# Update version numbers
# backend/pom.xml: <version>1.1.0</version>
# frontend/package.json: "version": "1.1.0"

# Build backend
cd backend && mvn clean package -DskipTests

# Build frontend
cd frontend && npm run build
```

**2. Build Docker Images**:
```bash
# Backend
docker build -f Dockerfile.prod -t ttcrypto-backend:1.1.0 .
docker tag ttcrypto-backend:1.1.0 yourusername/ttcrypto-backend:1.1.0
docker push yourusername/ttcrypto-backend:1.1.0

# Frontend
docker build -t ttcrypto-frontend:1.1.0 ./frontend
docker tag ttcrypto-frontend:1.1.0 yourusername/ttcrypto-frontend:1.1.0
docker push yourusername/ttcrypto-frontend:1.1.0
```

**3. Deploy (Docker Compose)**:
```bash
# Update docker-compose.prod.yml with new image tags
image: yourusername/ttcrypto-backend:1.1.0

# Deploy
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d

# Verify
docker-compose ps
curl http://localhost:8080/actuator/health
```

**4. Deploy (Kubernetes)**:
```bash
# Update deployment image
kubectl set image deployment/ttcrypto-backend \
  backend=yourusername/ttcrypto-backend:1.1.0 \
  -n production

# Watch rollout
kubectl rollout status deployment/ttcrypto-backend -n production

# Verify pods are running
kubectl get pods -n production
```

### Canary Deployment

```bash
# Create canary deployment (5% traffic)
kubectl apply -f canary-deployment.yaml

# Monitor metrics
kubectl top pods -n production
curl http://localhost:8080/actuator/metrics

# Promote to main (if successful)
kubectl delete -f canary-deployment.yaml
kubectl set image deployment/ttcrypto-backend \
  backend=yourusername/ttcrypto-backend:1.1.0 \
  -n production
```

### Rollback

```bash
# Docker Compose
docker-compose -f docker-compose.prod.yml down
git checkout HEAD~1      # Go to previous version
docker-compose -f docker-compose.prod.yml up -d

# Kubernetes
kubectl rollout undo deployment/ttcrypto-backend -n production
kubectl rollout history deployment/ttcrypto-backend -n production
```

## Incident Response

### Website Down

1. **Assess Situation**:
   ```bash
   # Check all services
   docker-compose ps
   curl http://localhost:8080/actuator/health
   curl http://localhost:5173
   ```

2. **Restart Services**:
   ```bash
   # Restart failed service
   docker-compose restart backend
   docker-compose restart frontend
   
   # Or full restart
   docker-compose down
   docker-compose up -d
   ```

3. **Check Logs**:
   ```bash
   docker-compose logs backend | tail -100
   docker-compose logs frontend | tail -100
   docker-compose logs mysql | tail -100
   ```

4. **Escalate if Needed**:
   - Check Aiven dashboard for database issues
   - Check monitoring system
   - Contact on-call engineer

### Database Issues

1. **Connection Error**:
   ```bash
   # Test database
   mysql -h localhost -u ttcrypto -p ttcrypto_db -e "SELECT 1;"
   
   # Check process count
   mysql -h localhost -u ttcrypto -p -e "SHOW PROCESSLIST;" | wc -l
   ```

2. **Out of Disk Space**:
   ```bash
   # Check disk usage
   df -h /var/lib/mysql
   
   # Increase volume size (cloud)
   # Or clean old backups
   ```

3. **Slow Queries**:
   ```bash
   # Identify slow queries
   mysql -h localhost -u ttcrypto -p -e "SELECT * FROM mysql.slow_log LIMIT 10;"
   
   # Kill long-running query
   mysql -h localhost -u ttcrypto -p -e "SHOW PROCESSLIST \G"
   mysql -h localhost -u ttcrypto -p -e "KILL QUERY <ID>;"
   ```

### Memory Leak

1. **Monitor Memory**:
   ```bash
   # Check JVM memory over time
   for i in {1..10}; do
     curl -s http://localhost:8080/actuator/metrics/jvm.memory.usage/area:heap | jq '.measurements[0].value'
     sleep 30
   done
   ```

2. **Collect Heap Dump**:
   ```bash
   # Force garbage collection
   jcmd <PID> GC.run
   
   # Create heap dump
   jcmd <PID> GC.heap_dump /tmp/heapdump.hprof
   ```

3. **Restart Service**:
   ```bash
   docker-compose restart backend
   ```

### High CPU Usage

1. **Identify Process**:
   ```bash
   docker stats --no-stream
   ```

2. **Profile Application**:
   ```bash
   # CPU flame graph (requires profiling tools)
   # Or check logs for loops
   docker logs -f ttcrypto-backend-local | grep ERROR
   ```

3. **Scale Up**:
   ```bash
   # Increase replicas
   docker-compose -f docker-compose.prod.yml up -d --scale backend=5
   ```

## Performance Tuning

### Database Optimization

```bash
# Configure for production
mysql -h localhost -u root -p << EOF
SET GLOBAL max_connections = 1000;
SET GLOBAL wait_timeout = 600;
SET GLOBAL interactive_timeout = 600;
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;
EOF
```

### Application Tuning

```properties
# In application-prod.properties

# Connection pool
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10

# Batch processing
spring.jpa.properties.hibernate.jdbc.batch_size=30
spring.jpa.properties.hibernate.order_inserts=true

# Caching
spring.cache.type=redis
spring.redis.host=redis-server
spring.redis.port=6379

# JVM
JVM_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
```

### Frontend Optimization

```javascript
// vite.config.js
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'recharts': ['recharts'],
          'vendor': ['react', 'react-dom'],
        }
      }
    }
  }
}
```

### Load Testing

```bash
# Using Apache Bench
ab -n 10000 -c 100 http://localhost:8080/api/v1/market/prices

# Using wrk
wrk -t4 -c100 -d30s http://localhost:8080/api/v1/market/prices

# Using LoadRunner or JMeter
# Create test plans for critical endpoints
```

## Checklists

### Daily Checks
- [ ] All services running (`docker-compose ps`)
- [ ] Backend healthy (`curl health endpoint`)
- [ ] Database responding (`mysql -e "SELECT 1"`)
- [ ] No critical errors in logs
- [ ] Disk space available (>20%)

### Weekly Checks
- [ ] Database optimization completed (`OPTIMIZE TABLE`)
- [ ] Backups completed and tested
- [ ] Performance metrics reviewed
- [ ] SSL certificate expiration checked (>30 days)
- [ ] Security patches applied

### Monthly Checks
- [ ] Full backup taken and stored safely
- [ ] Disaster recovery drills completed
- [ ] Capacity planning reviewed
- [ ] Performance trends analyzed
- [ ] Security audit completed

---

**For more information**: See [DEPLOYMENT.md](DEPLOYMENT.md) and [AIVEN_SETUP.md](AIVEN_SETUP.md)

