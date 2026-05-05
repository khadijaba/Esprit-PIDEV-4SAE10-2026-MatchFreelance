@echo off
REM Script de vérification des corrections de sécurité SonarCloud (Windows)
REM Ce script vérifie que toutes les corrections sont bien présentes dans le code

echo.
echo 🔍 Vérification des corrections de sécurité SonarCloud...
echo.

set ERRORS=0

echo 📋 Vérification des 4 vulnérabilités corrigées :
echo.

REM 1. Blocker - Mot de passe en dur
echo 1️⃣  Blocker - Mot de passe (DataInitializer.java)
findstr /C:"ADMIN_DEFAULT_PASSWORD" "src\main\java\Config\DataInitializer.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ Variable d'environnement ADMIN_DEFAULT_PASSWORD utilisée
) else (
    echo    ❌ Variable d'environnement ADMIN_DEFAULT_PASSWORD NON trouvée
    set /a ERRORS+=1
)

findstr /C:"throw new IllegalStateException" "src\main\java\Config\DataInitializer.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ Exception levée si variable non définie
) else (
    echo    ❌ Exception NON trouvée
    set /a ERRORS+=1
)

findstr /C:"Admin@123" "src\main\java\Config\DataInitializer.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ❌ Mot de passe en dur trouvé dans le code !
    set /a ERRORS+=1
) else (
    echo    ✅ Aucun mot de passe en dur trouvé
)
echo.

REM 2. Critical - Entité exposée
echo 2️⃣  Critical - Entité exposée (UserController.java)
if exist "src\main\java\DTO\UserProfileDTO.java" (
    echo    ✅ UserProfileDTO créé
) else (
    echo    ❌ UserProfileDTO NON trouvé
    set /a ERRORS+=1
)

findstr /C:"UserProfileDTO" "src\main\java\Controller\UserController.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ UserProfileDTO utilisé dans UserController
) else (
    echo    ❌ UserProfileDTO NON utilisé dans UserController
    set /a ERRORS+=1
)
echo.

REM 3. Major - CORS
echo 3️⃣  Major - CORS (WebConfig.java ^& SecurityConfig.java)
findstr /C:"ALLOWED_ORIGINS" "src\main\java\Config\WebConfig.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ Variable d'environnement ALLOWED_ORIGINS utilisée (WebConfig)
) else (
    echo    ❌ Variable d'environnement ALLOWED_ORIGINS NON trouvée (WebConfig)
    set /a ERRORS+=1
)

findstr /C:"ALLOWED_ORIGINS" "src\main\java\Config\SecurityConfig.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ Variable d'environnement ALLOWED_ORIGINS utilisée (SecurityConfig)
) else (
    echo    ❌ Variable d'environnement ALLOWED_ORIGINS NON trouvée (SecurityConfig)
    set /a ERRORS+=1
)

findstr /C:"allowedOriginPatterns(\"*\")" "src\main\java\Config\WebConfig.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ❌ CORS non sécurisé trouvé (allowedOriginPatterns(*))
    set /a ERRORS+=1
) else (
    echo    ✅ Pas de CORS wildcard trouvé
)
echo.

REM 4. Minor - Logging
echo 4️⃣  Minor - Logging (UserService.java)
findstr /C:"user with ID:" "src\main\java\Service\UserService.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ Logging utilise l'ID utilisateur
) else (
    echo    ⚠️  Vérifier que le logging n'expose pas d'emails
)

findstr /C:"getEmail()" "src\main\java\Service\UserService.java" | findstr /C:"logger" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ❌ Logging d'email trouvé !
    set /a ERRORS+=1
) else (
    echo    ✅ Pas de logging d'email trouvé
)
echo.

echo ═══════════════════════════════════════════════════════════
echo.
echo 🛡️  Vérification des 2 Security Hotspots :
echo.

REM Hotspot 1 - CSRF
echo 1️⃣  CSRF (SecurityConfig.java)
findstr /C:"CSRF protection is disabled because" "src\main\java\Config\SecurityConfig.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ Commentaire explicatif CSRF présent
) else (
    echo    ❌ Commentaire explicatif CSRF NON trouvé
    set /a ERRORS+=1
)

findstr /C:"SessionCreationPolicy.STATELESS" "src\main\java\Config\SecurityConfig.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ API stateless configurée
) else (
    echo    ❌ API stateless NON configurée
    set /a ERRORS+=1
)
echo.

REM Hotspot 2 - BCrypt
echo 2️⃣  Weak Cryptography (SecurityConfig.java)
findstr /C:"BCryptPasswordEncoder(12)" "src\main\java\Config\SecurityConfig.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ BCrypt strength augmenté à 12
) else (
    echo    ❌ BCrypt strength NON augmenté à 12
    set /a ERRORS+=1
)

findstr /C:"Using BCrypt with strength 12" "src\main\java\Config\SecurityConfig.java" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo    ✅ Commentaire explicatif BCrypt présent
) else (
    echo    ❌ Commentaire explicatif BCrypt NON trouvé
    set /a ERRORS+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo.

REM Résumé
if %ERRORS% EQU 0 (
    echo ✅ TOUTES LES CORRECTIONS SONT EN PLACE !
    echo.
    echo 📝 Prochaines étapes :
    echo    1. Faire un push : git push origin User
    echo    2. Aller sur SonarCloud : https://sonarcloud.io/project/issues?id=aminepidevops123_user-service
    echo    3. Marquer manuellement les 4 issues comme 'Fixed'
    echo    4. Marquer les 2 hotspots (CSRF=Safe, BCrypt=Fixed)
    echo    5. Vérifier que Quality Gate = PASSED
    echo.
    echo 📖 Guide complet : User\GUIDE-RESOLUTION-SONARCLOUD.md
    exit /b 0
) else (
    echo ❌ %ERRORS% ERREUR(S) DÉTECTÉE(S)
    echo.
    echo Veuillez corriger les erreurs ci-dessus avant de continuer.
    exit /b 1
)
