$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path ".env")) {
    Write-Host "Missing .env file. Copy .env.example to .env and set your Neon credentials." -ForegroundColor Red
    exit 1
}

Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $parts = $line -split "=", 2
    if ($parts.Count -eq 2) {
        $name = $parts[0].Trim()
        $value = $parts[1].Trim().Trim('"')
        Set-Item -Path "Env:$name" -Value $value
    }
}

if (-not $env:DATABASE_URL -or -not $env:DATABASE_URL.StartsWith("jdbc:")) {
    Write-Host "DATABASE_URL must start with jdbc:postgresql://..." -ForegroundColor Red
    exit 1
}

$port = if ($env:PORT) { $env:PORT } else { "8081" }
Write-Host "Starting solaris-billing-api on port $port..." -ForegroundColor Green
mvn spring-boot:run
