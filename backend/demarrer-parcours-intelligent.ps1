# Demarrer Eureka (8761), Gateway (8050) et Skill (8086) pour le Parcours Intelligent
# Prerequis : JDK 17 pour tout le backend.

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
Start-Process powershell -ArgumentList "-NoExit", "-Command", "${envPrefix}cd '$eurekaDir'; Write-Host '=== Eureka (8761) [JDK 17] ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Start-Sleep -Seconds 3

# 2) Gateway
$gatewayDir = Join-Path $root "Gateway"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "${envPrefix}cd '$gatewayDir'; Write-Host '=== Gateway (8050) [JDK 17] ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Start-Sleep -Seconds 5

# 3) Skill (Parcours Intelligent)
$skillDir = Join-Path $root "microservices\Skill"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "${envPrefix}cd '$skillDir'; Write-Host '=== Skill / Parcours Intelligent (8086) [JDK 17] ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

Write-Host ""
Write-Host "JDK 17 utilise : $jdk17" -ForegroundColor DarkGray
Write-Host "Trois fenetres ont ete ouvertes : Eureka, Gateway, Skill." -ForegroundColor Green
Write-Host "Attendez que chaque service affiche 'Started ...' puis testez :" -ForegroundColor Yellow
Write-Host "  - Eureka : http://localhost:8761" -ForegroundColor Gray
Write-Host "  - Gateway + Skill : http://localhost:8050/api/skills/ping" -ForegroundColor Gray
Write-Host "  - CRUD Compétences : http://localhost:4200/admin/skills (apres ng serve)" -ForegroundColor Gray
Write-Host "  - Parcours Intelligent : http://localhost:4200/parcours-intelligent" -ForegroundColor Gray
