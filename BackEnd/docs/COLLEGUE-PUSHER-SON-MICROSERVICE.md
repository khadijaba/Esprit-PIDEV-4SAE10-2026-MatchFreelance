# Comment pousser uniquement son microservice dans le dossier Microservices (Git)

Guide pour ta collègue : ajouter **seulement** son microservice dans **BackEnd/Microservices/** et le pousser sur le repo.

---

## 1. Cloner le repo (si elle ne l’a pas encore)

```bash
git clone https://github.com/khadijaba/PIDEV-4eme.git
cd PIDEV-4eme
```

---

## 2. Récupérer la dernière version

Avant de travailler, toujours faire un **pull** pour avoir les derniers changements (tes mises à jour, autres microservices, etc.) :

```bash
git pull origin master
```

*(Remplace `master` par `main` si c’est le nom de la branche sur le repo.)*

---

## 3. Où mettre son microservice

Elle place son projet **uniquement** ici :

```
PIDEV-4eme/
└── BackEnd/
    └── Microservices/
        ├── Skill/
        ├── Project/
        └── User/          ← elle crée ce dossier (ou le nom de son service)
```

- Soit elle **copie** tout son projet (pom.xml, src/, etc.) dans **BackEnd/Microservices/User/**.
- Soit elle **crée** un nouveau projet Spring Boot directement dans **BackEnd/Microservices/User/**.

Le dossier **User** doit contenir au minimum :
- `pom.xml`
- `src/main/java/`
- `src/main/resources/application.properties`

Et **pas** : `target/`, `node_modules/` (voir le `.gitignore` à la racine).

---

## 4. Configurer son microservice (même Eureka)

Dans **Microservices/User/src/main/resources/application.properties** (exemple pour "User") :

```properties
spring.application.name=USER
server.port=8085

eureka.client.service-url.defaultZone=http://192.168.1.8:8761/eureka/
eureka.instance.prefer-ip-address=true
```

- **USER** en MAJUSCULES (ou le nom de son service).
- **Port unique** (8085 si 8083 et 8084 sont déjà pris).
- **Même URL Eureka** que toi (ex. 192.168.1.8:8761).

---

## 5. N’ajouter que son dossier Microservices

Elle ne stage **que** son microservice (et éventuellement la doc si vous le demandez) :

```bash
# Uniquement son microservice
git add BackEnd/Microservices/User
```

*(Si elle a modifié la Gateway ou le registre, elle peut ajouter aussi :*
*`git add BackEnd/ApiGateway/...` et `git add BackEnd/docs/MICROSERVICES-REGISTRY.md`)*

Vérifier ce qui sera commité :

```bash
git status
```

Elle ne doit **pas** voir des dossiers des autres (Skill, Project) dans “Changes to be committed” **sauf** si elle les a modifiés. Pour ne pousser **que** son microservice, elle ne fait **que** :

```bash
git add BackEnd/Microservices/User
```

---

## 6. Commit et push

```bash
git commit -m "Add User microservice in BackEnd/Microservices"
git push origin master
```

Comme elle a fait un **pull** avant, elle pousse après les derniers commits. Si quelqu’un a poussé entre-temps, elle fait encore un **pull** (ou `git pull --rebase origin master`) puis **push**.

---

## En résumé (pour ta collègue)

| Étape | Commande / action |
|-------|-------------------|
| 1 | `git clone ...` puis `cd PIDEV-4eme` |
| 2 | `git pull origin master` |
| 3 | Copier/créer son projet dans **BackEnd/Microservices/User/** |
| 4 | Configurer `application.properties` (Eureka, port, nom USER) |
| 5 | `git add BackEnd/Microservices/User` (surtout pas tout le repo si elle veut pousser seulement son microservice) |
| 6 | `git commit -m "Add User microservice"` puis `git push origin master` |

Comme ça, elle pousse **seulement** son microservice dans le dossier **Microservices** du Git.
