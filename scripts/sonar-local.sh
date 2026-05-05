#!/bin/bash

# Script pour exécuter l'analyse SonarCloud localement
# Usage: ./scripts/sonar-local.sh [SONAR_TOKEN]

set -e

echo "🔍 Starting SonarCloud Local Analysis for User Microservice"
echo "============================================================"

# Vérifier si le token est fourni
if [ -z "$1" ]; then
    echo "❌ Error: SONAR_TOKEN is required"
    echo "Usage: ./scripts/sonar-local.sh YOUR_SONAR_TOKEN"
    echo ""
    echo "Get your token from: https://sonarcloud.io/account/security"
    exit 1
fi

SONAR_TOKEN=$1

# Vérifier si Maven est installé
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed"
    exit 1
fi

# Vérifier si Java est installé
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed"
    exit 1
fi

echo "✅ Prerequisites check passed"
echo ""

# Nettoyer les builds précédents
echo "🧹 Cleaning previous builds..."
mvn clean

# Compiler le projet
echo "🔨 Compiling project..."
mvn compile -B

# Exécuter les tests avec couverture
echo "🧪 Running tests with coverage..."
mvn test -B

# Générer le rapport JaCoCo
echo "📊 Generating JaCoCo coverage report..."
mvn jacoco:report

# Vérifier que le rapport existe
if [ ! -f "target/site/jacoco/jacoco.xml" ]; then
    echo "⚠️  Warning: JaCoCo report not found. Coverage data may be missing."
fi

# Exécuter l'analyse SonarCloud
echo "🔍 Running SonarCloud analysis..."
mvn sonar:sonar \
  -Dsonar.projectKey=user-microservice \
  -Dsonar.organization=your-org \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token=$SONAR_TOKEN \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
  -Dsonar.java.binaries=target/classes \
  -Dsonar.sources=src/main/java \
  -Dsonar.tests=src/test/java

echo ""
echo "✅ SonarCloud analysis completed successfully!"
echo "📊 View results at: https://sonarcloud.io/dashboard?id=user-microservice"
echo ""
