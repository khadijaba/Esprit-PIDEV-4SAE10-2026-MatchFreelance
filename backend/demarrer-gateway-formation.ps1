# Demarrer Eureka (8761), Gateway (8050) et Formation (8081) pour Formations + Modules
# Ouvrir 3 fenetres PowerShell : une par service.
# Ensuite : ng serve dans frontend et aller sur http://localhost:4200/admin/formations

$root = $PSScriptRoot

# 1) Eureka
$eurekaDir = Join-Path $root "EurekaServer\EurekaServer"
if (-not (Test-Path $eurekaDir)) {
    $eurekaDir = Join-Path $root "EurekaServer"
}
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$eurekaDir'; Write-Host '=== Eureka (8761) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Start-Sleep -Seconds 5

# 2) Gateway
$gatewayDir = Join-Path $root "Gateway"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$gatewayDir'; Write-Host '=== Gateway (8050) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Start-Sleep -Seconds 5

# 3) Formation (formulations + modules)
$formationDir = Join-Path $root "microservices\Formation"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$formationDir'; Write-Host '=== Formation (8081) - Formations + Modules ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Write-Host ""
Write-Host "Trois fenetres ouvertes : Eureka, Gateway, Formation." -ForegroundColor Green
Write-Host "Attendez que chaque service affiche 'Started ...' (environ 30-60 s)." -ForegroundColor Yellow
Write-Host ""
Write-Host "Puis :" -ForegroundColor White
Write-Host "  1. Dans un autre terminal : cd frontend puis ng serve" -ForegroundColor Gray
Write-Host "  2. Ouvrir http://localhost:4200/admin/formations" -ForegroundColor Gray
Write-Host "  3. Cliquer sur une formation -> Ajouter un module" -ForegroundColor Gray
Write-Host ""
Write-Host "Tests API :" -ForegroundColor White
Write-Host "  - Gateway : http://localhost:8050/api/formations" -ForegroundColor Gray
Write-Host "  - Modules : http://localhost:8050/api/modules/formation/1" -ForegroundColor Gray
