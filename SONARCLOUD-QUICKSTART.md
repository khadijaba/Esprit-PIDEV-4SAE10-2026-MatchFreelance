# 🚀 Guide Rapide - Intégration SonarCloud avec GitHub

## ⚡ Configuration en 5 Minutes

### Étape 1️⃣ : Créer un compte SonarCloud

1. Allez sur **[sonarcloud.io](https://sonarcloud.io)**
2. Cliquez sur **"Log in"** → **"With GitHub"**
3. Autorisez SonarCloud à accéder à votre compte GitHub

### Étape 2️⃣ : Importer votre projet

1. Dans SonarCloud, cliquez sur **"+"** (en haut à droite) → **"Analyze new project"**
2. Sélectionnez votre repository GitHub
3. Cliquez sur **"Set Up"**
4. Choisissez **"With GitHub Actions"**
5. **Notez les informations suivantes :**
   - **Organization Key** : `votre-org` (exemple: `mohamedaziz-esprit`)
   - **Project Key** : `user-microservice` (ou celui généré automatiquement)

### Étape 3️⃣ : Obtenir le Token SonarCloud

1. Dans SonarCloud, cliquez sur votre avatar (en haut à droite)
2. Allez dans **"My Account"** → **"Security"**
3. Dans la section **"Generate Tokens"** :
   - **Name** : `GitHub Actions User Service`
   - **Type** : `Global Analysis Token`
   - **Expires in** : `No expiration` (ou selon votre préférence)
4. Cliquez sur **"Generate"**
5. **Copiez le token** (vous ne pourrez plus le voir après !)

### Étape 4️⃣ : Ajouter le Token dans GitHub

1. Allez sur votre repository GitHub
2. Cliquez sur **"Settings"** → **"Secrets and variables"** → **"Actions"**
3. Cliquez sur **"New repository secret"**
4. Ajoutez :
   - **Name** : `SONAR_TOKEN`
   - **Secret** : Collez le token copié à l'étape 3
5. Cliquez sur **"Add secret"**

### Étape 5️⃣ : Mettre à jour la configuration

Modifiez les fichiers suivants avec votre **Organization Key** :

#### A. `User/.github/workflows/sonarcloud.yml`
```yaml
-Dsonar.organization=VOTRE-ORG  # Remplacez par votre organization
```

#### B. `User/pom.xml`
```xml
<sonar.organization>VOTRE-ORG</sonar.organization>  <!-- Remplacez -->
```

#### C. `User/sonar-project.properties`
```properties
sonar.organization=VOTRE-ORG  # Remplacez
```

#### D. `User/.sonarcloud.properties`
```properties
sonar.organization=VOTRE-ORG  # Remplacez
```

### Étape 6️⃣ : Pousser sur GitHub

```bash
cd User

# Ajouter les fichiers
git add .

# Commit
git commit -m "feat: Add SonarCloud integration"

# Push (déclenche automatiquement l'analyse)
git push origin main
```

### Étape 7️⃣ : Vérifier l'analyse

1. **Sur GitHub** :
   - Allez dans l'onglet **"Actions"**
   - Vous devriez voir le workflow **"🔍 SonarCloud Analysis"** en cours d'exécution
   - Attendez qu'il se termine (environ 2-5 minutes)

2. **Sur SonarCloud** :
   - Allez sur [sonarcloud.io](https://sonarcloud.io)
   - Cliquez sur votre projet **"User Microservice"**
   - Consultez les résultats de l'analyse

---

## 🎯 Déclenchement Automatique

L'analyse SonarCloud se déclenche automatiquement dans les cas suivants :

✅ **Push sur les branches** :
- `main`
- `develop`
- `master`
- `User`

✅ **Pull Requests** vers :
- `main`
- `develop`
- `master`

✅ **Déclenchement manuel** :
- Via l'onglet "Actions" → "Run workflow"

---

## 📊 Voir les Résultats

### Sur GitHub
- **Actions** → Workflow **"🔍 SonarCloud Analysis"**
- Cliquez sur le run pour voir les détails
- Consultez le **Summary** pour un aperçu rapide

### Sur SonarCloud
- Dashboard : `https://sonarcloud.io/dashboard?id=user-microservice`
- **Overview** : Vue d'ensemble
- **Issues** : Bugs, vulnérabilités, code smells
- **Security Hotspots** : Points sensibles
- **Measures** : Métriques détaillées
- **Code** : Navigation dans le code

---

## 🏆 Ajouter des Badges au README

Ajoutez ces badges à votre `README.md` :

```markdown
## 📊 Code Quality

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=alert_status)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=coverage)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=bugs)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=security_rating)](https://sonarcloud.io/dashboard?id=user-microservice)
```

---

## 🔧 Commandes Utiles

### Tester localement (avant de push)
```bash
# Windows
scripts\sonar-local.bat VOTRE_SONAR_TOKEN

# Linux/Mac
./scripts/sonar-local.sh VOTRE_SONAR_TOKEN
```

### Forcer une nouvelle analyse
```bash
git commit --allow-empty -m "chore: Trigger SonarCloud analysis"
git push
```

### Voir les logs du workflow
```bash
# Via GitHub CLI
gh run list --workflow=sonarcloud.yml
gh run view --log
```

---

## ❓ Dépannage

### ❌ Erreur : "SONAR_TOKEN not found"
**Solution** : Vérifiez que le secret `SONAR_TOKEN` est bien configuré dans GitHub Settings → Secrets

### ❌ Erreur : "Project not found"
**Solution** : Vérifiez que `sonar.projectKey` et `sonar.organization` correspondent à votre projet SonarCloud

### ❌ Erreur : "Insufficient privileges"
**Solution** : Régénérez un nouveau token avec les permissions "Execute Analysis"

### ⚠️ Pas de couverture de code
**Solution** : Vérifiez que les tests s'exécutent correctement et que JaCoCo génère le rapport

### ⏱️ Timeout du Quality Gate
**Solution** : C'est normal, le workflow continue même si le Quality Gate prend du temps

---

## 📚 Ressources

- [Documentation SonarCloud](https://docs.sonarcloud.io/)
- [GitHub Actions avec SonarCloud](https://docs.sonarcloud.io/advanced-setup/ci-based-analysis/github-actions/)
- [Règles Java](https://rules.sonarsource.com/java/)
- [Quality Gates](https://docs.sonarcloud.io/improving/quality-gates/)

---

## ✅ Checklist de Configuration

- [ ] Compte SonarCloud créé
- [ ] Projet importé dans SonarCloud
- [ ] Token SonarCloud généré
- [ ] Secret `SONAR_TOKEN` ajouté dans GitHub
- [ ] Organization Key mise à jour dans les fichiers
- [ ] Fichiers poussés sur GitHub
- [ ] Workflow exécuté avec succès
- [ ] Résultats visibles sur SonarCloud
- [ ] Badges ajoutés au README

---

**🎉 Félicitations ! Votre microservice User est maintenant analysé automatiquement par SonarCloud à chaque push !**
