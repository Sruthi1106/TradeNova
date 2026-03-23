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
$composeCheckOk = $true
& $docker.Source compose version | Out-Null
if ($LASTEXITCODE -ne 0) {
  $composeCheckOk = $false
}

if (-not $composeCheckOk) {
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

$requiredVars = @(
  'SPRING_DATASOURCE_URL',
  'SPRING_DATASOURCE_USERNAME',
  'SPRING_DATASOURCE_PASSWORD',
  'JWT_SECRET',
  'SPRING_WEB_CORS_ALLOWED_ORIGINS',
  'SPRING_WEBSOCKET_ALLOWED_ORIGINS',
  'SPRING_PROFILES_ACTIVE'
)

$envPairs = @{ }
Get-Content ".env.prod" |
  Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } |
  ForEach-Object {
    $parts = $_ -split '=', 2
    $envPairs[$parts[0]] = $parts[1]
  }

$invalidVars = @()
foreach ($name in $requiredVars) {
  if (-not $envPairs.ContainsKey($name)) {
    $invalidVars += "$name (missing)"
    continue
  }

  $value = $envPairs[$name].Trim()
  if ([string]::IsNullOrWhiteSpace($value)) {
    $invalidVars += "$name (empty)"
    continue
  }

  if (
    $value -match '<.*>' -or
    $value -match 'AIVEN_HOST|AIVEN_PORT|AIVEN_USERNAME|AIVEN_PASSWORD|yourdomain|RANDOM_32_PLUS_CHAR_SECRET'
  ) {
    $invalidVars += "$name (placeholder value)"
  }
}

if ($invalidVars.Count -gt 0) {
  Write-Host "Cannot deploy. Update these .env.prod values first:" -ForegroundColor Red
  $invalidVars | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
  exit 1
}

if ($Build) {
  Invoke-Compose -Args @('-f', 'docker-compose.prod.yml', '--env-file', '.env.prod', 'build', 'backend')
}

Invoke-Compose -Args @('-f', 'docker-compose.prod.yml', '--env-file', '.env.prod', 'up', '-d', 'backend')

Write-Host "Backend deployed. Checking health..." -ForegroundColor Cyan
Start-Sleep -Seconds 5
try {
  Invoke-RestMethod -Uri "http://localhost:8080/api/v1/actuator/health" -Method Get | ConvertTo-Json -Depth 5
} catch {
  Write-Host "Health check failed. Run: docker compose -f docker-compose.prod.yml --env-file .env.prod logs backend" -ForegroundColor Red
  exit 1
}

Write-Host "Deployment complete." -ForegroundColor Green