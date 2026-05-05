@echo off
REM Script de démarrage du stack de monitoring (Windows)
REM Usage: scripts\start-monitoring.bat

echo.
echo 🚀 Démarrage du stack de monitoring pour User Microservice...
echo.

REM Vérifier que Docker est installé
docker --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker n'est pas installé
    echo Installez Docker: https://docs.docker.com/get-docker/
    exit /b 1
)

REM Vérifier que Docker Compose est installé
docker-compose --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker Compose n'est pas installé
    echo Installez Docker Compose: https://docs.docker.com/compose/install/
    exit /b 1
)

echo ✅ Docker et Docker Compose sont installés
echo.

REM Vérifier que le microservice User est accessible
echo 🔍 Vérification du microservice User...
curl -f -s --max-time 5 http://localhost:9090/actuator/health >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Microservice User est accessible
) else (
    echo ⚠️  Microservice User n'est pas accessible sur http://localhost:9090
    echo    Assurez-vous que le service est démarré avant de continuer.
    set /p CONTINUE="   Continuer quand même ? (y/n) "
    if /i not "%CONTINUE%"=="y" exit /b 1
)
echo.

REM Créer les dossiers nécessaires s'ils n'existent pas
echo 📁 Création des dossiers de configuration...
if not exist "monitoring\prometheus" mkdir monitoring\prometheus
if not exist "monitoring\grafana\provisioning\datasources" mkdir monitoring\grafana\provisioning\datasources
if not exist "monitoring\grafana\provisioning\dashboards" mkdir monitoring\grafana\provisioning\dashboards
if not exist "monitoring\grafana\dashboards" mkdir monitoring\grafana\dashboards
if not exist "monitoring\alertmanager" mkdir monitoring\alertmanager
echo ✅ Dossiers créés
echo.

REM Démarrer le stack de monitoring
echo 🐳 Démarrage des conteneurs Docker...
docker-compose -f docker-compose-monitoring.yml up -d

REM Attendre que les services soient prêts
echo.
echo ⏳ Attente du démarrage des services...
timeout /t 10 /nobreak >nul

REM Vérifier l'état des services
echo.
echo 📊 État des services:
docker-compose -f docker-compose-monitoring.yml ps

REM Vérifier que Prometheus est accessible
echo.
echo 🔍 Vérification de Prometheus...
curl -f -s --max-time 5 http://localhost:9091/-/healthy >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Prometheus est accessible: http://localhost:9091
) else (
    echo ❌ Prometheus n'est pas accessible
)

REM Vérifier que Grafana est accessible
echo 🔍 Vérification de Grafana...
curl -f -s --max-time 5 http://localhost:3001/api/health >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Grafana est accessible: http://localhost:3001
    echo    Identifiants: admin / admin123
) else (
    echo ❌ Grafana n'est pas accessible
)

REM Vérifier que Alertmanager est accessible
echo 🔍 Vérification de Alertmanager...
curl -f -s --max-time 5 http://localhost:9093/-/healthy >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Alertmanager est accessible: http://localhost:9093
) else (
    echo ❌ Alertmanager n'est pas accessible
)

echo.
echo ═══════════════════════════════════════════════════════════
echo.
echo 🎉 Stack de monitoring démarré avec succès !
echo.
echo 📊 Accès aux services:
echo    • Grafana:       http://localhost:3001 (admin/admin123)
echo    • Prometheus:    http://localhost:9091
echo    • Alertmanager:  http://localhost:9093
echo.
echo 📚 Documentation:
echo    • Guide complet: MONITORING-GUIDE.md
echo.
echo 🛠️  Commandes utiles:
echo    • Voir les logs:    docker-compose -f docker-compose-monitoring.yml logs -f
echo    • Arrêter:          docker-compose -f docker-compose-monitoring.yml down
echo    • Redémarrer:       docker-compose -f docker-compose-monitoring.yml restart
echo.
echo ═══════════════════════════════════════════════════════════
echo.
pause
