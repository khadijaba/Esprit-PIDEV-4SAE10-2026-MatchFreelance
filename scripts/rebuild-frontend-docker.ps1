# Rebuild nginx Angular image so http://localhost:4200 matches your current source.
# Editing files on disk does NOT update Docker until you run this (or full compose build).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
Write-Host "Building frontend image (npm ci + ng build inside Docker)..." -ForegroundColor Cyan
docker compose build frontend --no-cache
if ($LASTEXITCODE -ne 0) { throw "docker compose build frontend failed" }
docker compose up -d frontend
Write-Host "Done. Hard-refresh the browser (Ctrl+Shift+R) on http://localhost:4200" -ForegroundColor Green
