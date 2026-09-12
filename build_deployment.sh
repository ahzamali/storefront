#!/bin/bash
set -e

echo -e "\033[0;36m===================================================\033[0m"
echo -e "\033[0;36m      StoreFront v2 - Build & Docker Deploy        \033[0m"
echo -e "\033[0;36m===================================================\033[0m"

# Ensure script is run from project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Build Frontend
echo -e "\n\033[1;33m[1/5] Building Frontend (React/Vite)...\033[0m"
cd web
npm install
npm run build
cd "$SCRIPT_DIR"

# 2. Copy Frontend to Backend static resources
echo -e "\n\033[1;33m[2/5] Copying Frontend assets to Backend static resources...\033[0m"
STATIC_DIR="server/src/main/resources/static"
mkdir -p "$STATIC_DIR"
rm -rf "$STATIC_DIR"/*
cp -r web/dist/* "$STATIC_DIR"/

# 3. Build Backend JAR
echo -e "\n\033[1;33m[3/5] Building Backend (Spring Boot JAR)...\033[0m"
cd server
mvn clean package -DskipTests
cd "$SCRIPT_DIR"

# 4. Build Docker Image
IMAGE_NAME="ghcr.io/ahzamali/storefront-v2:latest"
echo -e "\n\033[1;33m[4/5] Building Docker Image ($IMAGE_NAME)...\033[0m"
docker build -t "$IMAGE_NAME" -t "storefront:latest" .

# 5. Update and Deploy Docker Container
echo -e "\n\033[1;33m[5/5] Recreating and updating Docker containers...\033[0m"
mkdir -p data

if command -v docker-compose &> /dev/null; then
    docker-compose down || true
    docker-compose up -d
elif docker compose version &> /dev/null; then
    docker compose down || true
    docker compose up -d
else
    echo -e "\033[0;31mDocker Compose not found. Running with standard docker run...\033[0m"
    docker stop storefront-app || true
    docker rm storefront-app || true
    docker run -d \
        --name storefront-app \
        -p 8080:8080 \
        -v "$PWD/data:/app/data" \
        --restart unless-stopped \
        "$IMAGE_NAME"
fi

echo -e "\n\033[0;32m===================================================\033[0m"
echo -e "\033[0;32m   Deployment Successful! App is running.          \033[0m"
echo -e "\033[0;32m   URL: http://localhost:8080                      \033[0m"
echo -e "\033[0;32m===================================================\033[0m"
