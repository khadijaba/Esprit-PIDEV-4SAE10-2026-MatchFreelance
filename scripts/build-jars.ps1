# Build Spring Boot JARs for all Java microservices (atelier: mvn before docker compose).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$modules = @(
    "eureka-server",
    "config-server",
    "user-service",
    "project-service",
    "candidature-service",
    "contract-service",
    "interview-service",
    "evaluation-service",
    "formation-service",
    "skill-service",
    "analytics-service",
    "api-gateway"
)
foreach ($m in $modules) {
    $dir = Join-Path $root $m
    Write-Host ">>> mvn package: $m" -ForegroundColor Cyan
    Push-Location $dir
    try {
        mvn -B clean package -DskipTests -q
        if ($LASTEXITCODE -ne 0) { throw "mvn failed in $m" }
    } finally {
        Pop-Location
    }
}
Write-Host "Done. Run: docker compose up --build" -ForegroundColor Green
