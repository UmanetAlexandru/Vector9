$ErrorActionPreference = "Stop"

$confirmation = Read-Host "This will delete all local database data. Type 'yes' to continue"
if ($confirmation -ne "yes") {
    Write-Host "Reset cancelled"
    exit 0
}

Write-Host "Stopping and removing database volumes..."
docker compose down -v

Write-Host "Starting fresh database..."
docker compose up -d postgres | Out-Null

Write-Host "Waiting for database to be ready..."
for ($i = 0; $i -lt 30; $i++) {
    docker compose exec -T postgres pg_isready -U vector9 -d vector9_dev *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Database reset complete"
        exit 0
    }
    Start-Sleep -Seconds 2
}

throw "Database did not become ready after reset."