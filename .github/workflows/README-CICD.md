# 🚀 CI/CD Pipeline Documentation - User Microservice

## 📋 Table des Matières
- [Vue d'ensemble](#vue-densemble)
- [Workflows Disponibles](#workflows-disponibles)
- [Configuration Requise](#configuration-requise)
- [Guide d'Utilisation](#guide-dutilisation)
- [Stratégie de Déploiement](#stratégie-de-déploiement)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Vue d'ensemble

Ce projet utilise GitHub Actions pour automatiser l'ensemble du cycle de vie du développement, des tests au déploiement en production.

### Architecture du Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                    PUSH / PULL REQUEST                       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 1: Quality & Tests                                    │
│  ✓ Compilation                                               │
│  ✓ Code Quality Analysis                                     │
│  ✓ Unit Tests (101 tests)                                    │
│  ✓ Test Coverage Analysis                                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 2: Build & Package                                    │
│  ✓ Maven Build (JAR)                                         │
│  ✓ Docker Image Build                                        │
│  ✓ Push to Container Registry                                │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 3: Security Scan                                      │
│  ✓ Container Vulnerability Scan (Trivy)                      │
│  ✓ Security Report Generation                                │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 4: Deployment                                         │
│  ✓ Deploy to Staging (develop branch)                        │
│  ✓ Deploy to Production (main branch)                        │
│  ✓ Health Checks                                             │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 5: Post-Deployment                                    │
│  ✓ Deployment Report                                         │
│  ✓ Notifications                                             │
│  ✓ Monitoring Setup                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Workflows Disponibles

### 1. 🚀 Complete CI/CD Pipeline (`cicd-complete.yml`)
**Déclenchement**: Push sur `main`, `develop`, `User` ou Pull Request

**Étapes**:
- ✅ Tests et qualité du code
- 🏗️ Build et packaging
- 🛡️ Scan de sécurité
- 🚀 Déploiement automatique
- 📊 Rapports post-déploiement

**Durée estimée**: 5-8 minutes

### 2. 🧪 Tests Only (`tests-only.yml`)
**Déclenchement**: Push ou Pull Request

**Étapes**:
- ✅ Tests unitaires uniquement
- 📊 Statistiques de tests
- 📁 Upload des rapports

**Durée estimée**: 2-3 minutes

### 3. ✅ Basic Test (`basic-test.yml`)
**Déclenchement**: Push ou Pull Request

**Étapes**:
- ✅ Vérification Maven
- ✅ Compilation basique

**Durée estimée**: 30 secondes

### 4. 🚀 Manual Deployment (`manual-deploy.yml`)
**Déclenchement**: Manuel via GitHub UI

**Paramètres**:
- `environment`: staging ou production
- `version`: Version à déployer (optionnel)
- `skip_tests`: Ignorer les tests (non recommandé)

**Utilisation**:
1. Aller dans Actions → Manual Deployment
2. Cliquer "Run workflow"
3. Sélectionner les paramètres
4. Confirmer le déploiement

### 5. 🔄 Rollback (`rollback.yml`)
**Déclenchement**: Manuel via GitHub UI

**Paramètres**:
- `environment`: Environnement à rollback
- `target_version`: Version cible (optionnel)
- `reason`: Raison du rollback (requis)

**Utilisation**:
1. Aller dans Actions → Rollback
2. Cliquer "Run workflow"
3. Spécifier l'environnement et la raison
4. Confirmer le rollback

---

## ⚙️ Configuration Requise

### Secrets GitHub

Configurez les secrets suivants dans votre repository :

```bash
# Container Registry (automatique avec GITHUB_TOKEN)
GITHUB_TOKEN  # Fourni automatiquement par GitHub

# Kubernetes (si déploiement réel)
KUBE_CONFIG   # Configuration kubectl encodée en base64

# Notifications (optionnel)
SLACK_WEBHOOK_URL    # Pour notifications Slack
TEAMS_WEBHOOK_URL    # Pour notifications Teams

# Sécurité (optionnel)
SNYK_TOKEN          # Pour scans Snyk
```

### Environnements GitHub

Créez les environnements suivants avec protection :

#### **Staging**
- Protection: Aucune approbation requise
- URL: `https://user-service-staging.yourdomain.com`
- Secrets spécifiques à staging

#### **Production**
- Protection: Approbation requise (1+ reviewers)
- URL: `https://user-service.yourdomain.com`
- Secrets spécifiques à production

---

## 📖 Guide d'Utilisation

### Workflow Automatique

#### Pour le Développement (branche `develop`)
```bash
# 1. Créer une branche feature
git checkout -b feature/nouvelle-fonctionnalite

# 2. Développer et commiter
git add .
git commit -m "feat: nouvelle fonctionnalité"

# 3. Pousser vers GitHub
git push origin feature/nouvelle-fonctionnalite

# 4. Créer une Pull Request vers develop
# → Les tests s'exécutent automatiquement

# 5. Merger la PR
# → Déploiement automatique vers STAGING
```

#### Pour la Production (branche `main`)
```bash
# 1. Créer une PR de develop vers main
# → Tests complets s'exécutent

# 2. Approuver et merger
# → Déploiement automatique vers PRODUCTION
```

### Déploiement Manuel

#### Déployer une Version Spécifique
1. Aller dans **Actions** → **Manual Deployment**
2. Cliquer **Run workflow**
3. Sélectionner:
   - Environment: `staging` ou `production`
   - Version: `1.0.50` (exemple)
   - Skip tests: `false`
4. Cliquer **Run workflow**

#### Rollback en Cas de Problème
1. Aller dans **Actions** → **Rollback**
2. Cliquer **Run workflow**
3. Remplir:
   - Environment: `production`
   - Target version: `1.0.49` (version précédente)
   - Reason: "Bug critique en production"
4. Cliquer **Run workflow**

---

## 🎯 Stratégie de Déploiement

### Versioning

Le système utilise un versioning automatique :

```
Format: MAJOR.MINOR.BUILD_NUMBER[-SUFFIX]

Exemples:
- main branch:    1.0.100
- develop branch: 1.0.100-dev
- User branch:    1.0.100-User
- manual deploy:  manual-42
```

### Environnements

| Environnement | Branche | Déploiement | Approbation |
|---------------|---------|-------------|-------------|
| **Staging** | `develop` | Automatique | Non |
| **Production** | `main` | Automatique | Oui |
| **Feature** | `feature/*` | Manuel | Non |

### Stratégie de Tests

```
┌─────────────────────────────────────────┐
│  Pull Request                            │
│  ✓ Tests unitaires (101 tests)          │
│  ✓ Compilation                           │
│  ✓ Code quality                          │
└─────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Merge to develop                        │
│  ✓ Tous les tests PR                    │
│  ✓ Build Docker                          │
│  ✓ Security scan                         │
│  ✓ Deploy to staging                     │
└─────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Merge to main                           │
│  ✓ Tous les tests develop               │
│  ✓ Approbation manuelle                 │
│  ✓ Deploy to production                 │
│  ✓ Post-deployment tests                │
└─────────────────────────────────────────┘
```

---

## 🔧 Troubleshooting

### Problème: Tests Échouent

**Symptômes**: ❌ Tests failed in CI

**Solutions**:
```bash
# 1. Exécuter les tests localement
cd User
mvn clean test

# 2. Vérifier les logs dans GitHub Actions
# Actions → Workflow run → Job → Step logs

# 3. Vérifier la base de données MySQL
# Le workflow utilise MySQL 8.0 avec:
# - Database: userdb_test
# - User: testuser
# - Password: testpass
```

### Problème: Build Docker Échoue

**Symptômes**: ❌ Docker build failed

**Solutions**:
```bash
# 1. Tester le build localement
cd User
docker build -t user-service:test .

# 2. Vérifier le Dockerfile
# S'assurer que tous les fichiers nécessaires existent

# 3. Vérifier les logs de build
# Actions → Build & Package → Build and Push Docker Image
```

### Problème: Déploiement Échoue

**Symptômes**: ❌ Deployment failed

**Solutions**:
```bash
# 1. Vérifier les secrets GitHub
# Settings → Secrets and variables → Actions

# 2. Vérifier les environnements
# Settings → Environments → staging/production

# 3. Exécuter un rollback si nécessaire
# Actions → Rollback → Run workflow
```

### Problème: Workflow Ne Se Déclenche Pas

**Symptômes**: Aucun workflow ne démarre après push

**Solutions**:
```bash
# 1. Vérifier la branche
git branch  # Doit être main, develop, ou User

# 2. Vérifier les chemins modifiés
# Les workflows se déclenchent sur les changements dans User/**

# 3. Vérifier les permissions
# Settings → Actions → General → Workflow permissions
```

---

## 📊 Métriques et Monitoring

### Métriques Collectées

- ✅ **Taux de réussite des tests**: 100% attendu
- ⏱️ **Durée du pipeline**: 5-8 minutes
- 🐳 **Taille de l'image Docker**: ~200MB
- 📦 **Nombre de tests**: 101 tests
- 🔒 **Vulnérabilités**: 0 critique attendu

### Rapports Disponibles

Après chaque exécution, les artefacts suivants sont disponibles :

- 📁 **Test Reports**: Rapports JUnit XML
- 📊 **Coverage Reports**: Rapports JaCoCo (si configuré)
- 🐳 **Docker Images**: Stockés dans GitHub Container Registry
- 📝 **Deployment Reports**: Historique des déploiements

---

## 🎓 Best Practices

### Pour les Développeurs

1. **Toujours créer une branche feature**
   ```bash
   git checkout -b feature/ma-fonctionnalite
   ```

2. **Tester localement avant de pousser**
   ```bash
   mvn clean test
   ```

3. **Écrire des messages de commit clairs**
   ```bash
   git commit -m "feat: ajouter authentification OAuth2"
   ```

4. **Créer des PR petites et focalisées**
   - Une fonctionnalité par PR
   - Tests inclus
   - Documentation mise à jour

### Pour les Ops

1. **Surveiller les déploiements**
   - Vérifier les health checks
   - Monitorer les métriques
   - Garder un œil sur les logs

2. **Maintenir les secrets à jour**
   - Rotation régulière
   - Audit des accès
   - Documentation des changements

3. **Tester les rollbacks régulièrement**
   - Exercices de disaster recovery
   - Documentation des procédures
   - Formation de l'équipe

---

## 📞 Support

### Ressources

- 📖 **Documentation GitHub Actions**: https://docs.github.com/actions
- 🐳 **Docker Documentation**: https://docs.docker.com
- ☸️ **Kubernetes Documentation**: https://kubernetes.io/docs

### Contact

Pour toute question ou problème :
1. Créer une issue GitHub
2. Contacter l'équipe DevOps
3. Consulter la documentation du projet

---

**Dernière mise à jour**: $(date)
**Version**: 1.0.0
**Maintenu par**: DevOps Team