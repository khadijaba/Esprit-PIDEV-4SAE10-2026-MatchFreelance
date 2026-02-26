# Demarrer Eureka (8761), Gateway (8050) et Skill (8086) pour le Parcours Intelligent
# Ouvrir 3 fenetres PowerShell : une par service.

$root = $PSScriptRoot
$jdk17 = "C:\Users\benay\.jdks\ms-17.0.18"

# 1) Eureka
$eurekaDir = Join-Path $root "EurekaServer\EurekaServer"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$eurekaDir'; Write-Host '=== Eureka (8761) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Start-Sleep -Seconds 3

# 2) Gateway
$gatewayDir = Join-Path $root "Gateway"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$gatewayDir'; Write-Host '=== Gateway (8050) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Start-Sleep -Seconds 5

# 3) Skill (Parcours Intelligent) - necessite JDK 17
$skillDir = Join-Path $root "microservices\Skill"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:JAVA_HOME = '$jdk17'; cd '$skillDir'; Write-Host '=== Skill / Parcours Intelligent (8086) ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Write-Host ""
Write-Host "Trois fenetres ont ete ouvertes : Eureka, Gateway, Skill." -ForegroundColor Green
Write-Host "Attendez que chaque service affiche 'Started ...' puis testez :" -ForegroundColor Yellow
Write-Host "  - Eureka : http://localhost:8761" -ForegroundColor Gray
Write-Host "  - Gateway + Skill : http://localhost:8050/api/skills/ping" -ForegroundColor Gray
Write-Host "  - CRUD Compétences : http://localhost:4200/admin/skills (apres ng serve)" -ForegroundColor Gray
Write-Host "  - Parcours Intelligent : http://localhost:4200/parcours-intelligent" -ForegroundColor Gray
