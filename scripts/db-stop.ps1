$ErrorActionPreference = "Stop"

Write-Host "Stopping Vector9 PostgreSQL database..."
docker compose down
Write-Host "Database stopped"