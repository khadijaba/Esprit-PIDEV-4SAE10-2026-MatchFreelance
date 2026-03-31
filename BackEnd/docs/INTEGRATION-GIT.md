# Intégration BackEnd + Frontend dans le repo GitHub

Repo : **https://github.com/khadijaba/PIDEV-4eme.git**

## Prérequis

- Git installé
- Accès au repo (clone en HTTPS ou SSH)

---

## Option 1 : Tu pars de zéro (nouveau clone)

### 1. Cloner le repo et entrer dedans

```bash
git clone https://github.com/khadijaba/PIDEV-4eme.git
cd PIDEV-4eme
```

### 2. Créer la branche master (si elle n’existe pas) ou l’utiliser

```bash
# Si la branche master n'existe pas encore
git checkout -b master

# Ou si elle existe déjà (après un premier push)
git checkout master
```

*(Si le repo a déjà une branche `main`, tu peux faire `git checkout main` et utiliser `main` à la place de `master`.)*

### 3. Copier le BackEnd dans le repo

**Sous Windows (PowerShell)** – à adapter selon tes vrais chemins :

```powershell
# Supprimer le dossier BackEnd existant s'il est vide ou à remplacer
Remove-Item -Recurse -Force BackEnd -ErrorAction SilentlyContinue

# Créer BackEnd et y copier le contenu de ton BackEnd (Eureka, ApiGateway, Microservices, docs)
# Remplace "C:\Users\emnad\Backend\BackEnd\BackEnd" par le chemin de ton dossier BackEnd
xcopy "C:\Users\emnad\Backend\BackEnd\BackEnd\*" "BackEnd\" /E /I /H /Y
```

**Ou à la main :** copie tout le contenu de ton dossier BackEnd (Eureka, ApiGateway, Microservices, docs) dans `PIDEV-4eme/BackEnd/`.

### 4. Copier le Frontend dans le repo

```powershell
# Créer Frontend et y copier ton projet Angular
# Remplace "d:\telechqrgement\frontendd\frontend" par le chemin de ton dossier frontend
xcopy "d:\telechqrgement\frontendd\frontend\*" "Frontend\" /E /I /H /Y
```

**Exclure** `node_modules` du Frontend (ne pas le pousser sur Git) :

- Soit ne pas copier le dossier `node_modules` dans `Frontend/`.
- Soit s’assurer qu’un `.gitignore` à la racine du repo ou dans `Frontend/` contient `node_modules/`.

### 5. Fichier .gitignore à la racine du repo

Crée (ou complète) **PIDEV-4eme/.gitignore** à la racine :

```gitignore
# Frontend
Frontend/node_modules/
Frontend/dist/
Frontend/.angular/

# Backend - build
**/target/
**/build/

# IDE
.idea/
*.iml
.vscode/
*.class

# Env / local
*.log
.env
.DS_Store
```

### 6. Commit et push

```bash
git add BackEnd Frontend .gitignore
git status
git commit -m "Add BackEnd and Frontend structure (Eureka, Gateway, Skill, Project, Angular)"
git push -u origin master
```

*(Remplace `master` par `main` si c’est le nom de la branche par défaut.)*

---

## Option 2 : Le repo est déjà cloné (tu as déjà PIDEV-4eme)

```bash
cd PIDEV-4eme
git checkout master
# ou: git checkout -b master
```

Ensuite exécute les étapes 3, 4, 5 et 6 ci-dessus (copie BackEnd, copie Frontend, .gitignore, commit, push).

---

## Option 3 : Script PowerShell tout-en-un

Enregistre le script ci-dessous dans un fichier, **modifie les deux chemins** au début, puis exécute-le **depuis le dossier PIDEV-4eme** (après `git clone` et `cd PIDEV-4eme`).

```powershell
# ===== À ADAPTER =====
$cheminBackEnd = "C:\Users\emnad\Backend\BackEnd\BackEnd"
$cheminFrontend = "d:\telechqrgement\frontendd\frontend"
# =====================

if (-not (Test-Path $cheminBackEnd)) { Write-Error "BackEnd introuvable: $cheminBackEnd"; exit 1 }
if (-not (Test-Path $cheminFrontend)) { Write-Error "Frontend introuvable: $cheminFrontend"; exit 1 }

Remove-Item -Recurse -Force BackEnd -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force Frontend -ErrorAction SilentlyContinue

New-Item -ItemType Directory -Path BackEnd -Force
New-Item -ItemType Directory -Path Frontend -Force

Copy-Item -Path "$cheminBackEnd\*" -Destination "BackEnd\" -Recurse -Force
Copy-Item -Path "$cheminFrontend\*" -Destination "Frontend\" -Recurse -Force

# Exclure node_modules du Frontend (optionnel si .gitignore le fait)
Remove-Item -Recurse -Force "Frontend\node_modules" -ErrorAction SilentlyContinue

Write-Host "BackEnd et Frontend copies. Fais: git add BackEnd Frontend && git commit && git push"
```

---

## Résumé

1. **Clone** : `git clone https://github.com/khadijaba/PIDEV-4eme.git`
2. **Branche** : `git checkout master` (ou `git checkout -b master`)
3. **Copier** BackEnd et Frontend dans le repo (structure ci-dessus).
4. **.gitignore** : ignorer `node_modules`, `target`, etc.
5. **Commit + push** : `git add`, `git commit`, `git push origin master`

Après ça, le repo aura la branche **master** avec **BackEnd** et **Frontend** dans la même structure que décrite dans la doc.
