# Guide pour ta collègue : ajouter son microservice sur le repo (même Eureka)

Objectif : ta collègue pousse **son** microservice dans **ton** repo Git pour que tout le monde utilise **un seul Eureka** et une seule API Gateway.

---

## Étape 1 : Cloner le repo et se placer sur la bonne branche

Ta collègue clone le repo (une seule fois) :

```bash
git clone https://github.com/khadijaba/PIDEV-4eme.git
cd PIDEV-4eme
git checkout master
```

Puis elle fait un **pull** pour avoir la dernière version avant de travailler :

```bash
git pull origin master
```

---

## Étape 2 : Où mettre son microservice

Elle place son microservice dans le dossier **BackEnd** :

```
PIDEV-4eme/
└── BackEnd/
    └── Microservices/
        ├── Skill/      (toi)
        ├── Project/    (autre collègue)
        └── User/       ← elle crée ce dossier (ou le nom de son service)
```

- Soit elle **crée** un nouveau projet Spring Boot dans `BackEnd/Microservices/User/` (même structure que Skill ou Project).
- Soit elle **copie** son projet existant dans `BackEnd/Microservices/User/` (en gardant la structure : `pom.xml`, `src/`, etc.).

---

## Étape 3 : Configurer le microservice pour le même Eureka

Dans son microservice, elle doit avoir **exactement la même URL Eureka** que les autres.

Fichier : **`Microservices/User/src/main/resources/application.properties`** (exemple pour un service "User") :

```properties
# Nom en MAJUSCULES (obligatoire pour la Gateway : lb://USER)
spring.application.name=USER

# Port unique (pas déjà utilisé !)
server.port=8085

# Même Eureka que toute l'équipe
eureka.client.service-url.defaultZone=${EUREKA_URL:http://localhost:8761/eureka/}
eureka.instance.prefer-ip-address=true
```

Points importants :
- **spring.application.name** : en **MAJUSCULES** (ex. `USER`, `PAYMENT`) pour que la Gateway puisse utiliser `lb://USER`.
- **server.port** : un port **libre** (voir le tableau dans `MICROSERVICES-REGISTRY.md` : 8083 Skill, 8084 Project, donc 8085 pour User par exemple).
- **eureka.client.service-url.defaultZone** : **identique** à Skill et Project (`${EUREKA_URL:http://localhost:8761/eureka/}`).

Elle doit aussi avoir la **dépendance Eureka Client** dans son **pom.xml** (comme dans Skill/Project) :

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

Et le **parent** ou **dependencyManagement** Spring Cloud (comme dans les autres microservices du repo).

---

## Étape 4 : Ajouter la route dans l’API Gateway

Pour que le frontend puisse appeler son service via la Gateway, il faut ajouter une **route** dans le projet **ApiGateway**.

Fichier à modifier : **`BackEnd/ApiGateway/src/main/java/esprit/apigateway/ApiGatewayApplication.java`**.

1. Ajouter une variable (comme pour Skill et Project) :

```java
@Value("${user.service.uri:lb://USER}")
private String userServiceUri;
```

2. Dans la méthode `gatewayRoutes`, ajouter une route **avant** le `.build()` :

```java
// USER SERVICE (exemple)
.route("user-service", r -> r
        .path("/api/users/**")
        .filters(f -> f.stripPrefix(1))
        .uri(userServiceUri))

.build();
```

- **path** : l’URL exposée au frontend (ex. `/api/users/**`).
- **uri** : `userServiceUri` = `lb://USER` (même nom que `spring.application.name` du microservice).

Qui fait cette modification ?
- **Option A** : Ta collègue la fait elle-même dans son commit (elle modifie le fichier Gateway + ajoute son module).
- **Option B** : Toi tu l’ajoutes après avoir récupéré son microservice, puis vous faites un commit.

L’important : une seule Gateway, une seule fois la route pour `USER`.

---

## Étape 5 : Mettre à jour le registre des microservices

Fichier : **`BackEnd/docs/MICROSERVICES-REGISTRY.md`**.

Ajouter une ligne dans le tableau pour le nouveau service, par exemple :

| User | USER | 8085 | Microservices/User | /api/users |

Comme ça, tout le monde sait quel port et quel nom Eureka utiliser.

---

## Étape 6 : Commit et push (ta collègue)

Ta collègue fait un commit avec **son microservice** + **les changements Gateway** + **le registre** :

```bash
git add BackEnd/Microservices/User
git add BackEnd/ApiGateway/src/main/java/esprit/apigateway/ApiGatewayApplication.java
git add BackEnd/docs/MICROSERVICES-REGISTRY.md
git status
git commit -m "Add User microservice, register on same Eureka, add Gateway route"
git push origin master
```

Si vous travaillez avec des **branches** (ex. `develop`) ou des **Pull Requests** :

- Elle crée une branche : `git checkout -b feature/user-microservice`
- Elle fait le même `git add` et `git commit`
- Elle pousse : `git push -u origin feature/user-microservice`
- Puis elle ouvre une **Pull Request** vers `master` (ou `main`). Tu revues et tu merges.

---

## Résumé pour ta collègue

1. **Clone** le repo, **pull** la dernière version.
2. **Ajoute** son microservice sous `BackEnd/Microservices/NomService/` (ex. User).
3. **Configure** : `spring.application.name=NOM` (MAJUSCULES), port unique, **même** `eureka.client.service-url.defaultZone` que les autres.
4. **Ajoute** la route dans **ApiGateway** pour `/api/...` → `lb://NOM`.
5. **Met à jour** `docs/MICROSERVICES-REGISTRY.md`.
6. **Commit** et **push** (ou branche + PR).

Comme ça, son microservice est dans **ton** dossier Git et tout le monde utilise **un seul Eureka** et **une seule Gateway**.
