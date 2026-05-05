# 🔍 SonarCloud - Configuration Automatique avec GitHub

## 🎯 Ce qui a été configuré

✅ **Workflow GitHub Actions** qui s'exécute automatiquement à chaque push
✅ **Configuration SonarCloud** complète (pom.xml, sonar-project.properties)
✅ **Scripts de test local** (Windows et Linux)
✅ **Documentation complète** (guides, badges, FAQ)

---

## ⚡ Configuration Rapide (5 minutes)

### 1. Créer un compte SonarCloud
👉 [sonarcloud.io](https://sonarcloud.io) → Log in with GitHub

### 2. Importer votre projet
- Cliquez sur **"+"** → **"Analyze new project"**
- Sélectionnez votre repository
- Choisissez **"With GitHub Actions"**
- **Notez votre Organization Key** (exemple: `mohamedaziz-esprit`)

### 3. Ajouter le Token dans GitHub
- SonarCloud → Account → Security → Generate Token
- GitHub → Settings → Secrets → Actions → New secret
  - Name: `SONAR_TOKEN`
  - Value: [votre token]

### 4. Mettre à jour l'Organization Key

Remplacez `your-org` par votre organization dans ces fichiers :

```bash
# Fichiers à modifier :
User/.github/workflows/sonarcloud.yml
User/pom.xml
User/sonar-project.properties
User/.sonarcloud.properties
```

**Rechercher et remplacer** :
```
your-org  →  VOTRE-ORGANIZATION-KEY
```

### 5. Pousser sur GitHub

```bash
cd User
git add .
git commit -m "feat: Add SonarCloud integration"
git push origin main
```

✅ **C'est tout !** L'analyse se lance automatiquement.

---

## 🚀 Déclenchement Automatique

Le workflow SonarCloud s'exécute automatiquement sur :

### 📤 Push vers :
- `main`
- `develop`
- `master`
- `User`

### 🔀 Pull Requests vers :
- `main`
- `develop`
- `master`

### 🎯 Manuel :
- GitHub Actions → "Run workflow"

---

## 📊 Voir les Résultats

### Sur GitHub
**Actions** → **"🔍 SonarCloud Analysis"** → Cliquez sur le run

### Sur SonarCloud
👉 https://sonarcloud.io/dashboard?id=user-microservice

---

## 🏆 Ajouter des Badges

Ajoutez dans votre `README.md` :

```markdown
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=alert_status)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=coverage)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=bugs)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=security_rating)](https://sonarcloud.io/dashboard?id=user-microservice)
```

---

## 🧪 Tester Localement (Optionnel)

```bash
# Windows
scripts\sonar-local.bat VOTRE_SONAR_TOKEN

# Linux/Mac
./scripts/sonar-local.sh VOTRE_SONAR_TOKEN
```

---

## 📚 Documentation Complète

- 📖 **[SONARCLOUD-QUICKSTART.md](SONARCLOUD-QUICKSTART.md)** - Guide rapide détaillé
- 📖 **[SONARCLOUD-SETUP.md](SONARCLOUD-SETUP.md)** - Guide complet avec troubleshooting
- 📖 **[SONARCLOUD-INTEGRATION.md](SONARCLOUD-INTEGRATION.md)** - Détails de l'intégration
- 📖 **[SONARCLOUD-BADGES.md](SONARCLOUD-BADGES.md)** - Tous les badges disponibles

---

## ❓ Problèmes Courants

### ❌ "SONAR_TOKEN not found"
→ Vérifiez que le secret est bien configuré dans GitHub Settings → Secrets

### ❌ "Project not found"
→ Vérifiez que `sonar.projectKey` et `sonar.organization` sont corrects

### ❌ "Insufficient privileges"
→ Régénérez un nouveau token avec les bonnes permissions

### ⚠️ Pas de couverture
→ Vérifiez que les tests s'exécutent et que JaCoCo génère le rapport

---

## ✅ Checklist

- [ ] Compte SonarCloud créé
- [ ] Projet importé
- [ ] Token ajouté dans GitHub
- [ ] Organization Key mise à jour dans les 4 fichiers
- [ ] Code poussé sur GitHub
- [ ] Workflow exécuté avec succès
- [ ] Résultats visibles sur SonarCloud

---

## 🎉 Résultat

À chaque `git push`, votre code sera automatiquement analysé par SonarCloud ! 🚀

**Vous verrez** :
- ✅ Bugs détectés
- ✅ Vulnérabilités de sécurité
- ✅ Code smells
- ✅ Couverture de code
- ✅ Duplications
- ✅ Quality Gate status

---

**Besoin d'aide ?** Consultez les guides détaillés dans les fichiers `SONARCLOUD-*.md`
