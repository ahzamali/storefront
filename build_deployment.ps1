# Build Deployment Bundle

Write-Host "1. Building Frontend..." -ForegroundColor Cyan
Set-Location "web"
npm run build
if ($LASTEXITCODE -ne 0) { Write-Error "Frontend build failed"; exit 1 }
Set-Location ".."

Write-Host "2. Copying Frontend to Backend..." -ForegroundColor Cyan
$staticDir = "server/src/main/resources/static"
if (!(Test-Path $staticDir)) { New-Item -ItemType Directory -Force -Path $staticDir }
# Clear old files
Remove-Item "$staticDir/*" -Recurse -Force -ErrorAction SilentlyContinue
# Copy new build (Assuming vite builds to 'dist')
Copy-Item "web/dist/*" -Destination $staticDir -Recurse -Force

Write-Host "3. Building Backend..." -ForegroundColor Cyan
Set-Location "server"
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Error "Backend build failed"; exit 1 }
Set-Location ".."

Write-Host "4. Building Docker Image..." -ForegroundColor Cyan
$imageName = "ghcr.io/ahzamali/storefront-v2:latest"
docker build -t $imageName .
if ($LASTEXITCODE -ne 0) { Write-Error "Docker build failed"; exit 1 }

Write-Host "5. Pushing to GitHub Container Registry..." -ForegroundColor Cyan
Write-Host "Ensuring you are logged in... (run 'docker login ghcr.io' if this fails)" -ForegroundColor Yellow
docker push $imageName
if ($LASTEXITCODE -ne 0) { Write-Error "Docker push failed. Are you logged in to ghcr.io?"; exit 1 }

Write-Host "Build and Push Complete! Image '$imageName' is published." -ForegroundColor Green
