# 🔍 Guide de Configuration SonarCloud pour User Microservice

## 📋 Prérequis

1. Un compte SonarCloud (gratuit pour les projets open source)
2. Accès administrateur au repository GitHub
3. Java 17 installé localement
4. Maven 3.6+ installé

## 🚀 Étapes de Configuration

### 1. Créer un Compte SonarCloud

1. Allez sur [SonarCloud.io](https://sonarcloud.io)
2. Connectez-vous avec votre compte GitHub
3. Autorisez SonarCloud à accéder à votre organisation GitHub

### 2. Créer un Nouveau Projet

1. Dans SonarCloud, cliquez sur **"+"** → **"Analyze new project"**
2. Sélectionnez votre repository
3. Choisissez **"With GitHub Actions"** comme méthode d'analyse
4. Notez les informations suivantes :
   - **Organization Key** : `your-org`
   - **Project Key** : `user-microservice`

### 3. Configurer les Secrets GitHub

Ajoutez le secret suivant dans votre repository GitHub :

1. Allez dans **Settings** → **Secrets and variables** → **Actions**
2. Cliquez sur **"New repository secret"**
3. Ajoutez :
   - **Name** : `SONAR_TOKEN`
   - **Value** : Le token généré par SonarCloud (disponible dans Account → Security)

### 4. Mettre à Jour les Fichiers de Configuration

#### A. Mettre à jour `sonar-project.properties`

Remplacez `your-org` par votre organisation SonarCloud :

```properties
sonar.organization=your-org
sonar.projectKey=user-microservice
```

#### B. Mettre à jour `.github/workflows/sonarcloud.yml`

Remplacez `your-org` par votre organisation dans le workflow :

```yaml
-Dsonar.organization=your-org
```

#### C. Mettre à jour `pom.xml`

Remplacez `your-org` dans les propriétés :

```xml
<sonar.organization>your-org</sonar.organization>
```

### 5. Tester l'Analyse Localement (Optionnel)

Avant de pousser, vous pouvez tester l'analyse localement :

```bash
cd User

# Compiler et exécuter les tests
mvn clean verify

# Lancer l'analyse SonarCloud
mvn sonar:sonar \
  -Dsonar.projectKey=user-microservice \
  -Dsonar.organization=your-org \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token=YOUR_SONAR_TOKEN
```

### 6. Pousser les Modifications

```bash
git add .
git commit -m "feat: Add SonarCloud integration"
git push origin main
```

Le workflow GitHub Actions se déclenchera automatiquement et enverra les résultats à SonarCloud.

## 📊 Accéder aux Résultats

1. Allez sur [SonarCloud.io](https://sonarcloud.io)
2. Sélectionnez votre projet **"User Microservice"**
3. Consultez :
   - **Overview** : Vue d'ensemble de la qualité du code
   - **Issues** : Bugs, vulnérabilités, code smells
   - **Security Hotspots** : Points sensibles de sécurité
   - **Measures** : Métriques détaillées
   - **Code** : Navigation dans le code avec annotations

## 🎯 Quality Gates

Le projet est configuré avec les Quality Gates par défaut de SonarCloud :

- **Coverage** : > 80% (recommandé)
- **Duplications** : < 3%
- **Maintainability Rating** : A
- **Reliability Rating** : A
- **Security Rating** : A

Vous pouvez personnaliser ces seuils dans SonarCloud → **Quality Gates**.

## 🔧 Configuration Avancée

### Exclure des Fichiers de l'Analyse

Modifiez `sonar-project.properties` :

```properties
sonar.exclusions=\
  **/config/**,\
  **/dto/**,\
  **/MySpecificFile.java
```

### Désactiver des Règles Spécifiques

Dans SonarCloud → **Quality Profiles** → **Java** :
1. Créez un nouveau profil ou copiez le profil par défaut
2. Désactivez les règles non pertinentes
3. Assignez ce profil à votre projet

### Analyser les Pull Requests

Le workflow est déjà configuré pour analyser les PRs automatiquement. Les résultats apparaîtront :
- Dans l'onglet **Checks** de la PR
- Comme commentaire automatique sur la PR
- Dans SonarCloud sous **Pull Requests**

## 📈 Badges

Ajoutez des badges SonarCloud à votre README :

```markdown
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=alert_status)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=coverage)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=bugs)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=code_smells)](https://sonarcloud.io/dashboard?id=user-microservice)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=user-microservice&metric=security_rating)](https://sonarcloud.io/dashboard?id=user-microservice)
```

## 🐛 Dépannage

### Erreur : "Project not found"

- Vérifiez que `sonar.projectKey` et `sonar.organization` sont corrects
- Assurez-vous que le projet existe dans SonarCloud

### Erreur : "Insufficient privileges"

- Vérifiez que le `SONAR_TOKEN` est valide
- Assurez-vous que le token a les permissions nécessaires

### Pas de Coverage

- Vérifiez que JaCoCo génère bien le rapport XML
- Vérifiez le chemin dans `sonar.coverage.jacoco.xmlReportPaths`
- Exécutez `mvn clean verify` avant `mvn sonar:sonar`

### Timeout du Quality Gate

- Augmentez le timeout dans le workflow :
  ```yaml
  timeout-minutes: 10
  ```

## 📚 Ressources

- [Documentation SonarCloud](https://docs.sonarcloud.io/)
- [SonarCloud GitHub Action](https://github.com/SonarSource/sonarcloud-github-action)
- [Règles Java SonarCloud](https://rules.sonarsource.com/java/)
- [Quality Gates](https://docs.sonarcloud.io/improving/quality-gates/)

## 🎉 Prochaines Étapes

1. ✅ Configurer SonarCloud
2. ✅ Pousser le code et vérifier l'analyse
3. 📊 Consulter les résultats
4. 🔧 Corriger les issues critiques
5. 📈 Améliorer progressivement la qualité du code
6. 🏆 Atteindre le Quality Gate

---

**Note** : Pour les projets privés, SonarCloud nécessite un abonnement payant. Les projets open source sont gratuits.
