param(
  [string]$Model = $(if ($env:OLLAMA_MODEL) { $env:OLLAMA_MODEL } else { "phi4-mini" })
)

$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot

Write-Host "Starting Local SLM Gateway (model=$Model)"

# Load local env file (recommended) if it exists.
# This lets you keep secrets out of git while still using Docker Compose env_file support.
function Import-EnvFile {
  param([string] $Path)

  if (!(Test-Path $Path)) {
    return
  }

  Write-Host "Loading env from $Path"
  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if (!$line -or $line.StartsWith("#")) { return }
    $parts = $line.Split("=", 2)
    if ($parts.Length -ne 2) { return }
    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    if ($name) { Set-Item -Path "Env:$name" -Value $value }
  }
}

# Prefer local-only file name.
Import-EnvFile -Path (Join-Path $PSScriptRoot "gateway.env.local")

# Backwards-compatible: also load gateway.env if you use it locally.
Import-EnvFile -Path (Join-Path $PSScriptRoot "gateway.env")

# Persist model selection for docker-compose via environment variable (overrides gateway.env).
$env:OLLAMA_MODEL = $Model

function Assert-DockerRunning {
  # Fail fast with a clear message if Docker Desktop / Engine is not available.
  try {
    $null = docker version 2>$null
  } catch {
    throw @"
Docker is not available.

This script uses Docker Compose. Please start Docker Desktop first.

Quick checks:
  - Is Docker Desktop running?
  - In Docker Desktop, is the Linux engine enabled?
  - Run: docker version
"@
  }
}

Assert-DockerRunning

docker compose up -d --build

Write-Host ""
Write-Host "Health check:"
# Give the container a moment to bind the port.
Start-Sleep -Seconds 2
Invoke-RestMethod "http://localhost:8090/health"

Write-Host ""
Write-Host "Tip: first-time model download can be large."
Write-Host "To stop: docker compose down"

