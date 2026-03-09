param(
  [switch]$Build
)

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
  Write-Host "Docker CLI not found. Install Docker Desktop and reopen terminal." -ForegroundColor Red
  exit 1
}

$composeCmd = @($docker.Source, 'compose')
try {
  & $docker.Source compose version | Out-Null
} catch {
  $composePath = "C:\Program Files\Docker\Docker\resources\bin\docker-compose.exe"
  if (-not (Test-Path $composePath)) {
    $composePath = "C:\Program Files\Docker\Docker\resources\bin\docker-compose"
  }
  if (-not (Test-Path $composePath)) {
    Write-Host "Neither 'docker compose' nor 'docker-compose' is available." -ForegroundColor Red
    exit 1
  }
  $composeCmd = @($composePath)
}

function Invoke-Compose {
  param([string[]]$Args)

  if ($composeCmd.Length -gt 1) {
    & $composeCmd[0] $composeCmd[1] @Args
  } else {
    & $composeCmd[0] @Args
  }
}

if (-not (Test-Path ".env.prod")) {
  Copy-Item ".env.prod.example" ".env.prod"
  Write-Host "Created .env.prod from template. Fill it and rerun this script." -ForegroundColor Yellow
  exit 1
}

if ($Build) {
  Invoke-Compose -Args @('-f', 'docker-compose.prod.yml', '--env-file', '.env.prod', 'build', 'backend')
}

Invoke-Compose -Args @('-f', 'docker-compose.prod.yml', '--env-file', '.env.prod', 'up', '-d', 'backend')

Write-Host "Backend deployed. Checking health..." -ForegroundColor Cyan
Start-Sleep -Seconds 5
try {
  Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get | ConvertTo-Json -Depth 5
} catch {
  Write-Host "Health check failed. Run: docker compose -f docker-compose.prod.yml --env-file .env.prod logs backend" -ForegroundColor Red
  exit 1
}

Write-Host "Deployment complete." -ForegroundColor Green