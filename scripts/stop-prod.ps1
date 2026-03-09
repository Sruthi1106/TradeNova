$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (-not (Test-Path ".env.prod")) {
  Write-Host ".env.prod not found. Using compose defaults." -ForegroundColor Yellow
  docker compose -f docker-compose.prod.yml down
  exit 0
}

docker compose -f docker-compose.prod.yml --env-file .env.prod down
Write-Host "Production backend stopped." -ForegroundColor Green