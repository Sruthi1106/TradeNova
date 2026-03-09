$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

$docker = Get-Command docker -ErrorAction SilentlyContinue
if (-not $docker) {
  $dockerPath = "C:\Program Files\Docker\Docker\resources\bin\docker.exe"
  if (Test-Path $dockerPath) {
    $docker = @{ Source = $dockerPath }
  }
}

if (-not $docker) {
  Write-Host "Docker CLI not found. Nothing to stop." -ForegroundColor Yellow
  exit 0
}

if (-not (Test-Path ".env.prod")) {
  Write-Host ".env.prod not found. Using compose defaults." -ForegroundColor Yellow
  & $docker.Source compose -f docker-compose.prod.yml down
  exit 0
}

& $docker.Source compose -f docker-compose.prod.yml --env-file .env.prod down
Write-Host "Production backend stopped." -ForegroundColor Green