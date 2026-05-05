@echo off
REM Script pour exécuter l'analyse SonarCloud localement (Windows)
REM Usage: scripts\sonar-local.bat [SONAR_TOKEN]

setlocal enabledelayedexpansion

echo 🔍 Starting SonarCloud Local Analysis for User Microservice
echo ============================================================

REM Vérifier si le token est fourni
if "%~1"=="" (
    echo ❌ Error: SONAR_TOKEN is required
    echo Usage: scripts\sonar-local.bat YOUR_SONAR_TOKEN
    echo.
    echo Get your token from: https://sonarcloud.io/account/security
    exit /b 1
)

set SONAR_TOKEN=%~1

REM Vérifier si Maven est installé
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Error: Maven is not installed
    exit /b 1
)

REM Vérifier si Java est installé
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Error: Java is not installed
    exit /b 1
)

echo ✅ Prerequisites check passed
echo.

REM Nettoyer les builds précédents
echo 🧹 Cleaning previous builds...
call mvn clean

REM Compiler le projet
echo 🔨 Compiling project...
call mvn compile -B

REM Exécuter les tests avec couverture
echo 🧪 Running tests with coverage...
call mvn test -B

REM Générer le rapport JaCoCo
echo 📊 Generating JaCoCo coverage report...
call mvn jacoco:report

REM Vérifier que le rapport existe
if not exist "target\site\jacoco\jacoco.xml" (
    echo ⚠️  Warning: JaCoCo report not found. Coverage data may be missing.
)

REM Exécuter l'analyse SonarCloud
echo 🔍 Running SonarCloud analysis...
call mvn sonar:sonar ^
  -Dsonar.projectKey=user-microservice ^
  -Dsonar.organization=aminepidevops123 ^
  -Dsonar.host.url=https://sonarcloud.io ^
  -Dsonar.token=%SONAR_TOKEN% ^
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml ^
  -Dsonar.java.binaries=target/classes ^
  -Dsonar.sources=src/main/java ^
  -Dsonar.tests=src/test/java

echo.
echo ✅ SonarCloud analysis completed successfully!
echo 📊 View results at: https://sonarcloud.io/dashboard?id=user-microservice
echo.

endlocal
