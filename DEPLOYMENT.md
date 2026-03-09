# Production Deployment Guide - TT Crypto Trading Platform

## Overview

This guide covers deploying the TT Crypto Trading Platform to production using:
- **Backend**: Java Spring Boot on Aiven Cloud or Kubernetes
- **Database**: Aiven Cloud MySQL (managed service)
- **Frontend**: Static hosting (Netlify, Vercel, AWS S3) or containerized nginx

## Prerequisites

- Docker & Docker Compose installed
- Maven 3.9+ and Java 17+ (for local builds)
- Node.js 18+ (for frontend builds)
- Aiven account with MySQL service provisioned
- Git for version control
- Domain name and SSL certificate (production)

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Internet Users                    │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│         CDN / Static Hosting (Frontend)              │
│         (Netlify, Vercel, AWS CloudFront)           │
│         - React SPA                                  │
│         - Tailwind CSS                              │
│         - WebSocket to Backend                      │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│              API Load Balancer                       │
│         (Nginx, AWS ALB, or Kubernetes Ingress)     │
│         - HTTPS/WSS termination                     │
│         - Rate limiting                             │
│         - Request routing                           │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│           Backend Services (3+ replicas)             │
│         (Docker / Kubernetes / App Platform)        │
│         - Spring Boot 3.2.0                         │
│         - JWT Authentication                        │
│         - Order Matching Engine                     │
│         - WebSocket Server                          │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│           Aiven Cloud MySQL Database                 │
│         - Managed database-as-a-service             │
│         - Automatic backups                         │
│         - SSL/TLS encryption                        │
│         - Connection pooling                        │
└─────────────────────────────────────────────────────┘
```

## Step 1: Prepare for Production

### 1.1 Update Configuration Files

```bash
# Copy production environment files
cp .env.prod.example .env.prod
cp backend/src/main/resources/application-prod.properties.example \
   backend/src/main/resources/application-prod.properties
```

### 1.2 Update Credentials

Edit `.env.prod` with your actual values:

```bash
# Aiven Cloud MySQL
SPRING_DATASOURCE_URL=jdbc:mysql://your-aiven-host:port/defaultdb?useSSL=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_secure_password

# JWT Secret (generate a strong random key)
JWT_SECRET=$(openssl rand -base64 32)

# Frontend URLs
VITE_API_URL=https://api.yourdomain.com/api/v1
VITE_WS_URL=wss://api.yourdomain.com/ws/trading

# CORS Origins
SPRING_WEB_CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

### 1.3 Generate JWT Secret

```bash
# Generate a secure random JWT secret
openssl rand -base64 32

# Output example (use this in your .env.prod):
# eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Step 2: Initialize Aiven Database

### 2.1 Connect to Aiven MySQL

```bash
# Verify connection
mysql -h your-aiven-host \
      -P your-port \
      -u username \
      -p"password" \
      -e "SELECT VERSION();"
```

### 2.2 Initialize Schema

```bash
# Execute schema initialization
mysql -h your-aiven-host \
      -P your-port \
      -u username \
      -p"password" \
      defaultdb < database/aiven_schema.sql

# Verify tables were created
mysql -h your-aiven-host \
      -P your-port \
      -u username \
      -p"password" \
      defaultdb -e "SHOW TABLES;"
```

## Step 3: Build Docker Images

### 3.1 Build Backend Image

```bash
# Build for production
docker build -f Dockerfile.prod -t ttcrypto-backend:latest .

# Tag for registry (e.g., Docker Hub)
docker tag ttcrypto-backend:latest yourusername/ttcrypto-backend:latest
docker tag ttcrypto-backend:latest yourusername/ttcrypto-backend:v1.0.0

# Push to registry
docker push yourusername/ttcrypto-backend:latest
docker push yourusername/ttcrypto-backend:v1.0.0
```

### 3.2 Build Frontend Image

```bash
# Create Dockerfile for frontend
cat > frontend/Dockerfile << 'EOF'
# Build stage
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
EOF

# Build image
docker build -t ttcrypto-frontend:latest ./frontend

# Push to registry
docker tag ttcrypto-frontend:latest yourusername/ttcrypto-frontend:latest
docker push yourusername/ttcrypto-frontend:latest
```

### 3.3 Create nginx.conf for Frontend

```bash
cat > frontend/nginx.conf << 'EOF'
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css text/javascript application/javascript;
    gzip_min_length 1000;

    # Cache busting for versioned assets
    location ~* ^/assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # API proxy (alternative to CORS)
    location /api/v1/ {
        proxy_pass http://backend:8080/api/v1/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket proxy
    location /ws/ {
        proxy_pass http://backend:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    # SPA fallback - serve index.html for all routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
}
EOF
```

## Step 4: Deploy with Docker Compose

### 4.1 Production Deployment

```bash
# Create production compose file
cp docker-compose.prod.yml docker-compose.override.yml

# Set environment variables
export $(cat .env.prod | grep -v '^#' | xargs)

# Deploy
docker-compose -f docker-compose.prod.yml up -d

# Verify services
docker-compose ps
docker-compose logs -f backend
```

### 4.2 Scale Services

```bash
# Scale backend to 3 replicas (requires load balancer)
docker-compose -f docker-compose.prod.yml up -d --scale backend=3

# Note: Docker Compose scaling works best with load balancer (nginx, traefik)
```

## Step 5: Deploy to Kubernetes

### 5.1 Create Kubernetes Manifests

**backend-deployment.yaml**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ttcrypto-backend
  namespace: production
spec:
  replicas: 3
  revisionHistoryLimit: 3
  selector:
    matchLabels:
      app: ttcrypto-backend
  template:
    metadata:
      labels:
        app: ttcrypto-backend
        version: v1
    spec:
      containers:
      - name: backend
        image: yourusername/ttcrypto-backend:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: aiven-mysql
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: aiven-mysql
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: aiven-mysql
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: ttcrypto-backend
  namespace: production
spec:
  type: ClusterIP
  selector:
    app: ttcrypto-backend
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
```

**ingress.yaml**:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ttcrypto-ingress
  namespace: production
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.yourdomain.com
    secretName: ttcrypto-tls
  rules:
  - host: api.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ttcrypto-backend
            port:
              number: 80
```

### 5.2 Create Secrets

```bash
# Create namespace
kubectl create namespace production

# Create Aiven database secret
kubectl create secret generic aiven-mysql \
  --from-literal=url='jdbc:mysql://...' \
  --from-literal=username='avnadmin' \
  --from-literal=password='...' \
  -n production

# Create JWT secret
kubectl create secret generic jwt-secret \
  --from-literal=secret='your-generated-secret' \
  -n production
```

### 5.3 Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace production

# Apply configurations
kubectl apply -f backend-deployment.yaml
kubectl apply -f ingress.yaml

# Verify deployment
kubectl get pods -n production
kubectl describe deployment ttcrypto-backend -n production

# View logs
kubectl logs -f deployment/ttcrypto-backend -n production --tail=50
```

## Step 6: Set Up HTTPS

### 6.1 Using Let's Encrypt (Free)

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Create ClusterIssuer for Let's Encrypt
cat > letsencrypt-issuer.yaml << 'EOF'
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@yourdomain.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF

kubectl apply -f letsencrypt-issuer.yaml
```

### 6.2 Using AWS Certificate Manager

```bash
# Request certificate in AWS ACM
# Then reference in ELB/ALB configuration
# Update Ingress annotations:
# alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:region:account:certificate/id
```

## Step 7: Monitoring & Logging

### 7.1 Enable Health Checks

```properties
# In application-prod.properties
management.endpoints.web.exposure.include=health,metrics,info,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true
```

### 7.2 Set Up Monitoring Stack

```bash
# Deploy Prometheus and Grafana
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring --create-namespace

# Access Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80
```

### 7.3 Configure Logging

```bash
# Deploy ELK Stack for centralized logging
helm repo add elastic https://helm.elastic.co
helm install elasticsearch elastic/elasticsearch -n elastic-system --create-namespace
```

## Step 8: Backup & Recovery

### 8.1 Aiven Database Backups

```bash
# Aiven provides automatic backups
# Access via console: https://console.aiven.io
# - Automatic daily backups (7 days retention)
# - Restore from console (1-click)
# - Point-in-time recovery available
```

### 8.2 Manual Backup

```bash
# Create backup
mysqldump -h your-aiven-host \
          -P your-port \
          -u username \
          -p"password" \
          defaultdb > backup-$(date +%Y%m%d-%H%M%S).sql

# Restore from backup
mysql -h your-aiven-host \
      -P your-port \
      -u username \
      -p"password" \
      defaultdb < backup-backup.sql
```

### 8.3 Kubernetes Persistent Volumes (if using local DB)

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
  namespace: production
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
  storageClassName: fast-ssd
```

## Step 9: Performance Optimization

### 9.1 Database Connection Pooling

```properties
# In application-prod.properties
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### 9.2 Caching Layer

```bash
# Deploy Redis for caching
docker run -d --name redis \
  -p 6379:6379 \
  redis:7-alpine
```

```properties
# Enable Spring Cache
spring.cache.type=redis
spring.redis.host=redis
spring.redis.port=6379
```

### 9.3 CDN for Frontend Assets

```bash
# CloudFront (AWS)
# - Distribution: yourdomain.com → S3/Nginx
# - Cache 1 year for versioned assets (/assets/*)
# - Cache 1 minute for HTML
```

## Step 10: Security Hardening

### 10.1 Web Application Firewall

```bash
# Enable WAF on CloudFront/ALB
# Rules for:
# - Rate limiting
# - SQL injection protection
# - XSS protection
# - DDoS protection
```

### 10.2 Environment Security

```bash
# Secrets management
# - AWS Secrets Manager
# - HashiCorp Vault
# - Kubernetes Secrets

# Never commit secrets to Git
# Use .gitignore:
.env
.env.prod
*.key
*.pem
```

### 10.3 API Rate Limiting

```java
// In SecurityConfig.java
http.addFilterBefore(new RateLimitFilter(), AuthorizationFilter.class);
```

## Troubleshooting

### Backend Won't Start

```bash
# Check logs
docker logs ttcrypto-backend

# Verify database connection
curl -i http://localhost:8080/actuator/health

# Check environment variables
docker ps | grep ttcrypto
docker inspect ttcrypto-backend | grep -i env
```

### WebSocket Connection Failed

```bash
# Verify CORS settings
# Check browser console for errors
# Ensure WSS is enabled for HTTPS

# In browser console:
# Check: Connection headers
# Check: Origin matches CORS_ALLOWED_ORIGINS
```

### Database Connection Timeout

```bash
# Verify Aiven service is running
# Check firewall rules allow port 12969
# Verify credentials in .env.prod
# Test connection: mysql -h host -u user -p"password"
```

### Out of Memory

```bash
# Increase JVM heap
export JVM_OPTS="-Xmx2g -Xms1g"

# Or in deployment:
env:
  - name: JVM_OPTS
    value: "-Xmx2g -Xms1g"
```

## Monitoring Checklist

- [ ] Application health endpoint responding
- [ ] Database connections within pool limits
- [ ] Error rate < 1%
- [ ] Response time < 500ms (p95)
- [ ] CPU usage < 60%
- [ ] Memory usage < 70%
- [ ] Disk space > 20% available
- [ ] SSL certificate valid (>30 days)
- [ ] Backups completing successfully
- [ ] No authentication failures anomalies

## Rollback Procedure

```bash
# If issues occur after deployment

# Kubernetes rollback
kubectl rollout undo deployment/ttcrypto-backend -n production

# Docker Compose rollback
docker-compose down
docker tag yourusername/ttcrypto-backend:v1.0.0 yourusername/ttcrypto-backend:latest
docker-compose -f docker-compose.prod.yml up -d
```

## Post-Deployment Verification

```bash
# 1. Test API endpoints
curl -X POST http://api.yourdomain.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"password"}'

# 2. Verify WebSocket
# Open browser → https://yourdomain.com → Open DevTools
# Navigate to Network tab → WS
# Should see connection to ws://api.yourdomain.com/ws/trading

# 3. Test trading functionality
# Login with demo account
# Create and cancel a test order
# Verify order appears in database

# 4. Monitor logs
kubectl logs -f deployment/ttcrypto-backend -n production
```

## Production Checklist

- [ ] Database schema initialized on Aiven
- [ ] Environment configuration files created (.env.prod)
- [ ] SSL/TLS certificate installed
- [ ] CORS origins configured correctly
- [ ] JWT secret generated and stored securely
- [ ] Docker images built and pushed to registry
- [ ] Kubernetes manifests created and applied
- [ ] Health checks configured and passing
- [ ] Monitoring and logging enabled
- [ ] Backup procedures tested
- [ ] Rollback procedure documented
- [ ] Security headers enabled
- [ ] Rate limiting configured
- [ ] CDN setup for frontend
- [ ] Database backups scheduled

## Support Resources

- **Aiven Support**: https://aiven.io/support
- **Kubernetes Docs**: https://kubernetes.io/docs
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Docker Docs**: https://docs.docker.com

---

**Last Updated**: 2024  
**Maintained By**: Development Team
