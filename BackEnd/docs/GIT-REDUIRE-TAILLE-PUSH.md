# Réduire la taille du repo et faire passer le push

Si le push échoue (dossier trop gros, timeout, erreur), c’est souvent à cause de **node_modules** (Frontend) ou **target/** (Backend) qui ne doivent **pas** être versionnés.

## 1. Fichier .gitignore à la racine du repo

À la **racine** de ton repo (PIDEV-4eme, là où il y a BackEnd et Frontend), crée ou remplace le fichier **`.gitignore`** par ce contenu :

```gitignore
# Frontend
node_modules/
Frontend/node_modules/
**/node_modules/
Frontend/dist/
Frontend/.angular/

# Backend
target/
**/target/
*.jar
!**/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/

# Divers
*.log
.DS_Store
.env
```

Ainsi Git **ignore** ces dossiers/fichiers et ils ne seront plus poussés.

---

## 2. Si tu n’as pas encore push (premier push)

1. Vérifie que **node_modules** et **target** sont bien ignorés :
   ```bash
   git status
   ```
   Tu ne dois **pas** voir `node_modules/` ni `**/target/` dans la liste des fichiers à ajouter.

2. Si tu les vois encore, ils sont peut‑être déjà ajoutés. Retire-les du suivi :
   ```bash
   git rm -r --cached Frontend/node_modules 2>nul
   git rm -r --cached **/target 2>nul
   git rm -r --cached BackEnd/**/target 2>nul
   ```
   (Adapte les chemins si ton repo s’appelle autrement : BackEnd, Frontend, etc.)

3. Commit et push :
   ```bash
   git add .gitignore
   git add .
   git commit -m "Add BackEnd and Frontend, ignore node_modules and target"
   git push -u origin master
   ```

---

## 3. Si tu as déjà push des gros dossiers (push qui ne marche plus)

Ils sont dans l’historique Git, il faut les enlever du suivi et refaire un commit :

```bash
# Retirer du suivi (les fichiers restent sur le disque, Git les ignore après)
git rm -r --cached Frontend/node_modules
git rm -r --cached BackEnd/Eureka/eureka/target
git rm -r --cached BackEnd/ApiGateway/target
git rm -r --cached BackEnd/Microservices/Skill/target
git rm -r --cached BackEnd/Microservices/Project/target
# Répéter pour chaque module qui a un target/

git add .gitignore
git commit -m "Stop tracking node_modules and target folders"
git push origin master
```

Après ça, les prochains push seront beaucoup plus légers.

---

## 4. Vérifier la taille avant de push

```bash
# Taille du dossier .git (historique)
du -sh .git

# Fichiers qui seraient commités (sans les ignorés)
git add -n .
```

---

## 5. En résumé

| À ne pas pousser | Raison |
|------------------|--------|
| `node_modules/`  | Très gros, se recrée avec `npm install` |
| `target/`        | Compilation Maven, se recrée avec `mvn compile` |
| `dist/`, `.angular/` | Build Angular |
| `.idea/`, `*.iml` | Fichiers IDE locaux |

Avec un **.gitignore** correct et éventuellement un `git rm --cached` sur ces dossiers, la taille du repo baisse fortement et le push peut à nouveau fonctionner.
