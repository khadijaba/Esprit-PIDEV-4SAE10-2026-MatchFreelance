# 🚀 Workflows Actifs - User Microservice

## ✅ Workflows Déclenchés à Chaque Push

Tous les workflows suivants sont maintenant **actifs** et se déclenchent automatiquement à chaque push sur les branches `main`, `develop`, et `User`.

---

## 📋 Liste des Workflows Actifs

### 1. ✅ Basic Test
**Fichier**: `basic-test.yml`  
**Déclencheurs**: Push sur toutes les branches  
**Durée**: ~7-10 secondes  
**Description**: Validation rapide de la compilation  
**Status**: ✅ ACTIF

### 2. 🧪 Simple Tests - User Microservice
**Fichier**: `test-simple.yml`  
**Déclencheurs**: Push, Pull Request  
**Durée**: ~1-2 minutes  
**Description**: Tests avec MySQL  
**Status**: ✅ ACTIF

### 3. 🧪 Tests Only - User Microservice
**Fichier**: `tests-only.yml`  
**Déclencheurs**: Push, Pull Request  
**Durée**: ~1 minute  
**Description**: Tests unitaires uniquement  
**Status**: ✅ ACTIF

### 4. 🚀 Complete CI/CD Pipeline
**Fichier**: `cicd-complete.yml`  
**Déclencheurs**: Push, Pull Request  
**Durée**: ~5-8 minutes  
**Description**: Pipeline complet (build, test, Docker, deploy)  
**Status**: ✅ ACTIF (avec issue Docker à résoudre)

### 5. 🛡️ Security & Compliance
**Fichier**: `security.yml`  
**Déclencheurs**: 
- Push sur main, develop, User
- Pull Request
- Quotidien à 3h UTC
- Manuel
**Description**: 
- SAST Analysis (CodeQL, Semgrep)
- Dependency Vulnerability Scan (OWASP, Snyk)
- Container Security Scan (Trivy, Hadolint)
- Secrets Detection (TruffleHog, GitLeaks)
- License Compliance
**Status**: ✅ ACTIF

### 6. 📊 Monitoring & Health Checks
**Fichier**: `monitoring.yml`  
**Déclencheurs**: 
- Push sur main, develop, User
- Pull Request
- Toutes les 6 heures
- Manuel
**Description**: 
- Health checks (production, staging)
- Performance metrics collection
- Log analysis
- Synthetic transaction tests
**Status**: ✅ ACTIF

### 7. 🚀 Performance Testing
**Fichier**: `performance.yml`  
**Déclencheurs**: 
- Push sur main, develop, User
- Pull Request
- Hebdomadaire (dimanche 2h UTC)
- Manuel
**Description**: 
- Load testing avec JMeter
- Memory & CPU profiling
- Performance benchmarks
**Status**: ✅ ACTIF

### 8. 🧹 Cleanup
**Fichier**: `cleanup.yml`  
**Déclencheurs**: 
- Push sur main, develop, User
- Hebdomadaire (dimanche 2h UTC)
- Manuel
**Description**: 
- Nettoyage des anciens artifacts
- Nettoyage des anciennes images Docker
- Nettoyage des anciens workflow runs
**Status**: ✅ ACTIF

### 9. 📊 Metrics & Reports
**Fichier**: `metrics.yml`  
**Déclencheurs**: 
- Push sur main, develop, User
- Pull Request
- Quotidien à 8h UTC
- Manuel
**Description**: 
- Collection de métriques de code
- Génération de rapports hebdomadaires
- Statistiques de CI/CD
**Status**: ✅ ACTIF

### 10. 📢 Notifications
**Fichier**: `notifications.yml`  
**Déclencheurs**: Après l'exécution du workflow CI/CD complet  
**Description**: 
- Notifications Email (simulation)
- Notifications Slack (simulation)
- Notifications Teams (simulation)
- Création d'issues en cas d'échec
**Status**: ✅ ACTIF

---

## 🔧 Workflows Manuels Uniquement

Ces workflows ne se déclenchent que manuellement via `workflow_dispatch`:

### 11. 🚀 Manual Deployment
**Fichier**: `manual-deploy.yml`  
**Description**: Déploiement manuel avec choix d'environnement et version  
**Status**: ⚙️ MANUEL UNIQUEMENT

### 12. 🔄 Rollback
**Fichier**: `rollback.yml`  
**Description**: Rollback d'urgence vers une version précédente  
**Status**: ⚙️ MANUEL UNIQUEMENT

### 13. 📦 Release
**Fichier**: `release.yml`  
**Déclencheurs**: 
- Push de tags `v*.*.*`
- Manuel
**Description**: Création de releases avec artifacts et Docker images  
**Status**: ⚙️ MANUEL/TAGS

### 14. 🚀 Deploy (Reusable)
**Fichier**: `deploy.yml`  
**Description**: Workflow réutilisable pour les déploiements  
**Status**: 🔄 WORKFLOW RÉUTILISABLE

### 15. 🚀 CI/CD Pipeline (Legacy - DISABLED)
**Fichier**: `ci.yml`  
**Description**: Ancien workflow, remplacé par cicd-complete.yml  
**Status**: ⏸️ DÉSACTIVÉ

---

## 📊 Résumé des Déclencheurs

| Événement | Nombre de Workflows |
|-----------|---------------------|
| Push (main/develop/User) | 9 workflows |
| Pull Request | 6 workflows |
| Schedule (quotidien) | 2 workflows |
| Schedule (hebdomadaire) | 2 workflows |
| Schedule (6 heures) | 1 workflow |
| Manuel uniquement | 3 workflows |
| Tags | 1 workflow |
| Workflow completion | 1 workflow |

---

## 🎯 Ce qui se Passe à Chaque Push

Lorsque vous faites un push sur la branche `User`, les workflows suivants s'exécutent **automatiquement**:

1. ✅ **Basic Test** (~10s) - Validation rapide
2. 🧪 **Simple Tests** (~1-2min) - Tests avec MySQL
3. 🧪 **Tests Only** (~1min) - Tests unitaires
4. 🚀 **CI/CD Complete** (~5-8min) - Pipeline complet
5. 🛡️ **Security** (~10-15min) - Scans de sécurité
6. 📊 **Monitoring** (~2-3min) - Health checks
7. 🚀 **Performance** (~5-10min) - Tests de performance
8. 🧹 **Cleanup** (~1-2min) - Nettoyage
9. 📊 **Metrics** (~1-2min) - Métriques

**Durée totale estimée**: ~25-45 minutes (en parallèle)

---

## 🔍 Vérification des Workflows Actifs

Pour vérifier que tous les workflows sont actifs sur GitHub:

1. Allez sur: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance/actions
2. Vous devriez voir tous les workflows listés ci-dessus
3. Après un push, vérifiez l'onglet "Actions" pour voir les exécutions

---

## ⚙️ Configuration Requise

### Secrets GitHub à Configurer

Pour que tous les workflows fonctionnent correctement, configurez ces secrets:

```
KUBE_CONFIG          # Configuration Kubernetes (base64)
SLACK_WEBHOOK_URL    # URL webhook Slack
TEAMS_WEBHOOK_URL    # URL webhook Teams
SNYK_TOKEN          # Token Snyk pour scan de vulnérabilités
GITLEAKS_LICENSE    # License GitLeaks (optionnel)
```

### Environments GitHub

Créez ces environments avec protection rules:

- **staging**: Environnement de staging
- **production**: Environnement de production (avec approbation requise)

---

## 📈 Métriques Actuelles

- **Tests**: 101 tests
- **Coverage**: 31% (objectif: 50%)
- **Workflows Actifs**: 9 sur push
- **Workflows Totaux**: 15
- **Build Time**: ~2-3 minutes
- **Pipeline Complet**: ~5-8 minutes

---

## 🚨 Issues Connues

### Docker Build Issue (cicd-complete.yml)
**Problème**: Le workflow `cicd-complete.yml` échoue lors du build Docker avec l'erreur "Dockerfile not found"  
**Status**: En investigation  
**Workaround**: Les autres workflows fonctionnent correctement

---

## 📝 Notes

- Tous les workflows sont configurés pour s'exécuter en parallèle quand possible
- Les workflows de nettoyage et métriques ont aussi des exécutions planifiées
- Les notifications sont simulées (à configurer avec de vrais webhooks)
- Les déploiements sont simulés (à configurer avec de vrais clusters Kubernetes)

---

**Dernière mise à jour**: 5 mai 2026  
**Version**: 1.0.0  
**Auteur**: DevOps Team
