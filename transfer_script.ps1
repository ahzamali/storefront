# Transfer Script

$targetIp = "192.168.211.175"
$imageName = "storefront-v2:latest"
$fileName = "storefront-v2.tar"

Write-Host "1. Saving Docker Image..." -ForegroundColor Cyan
docker save -o $fileName $imageName
if ($LASTEXITCODE -ne 0) { Write-Error "Failed to save docker image"; exit 1 }

Write-Host "2. Compressing Image (Optional but recommended)..." -ForegroundColor Cyan
# Using tar if available for gzip, else just keep .tar
# For simplicity in simplified windows environment, just keeping .tar or expecting 7z
# We will just stick to .tar for compatibility

Write-Host "3. Transferring to $targetIp..." -ForegroundColor Cyan
Write-Host "Run the following command to copy via SCP (if SSH is enabled on target):" -ForegroundColor Yellow
Write-Host "scp $fileName user@$targetIp:/path/to/destination"

Write-Host "4. Load on Target:" -ForegroundColor Cyan
Write-Host "ssh user@$targetIp 'docker load -i /path/to/destination/$fileName'"
Write-Host "ssh user@$targetIp 'docker run -d -p 8080:8080 --name storefront $imageName'"
