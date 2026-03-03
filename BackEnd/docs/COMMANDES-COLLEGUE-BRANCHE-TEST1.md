# Commandes exactes : mettre le microservice de la collègue dans Git (BackEnd/Microservices, branche test1)

À exécuter **sur le PC de ta collègue**. Elle remplace **CHEMIN_SON_MICROSERVICE** par le chemin réel de son dossier (ex. `C:\Users\Colleague\Desktop\UserMicroservice`).

---

## Étape 1 : Ouvrir un terminal

- **Windows** : CMD ou PowerShell (clic droit Démarrer → Terminal / Invite de commandes).
- Se placer dans un dossier de travail, par exemple :
  ```bash
  cd C:\Users\COLLEGUE\Documents
  ```
  *(Elle remplace COLLEGUE par son nom d’utilisateur Windows.)*

---

## Étape 2 : Cloner le repo (si elle ne l’a pas déjà)

Si le dossier **PIDEV-4eme** n’existe pas encore :

```bash
git clone https://github.com/khadijaba/PIDEV-4eme.git
cd PIDEV-4eme
```

Si elle a **déjà** cloné le repo :

```bash
cd PIDEV-4eme
```

---

## Étape 3 : Passer sur la branche test1

Créer la branche **test1** si elle n’existe pas, sinon la récupérer et s’y mettre :

```bash
git fetch origin
git checkout test1
```

Si la branche **test1** n’existe pas encore (création locale) :

```bash
git checkout -b test1
```

*(Plus tard, pour pousser la branche sur GitHub : `git push -u origin test1`.)*

---

## Étape 4 : Récupérer les derniers changements

```bash
git pull origin test1
```

*(Si la branche n’existe pas encore sur GitHub, cette commande peut afficher une erreur ; dans ce cas, ignorer et continuer.)*

---

## Étape 5 : Copier son microservice dans BackEnd/Microservices

Elle doit avoir **un seul dossier** qui contient son microservice (avec `pom.xml`, `src/`, etc.).  
On appelle ce dossier **User** ici (elle peut le remplacer par le nom de son service : Payment, Notification, etc.).

**PowerShell (sur le PC de la collègue) :**

```powershell
# Créer le dossier Microservices s'il n'existe pas
New-Item -ItemType Directory -Path "BackEnd\Microservices" -Force

# Copier tout le contenu de son microservice dans BackEnd/Microservices/User
# REMPLACER CHEMIN_SON_MICROSERVICE par le vrai chemin, ex: C:\Users\Colleague\Desktop\UserService
Copy-Item -Path "CHEMIN_SON_MICROSERVICE\*" -Destination "BackEnd\Microservices\User" -Recurse -Force
```

**Exemple concret** (si son microservice est dans `C:\Users\Marie\Desktop\UserService`) :

```powershell
New-Item -ItemType Directory -Path "BackEnd\Microservices\User" -Force
Copy-Item -Path "C:\Users\Marie\Desktop\UserService\*" -Destination "BackEnd\Microservices\User" -Recurse -Force
```

**Alternative avec xcopy (CMD) :**

```bash
mkdir BackEnd\Microservices\User 2>nul
xcopy "CHEMIN_SON_MICROSERVICE\*" "BackEnd\Microservices\User\" /E /I /H /Y
```

**Exemple :**

```bash
xcopy "C:\Users\Marie\Desktop\UserService\*" "BackEnd\Microservices\User\" /E /I /H /Y
```

---

## Étape 6 : Vérifier que le dossier est bien là

```bash
dir BackEnd\Microservices\User
```

Elle doit voir au moins : `pom.xml`, dossier `src`, etc.

---

## Étape 7 : Ne pas versionner target / node_modules

À la racine de **PIDEV-4eme**, le fichier **.gitignore** doit contenir au moins :

- `target/`
- `**/target/`
- `node_modules/`

Si le repo n’a pas encore de `.gitignore` à la racine, elle peut le créer. Sinon, ne rien ajouter et continuer.

---

## Étape 8 : Ajouter uniquement le microservice dans Git

```bash
git add BackEnd/Microservices/User
```

Vérifier :

```bash
git status
```

Elle doit voir **BackEnd/Microservices/User** dans “Changes to be committed”.

---

## Étape 9 : Commit

```bash
git commit -m "Add User microservice in BackEnd/Microservices (branch test1)"
```

---

## Étape 10 : Pousser sur la branche test1

Si la branche **test1** n’a jamais été poussée :

```bash
git push -u origin test1
```

Sinon :

```bash
git push origin test1
```

---

## Récapitulatif des commandes (à adapter)

À exécuter **dans l’ordre** sur le PC de la collègue (remplacer les chemins) :

```bash
cd C:\Users\COLLEGUE\Documents
git clone https://github.com/khadijaba/PIDEV-4eme.git
cd PIDEV-4eme
git fetch origin
git checkout test1
git pull origin test1
```

Puis **PowerShell** pour la copie :

```powershell
New-Item -ItemType Directory -Path "BackEnd\Microservices\User" -Force
Copy-Item -Path "C:\Users\COLLEGUE\Desktop\MonMicroservice\*" -Destination "BackEnd\Microservices\User" -Recurse -Force
```

Puis à nouveau **Git** :

```bash
git add BackEnd/Microservices/User
git status
git commit -m "Add User microservice in BackEnd/Microservices (branch test1)"
git push -u origin test1
```

---

## En cas d’erreur au push

- **“failed to push” / “rejected”** : quelqu’un a poussé sur test1 avant elle. Elle fait :
  ```bash
  git pull origin test1
  ```
  puis :
  ```bash
  git push origin test1
  ```
- **Authentification** : GitHub peut demander login + mot de passe (ou token). Utiliser un **Personal Access Token** si l’authentification par mot de passe est désactivée.

Après ça, son microservice est dans **BackEnd/Microservices/User** sur la branche **test1** dans le repo Git.
