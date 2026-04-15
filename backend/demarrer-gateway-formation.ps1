# Demarrer Eureka (8761), Gateway (8050) et Formation (8081) pour Formations + Modules
# Prerequis : JDK 17 pour tout le backend (definir JAVA_HOME ou laisser la detection ci-dessous).

$root = $PSScriptRoot

$jdk17 = $null
foreach ($c in @(
        $env:JAVA_HOME,
        "C:\Users\benay\.jdks\ms-17.0.18",
        "C:\Program Files\Java\jdk-17"
    )) {
    if ($c -and (Test-Path (Join-Path $c "bin\java.exe"))) {
        $jdk17 = $c
        break
    }
}
if (-not $jdk17) {
    Write-Host "ERREUR : JDK 17 introuvable. Installez un JDK 17 et definissez JAVA_HOME." -ForegroundColor Red
    exit 1
}

$envPrefix = "`$env:JAVA_HOME='$jdk17'; `$env:Path='$jdk17\bin;' + `$env:Path; "

# 1) Eureka
$eurekaDir = Join-Path $root "EurekaServer\EurekaServer"
if (-not (Test-Path $eurekaDir)) {
    $eurekaDir = Join-Path $root "EurekaServer"
}
Start-Process powershell -ArgumentList "-NoExit", "-Command", "${envPrefix}cd '$eurekaDir'; Write-Host '=== Eureka (8761) [JDK 17] ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Start-Sleep -Seconds 5

# 2) Gateway
$gatewayDir = Join-Path $root "Gateway"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "${envPrefix}cd '$gatewayDir'; Write-Host '=== Gateway (8050) [JDK 17] ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Start-Sleep -Seconds 5

# 3) Formation
$formationDir = Join-Path $root "microservices\Formation"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "${envPrefix}cd '$formationDir'; Write-Host '=== Formation (8081) [JDK 17] ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Write-Host ""
Write-Host "JDK 17 utilise : $jdk17" -ForegroundColor DarkGray
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
