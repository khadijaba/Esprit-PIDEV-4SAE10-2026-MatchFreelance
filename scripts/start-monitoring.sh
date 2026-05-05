#!/bin/bash

# Script de démarrage du stack de monitoring
# Usage: ./scripts/start-monitoring.sh

set -e

echo "🚀 Démarrage du stack de monitoring pour User Microservice..."
echo ""

# Couleurs
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Vérifier que Docker est installé
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker n'est pas installé${NC}"
    echo "Installez Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# Vérifier que Docker Compose est installé
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose n'est pas installé${NC}"
    echo "Installez Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✅ Docker et Docker Compose sont installés${NC}"
echo ""

# Vérifier que le microservice User est accessible
echo "🔍 Vérification du microservice User..."
if curl -f -s --max-time 5 http://localhost:9090/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Microservice User est accessible${NC}"
else
    echo -e "${YELLOW}⚠️  Microservice User n'est pas accessible sur http://localhost:9090${NC}"
    echo "   Assurez-vous que le service est démarré avant de continuer."
    read -p "   Continuer quand même ? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
echo ""

# Créer les dossiers nécessaires s'ils n'existent pas
echo "📁 Création des dossiers de configuration..."
mkdir -p monitoring/prometheus
mkdir -p monitoring/grafana/provisioning/datasources
mkdir -p monitoring/grafana/provisioning/dashboards
mkdir -p monitoring/grafana/dashboards
mkdir -p monitoring/alertmanager
echo -e "${GREEN}✅ Dossiers créés${NC}"
echo ""

# Démarrer le stack de monitoring
echo "🐳 Démarrage des conteneurs Docker..."
docker-compose -f docker-compose-monitoring.yml up -d

# Attendre que les services soient prêts
echo ""
echo "⏳ Attente du démarrage des services..."
sleep 10

# Vérifier l'état des services
echo ""
echo "📊 État des services:"
docker-compose -f docker-compose-monitoring.yml ps

# Vérifier que Prometheus est accessible
echo ""
echo "🔍 Vérification de Prometheus..."
if curl -f -s --max-time 5 http://localhost:9091/-/healthy > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Prometheus est accessible: http://localhost:9091${NC}"
else
    echo -e "${RED}❌ Prometheus n'est pas accessible${NC}"
fi

# Vérifier que Grafana est accessible
echo "🔍 Vérification de Grafana..."
if curl -f -s --max-time 5 http://localhost:3001/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Grafana est accessible: http://localhost:3001${NC}"
    echo "   Identifiants: admin / admin123"
else
    echo -e "${RED}❌ Grafana n'est pas accessible${NC}"
fi

# Vérifier que Alertmanager est accessible
echo "🔍 Vérification de Alertmanager..."
if curl -f -s --max-time 5 http://localhost:9093/-/healthy > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Alertmanager est accessible: http://localhost:9093${NC}"
else
    echo -e "${RED}❌ Alertmanager n'est pas accessible${NC}"
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
echo -e "${GREEN}🎉 Stack de monitoring démarré avec succès !${NC}"
echo ""
echo "📊 Accès aux services:"
echo "   • Grafana:       http://localhost:3001 (admin/admin123)"
echo "   • Prometheus:    http://localhost:9091"
echo "   • Alertmanager:  http://localhost:9093"
echo ""
echo "📚 Documentation:"
echo "   • Guide complet: MONITORING-GUIDE.md"
echo ""
echo "🛠️  Commandes utiles:"
echo "   • Voir les logs:    docker-compose -f docker-compose-monitoring.yml logs -f"
echo "   • Arrêter:          docker-compose -f docker-compose-monitoring.yml down"
echo "   • Redémarrer:       docker-compose -f docker-compose-monitoring.yml restart"
echo ""
echo "═══════════════════════════════════════════════════════════"
