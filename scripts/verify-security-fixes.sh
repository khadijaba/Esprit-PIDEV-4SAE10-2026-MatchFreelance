#!/bin/bash

# Script de vérification des corrections de sécurité SonarCloud
# Ce script vérifie que toutes les corrections sont bien présentes dans le code

echo "🔍 Vérification des corrections de sécurité SonarCloud..."
echo ""

ERRORS=0

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction de vérification
check_fix() {
    local file=$1
    local pattern=$2
    local description=$3
    
    if grep -q "$pattern" "$file" 2>/dev/null; then
        echo -e "${GREEN}✅ $description${NC}"
        return 0
    else
        echo -e "${RED}❌ $description${NC}"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

echo "📋 Vérification des 4 vulnérabilités corrigées :"
echo ""

# 1. Blocker - Mot de passe en dur
echo "1️⃣  Blocker - Mot de passe (DataInitializer.java)"
check_fix "src/main/java/Config/DataInitializer.java" \
    "ADMIN_DEFAULT_PASSWORD" \
    "   Variable d'environnement ADMIN_DEFAULT_PASSWORD utilisée"

check_fix "src/main/java/Config/DataInitializer.java" \
    "throw new IllegalStateException" \
    "   Exception levée si variable non définie"

if grep -q "Admin@123\|ChangeMe@123" "src/main/java/Config/DataInitializer.java" 2>/dev/null; then
    echo -e "${RED}   ❌ Mot de passe en dur trouvé dans le code !${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}   ✅ Aucun mot de passe en dur trouvé${NC}"
fi
echo ""

# 2. Critical - Entité exposée
echo "2️⃣  Critical - Entité exposée (UserController.java)"
check_fix "src/main/java/DTO/UserProfileDTO.java" \
    "class UserProfileDTO" \
    "   UserProfileDTO créé"

check_fix "src/main/java/Controller/UserController.java" \
    "UserProfileDTO" \
    "   UserProfileDTO utilisé dans UserController"

if grep -q "return ResponseEntity.ok(user)" "src/main/java/Controller/UserController.java" 2>/dev/null; then
    echo -e "${YELLOW}   ⚠️  Attention : Entité User peut-être encore retournée directement${NC}"
fi
echo ""

# 3. Major - CORS
echo "3️⃣  Major - CORS (WebConfig.java & SecurityConfig.java)"
check_fix "src/main/java/Config/WebConfig.java" \
    "ALLOWED_ORIGINS" \
    "   Variable d'environnement ALLOWED_ORIGINS utilisée (WebConfig)"

check_fix "src/main/java/Config/SecurityConfig.java" \
    "ALLOWED_ORIGINS" \
    "   Variable d'environnement ALLOWED_ORIGINS utilisée (SecurityConfig)"

if grep -q 'allowedOriginPatterns("\*")' "src/main/java/Config/WebConfig.java" 2>/dev/null; then
    echo -e "${RED}   ❌ CORS non sécurisé trouvé (allowedOriginPatterns(*))${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}   ✅ Pas de CORS wildcard trouvé${NC}"
fi
echo ""

# 4. Minor - Logging
echo "4️⃣  Minor - Logging (UserService.java)"
if grep -q "user with ID:" "src/main/java/Service/UserService.java" 2>/dev/null; then
    echo -e "${GREEN}   ✅ Logging utilise l'ID utilisateur${NC}"
else
    echo -e "${YELLOW}   ⚠️  Vérifier que le logging n'expose pas d'emails${NC}"
fi

if grep -q 'logger.*getEmail()' "src/main/java/Service/UserService.java" 2>/dev/null; then
    echo -e "${RED}   ❌ Logging d'email trouvé !${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "${GREEN}   ✅ Pas de logging d'email trouvé${NC}"
fi
echo ""

echo "═══════════════════════════════════════════════════════════"
echo ""
echo "🛡️  Vérification des 2 Security Hotspots :"
echo ""

# Hotspot 1 - CSRF
echo "1️⃣  CSRF (SecurityConfig.java)"
check_fix "src/main/java/Config/SecurityConfig.java" \
    "CSRF protection is disabled because" \
    "   Commentaire explicatif CSRF présent"

check_fix "src/main/java/Config/SecurityConfig.java" \
    "SessionCreationPolicy.STATELESS" \
    "   API stateless configurée"
echo ""

# Hotspot 2 - BCrypt
echo "2️⃣  Weak Cryptography (SecurityConfig.java)"
check_fix "src/main/java/Config/SecurityConfig.java" \
    "BCryptPasswordEncoder(12)" \
    "   BCrypt strength augmenté à 12"

check_fix "src/main/java/Config/SecurityConfig.java" \
    "Using BCrypt with strength 12" \
    "   Commentaire explicatif BCrypt présent"
echo ""

echo "═══════════════════════════════════════════════════════════"
echo ""

# Résumé
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ TOUTES LES CORRECTIONS SONT EN PLACE !${NC}"
    echo ""
    echo "📝 Prochaines étapes :"
    echo "   1. Faire un push : git push origin User"
    echo "   2. Aller sur SonarCloud : https://sonarcloud.io/project/issues?id=aminepidevops123_user-service"
    echo "   3. Marquer manuellement les 4 issues comme 'Fixed'"
    echo "   4. Marquer les 2 hotspots (CSRF=Safe, BCrypt=Fixed)"
    echo "   5. Vérifier que Quality Gate = PASSED"
    echo ""
    echo "📖 Guide complet : User/GUIDE-RESOLUTION-SONARCLOUD.md"
    exit 0
else
    echo -e "${RED}❌ $ERRORS ERREUR(S) DÉTECTÉE(S)${NC}"
    echo ""
    echo "Veuillez corriger les erreurs ci-dessus avant de continuer."
    exit 1
fi
