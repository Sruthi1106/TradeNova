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

$composeCmd = @($docker.Source, 'compose')
try {
  & $docker.Source compose version | Out-Null
} catch {
  $composePath = "C:\Program Files\Docker\Docker\resources\bin\docker-compose.exe"
  if (-not (Test-Path $composePath)) {
    $composePath = "C:\Program Files\Docker\Docker\resources\bin\docker-compose"
  }
  if (-not (Test-Path $composePath)) {
    Write-Host "Compose runtime not found. Nothing to stop." -ForegroundColor Yellow
    exit 0
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
  Write-Host ".env.prod not found. Using compose defaults." -ForegroundColor Yellow
  Invoke-Compose -Args @('-f', 'docker-compose.prod.yml', 'down')
  exit 0
}

Invoke-Compose -Args @('-f', 'docker-compose.prod.yml', '--env-file', '.env.prod', 'down')
Write-Host "Production backend stopped." -ForegroundColor Green