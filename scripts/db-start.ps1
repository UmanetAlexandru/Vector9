$ErrorActionPreference = "Stop"

Write-Host "Starting Vector9 PostgreSQL database..."
docker compose up -d postgres | Out-Null

Write-Host "Waiting for database to be ready..."
for ($i = 0; $i -lt 30; $i++) {
    docker compose exec -T postgres pg_isready -U vector9 -d vector9_dev *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Database is ready!"
        Write-Host "  Host: localhost:5432"
        Write-Host "  Database: vector9_dev"
        Write-Host "  User: vector9"
        Write-Host "  Password: vector9_dev_password"
        Write-Host ""
        Write-Host "Connection string:"
        Write-Host "  jdbc:postgresql://localhost:5432/vector9_dev"
        exit 0
    }
    Start-Sleep -Seconds 2
}

throw "Database did not become ready in time."