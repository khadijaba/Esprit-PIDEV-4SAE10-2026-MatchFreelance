# Lancer Maven / les microservices avec JDK 17
# Utilisez ce script si JAVA_HOME pointe encore vers Java 8

$jdk17 = "C:\Program Files\Java\jdk-17"
if (-not (Test-Path "$jdk17\bin\java.exe")) {
    Write-Host "JDK 17 introuvable dans: $jdk17" -ForegroundColor Red
    exit 1
}
$env:JAVA_HOME = $jdk17
Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Green
& java -version
Write-Host ""
Write-Host "Exemples:" -ForegroundColor Cyan
Write-Host "  Eureka Server :  cd EurekaServer\EurekaServer ; ..\..\run-with-jdk17.ps1 ; .\mvnw.cmd spring-boot:run"
Write-Host "  Formation      :  cd microservices\Formation ; ..\..\run-with-jdk17.ps1 ; .\mvnw.cmd spring-boot:run"
Write-Host "  Ou dans ce terminal, JAVA_HOME est deja defini - lancez simplement mvnw.cmd depuis le module."
Write-Host ""
