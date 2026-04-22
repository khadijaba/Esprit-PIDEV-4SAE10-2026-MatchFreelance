# Demarre les 4 services backend (Eureka, Skill, Project, ApiGateway)
# A lancer avant "npm start" dans frontend pour eviter ECONNREFUSED

$base = "C:\Users\emnad\Backend\BackEnd\BackEnd"
if (-not (Test-Path $base)) {
    Write-Host "Modifiez la variable base dans ce script pour pointer vers votre dossier BackEnd." -ForegroundColor Red
    exit 1
}

Write-Host "Demarrage des 4 services (4 fenetres)..." -ForegroundColor Cyan
Write-Host "Attendez 'Started ApiGatewayApplication' avant de lancer le frontend." -ForegroundColor Yellow

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$base\Eureka\eureka'; mvn spring-boot:run"
Start-Sleep -Seconds 3
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$base\Microservices\Skill'; mvn spring-boot:run"
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$base\Microservices\Project'; mvn spring-boot:run"
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$base\ApiGateway'; mvn spring-boot:run"

Write-Host "4 fenetres ouvertes. Attendez ~1 min puis: cd frontend && npm start" -ForegroundColor Green
