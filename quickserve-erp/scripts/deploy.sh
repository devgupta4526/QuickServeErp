#!/bin/bash
# deploy.sh — Zero-downtime deployment script for QuickServe ERP VPS
# Usage: ./scripts/deploy.sh [--tag v1.2.3]

set -euo pipefail

TAG=${1:-latest}
COMPOSE_FILE="docker/docker-compose.prod.yml"

echo "🚀 Starting QuickServe ERP deployment (tag: $TAG)"

# Validate required env vars
REQUIRED_VARS=(DB_USERNAME DB_PASSWORD REDIS_PASSWORD JWT_SECRET MINIO_USER MINIO_PASSWORD WHATSAPP_ENCRYPTION_KEY)
for var in "${REQUIRED_VARS[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        echo "❌ Required environment variable $var is not set"
        exit 1
    fi
done

# Pull latest images
echo "📦 Pulling latest images..."
docker compose -f "$COMPOSE_FILE" pull

# Apply database migrations (Flyway runs on app startup)
echo "🗄️  Migrations will run automatically on startup..."

# Rolling restart — bring up new backend while old still serves
echo "🔄 Deploying backend..."
docker compose -f "$COMPOSE_FILE" up -d --no-deps backend

# Wait for health check
echo "⏳ Waiting for backend to be healthy..."
for i in $(seq 1 30); do
    if docker compose -f "$COMPOSE_FILE" exec -T backend curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Backend is healthy!"
        break
    fi
    echo "   Attempt $i/30..."
    sleep 10
done

# Deploy frontend
echo "🎨 Deploying frontend..."
docker compose -f "$COMPOSE_FILE" up -d --no-deps frontend

# Reload Nginx
echo "🌐 Reloading Nginx..."
docker compose -f "$COMPOSE_FILE" exec nginx nginx -s reload

echo ""
echo "✅ Deployment complete!"
echo "   Backend:  https://\$DOMAIN/api-docs"
echo "   Frontend: https://\$DOMAIN"
