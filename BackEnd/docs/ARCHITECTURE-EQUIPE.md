# Architecture microservices – Équipe (5 membres)

## 1. Principe : un seul Eureka pour toute l’équipe

- **Un seul serveur Eureka** : tous les microservices de tous les membres s’enregistrent sur la **même** instance Eureka.
- **Une seule API Gateway** : le frontend (ou les clients) parlent à la Gateway ; la Gateway utilise Eureka pour trouver les microservices (`lb://NOM_SERVICE`).
- **Chaque membre** développe **son** microservice (User, Skill, Project, etc.) ; tous pointent vers la même URL Eureka.

```
                    ┌─────────────────┐
                    │  Frontend /     │
                    │  Clients        │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  API Gateway    │  (port 8086)
                    │  (Eureka Client)│
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
     ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
     │ Eureka       │ │ Microservice │ │ Microservice │  ...
     │ Server       │ │ Skill        │ │ Project      │
     │ (port 8761)  │ │ (8083)       │ │ (8084)       │
     └──────────────┘ └──────┬───────┘ └──────┬───────┘
              ▲              │                 │
              │              └────────┬────────┘
              │                       │
              └───────────────────────┘
                    (tous s’enregistrent sur Eureka)
```

---

## 2. Organisation Git (recommandation)

### Option A : un seul dépôt (recommandé pour 5 personnes)

- **Un repo** contenant : Eureka, API Gateway, et **tous** les microservices (Skill, Project, User, etc.).
- Chaque membre a **son dossier** (ex. `Microservices/User`, `Microservices/Skill`).
- **Avantages** : une seule base de code, un seul Eureka/Gateway, tout le monde tire les mêmes configs.

Structure proposée :

```
BackEnd/
├── Eureka/
│   └── eureka/                 # Serveur Eureka (un seul pour l’équipe)
├── ApiGateway/                 # Gateway (une seule pour l’équipe)
├── Microservices/
│   ├── Skill/                  # Membre 1
│   ├── Project/                # Membre 2
│   ├── User/                   # Membre 3 (exemple)
│   └── ...                     # Autres membres
└── docs/
    └── ARCHITECTURE-EQUIPE.md  # Ce fichier
```

### Règles Git

1. **Branche principale** : `main` ou `develop` pour l’intégration.
2. **Branches de fonctionnalité** : `feature/nom-microservice-description` (ex. `feature/user-auth`).
3. **Qui modifie quoi** :
   - **Eureka** et **ApiGateway** : à modifier avec accord (ex. pour ajouter une route vers un nouveau microservice). Faire une PR / merge après revue.
   - **Microservice personnel** : chaque membre travaille dans son module, fait des commits et des PR vers `develop`.
4. **Avant de merger** : vérifier que le microservice démarre et s’enregistre bien sur Eureka (même URL Eureka pour tous).

---

## 3. URL Eureka partagée (pour que tous voient le même Eureka)

Tous les microservices et la Gateway doivent pointer vers **la même** URL Eureka.

### Qui fait tourner Eureka ?

- **En local (développement)** : un membre (ou chacun à tour de rôle) lance Eureka sur sa machine et partage son **IP** (ex. `192.168.1.10`).
- **En intégration / démo** : une machine ou un serveur dédié héberge Eureka (une IP ou un hostname connu de toute l’équipe).

### Configuration côté Eureka (celui qui lance le serveur)

Fichier : `Eureka/eureka/src/main/resources/application.properties`

```properties
# Host accessible par toute l’équipe (remplacer par l’IP ou le hostname de la machine qui lance Eureka)
eureka.instance.hostname=${EUREKA_HOST:localhost}
eureka.instance.prefer-ip-address=true
```

- En local seul : ne rien changer (`localhost`).
- Pour l’équipe : lancer Eureka avec `-DEUREKA_HOST=192.168.1.10` (ou mettre `192.168.1.10` dans le fichier), et ouvrir le port **8761** sur le firewall si besoin.

### Configuration côté microservices et Gateway

Dans **chaque** microservice et dans la **Gateway**, garder la **même** URL Eureka :

```properties
# Remplacer 192.168.1.10 par l’IP de la machine qui lance Eureka (ou localhost si tout est sur la même machine)
eureka.client.service-url.defaultZone=${EUREKA_URL:http://localhost:8761/eureka/}
eureka.instance.prefer-ip-address=true
```

- **En local** : `EUREKA_URL=http://localhost:8761/eureka/` (défaut).
- **En équipe** : tout le monde met la même URL, ex. `EUREKA_URL=http://192.168.1.10:8761/eureka/` (variable d’environnement ou `application.properties` partagé).

---

## 4. Convention des noms de services (Eureka)

Pour que la Gateway puisse faire `lb://NOM_SERVICE`, **spring.application.name** doit être **exact** et **cohérent** :

| Microservice | spring.application.name | Port suggéré |
|--------------|-------------------------|--------------|
| Eureka       | (serveur, pas de registration) | 8761 |
| API Gateway  | ApiGateway              | 8086 |
| Skill        | **SKILL**               | 8083 |
| Project      | **PROJECT**             | 8084 |
| User         | **USER**                | 8085 (ex.)  |

- Les noms en **MAJUSCULES** (SKILL, PROJECT, USER) sont une convention courante pour Eureka (serviceId).
- La Gateway utilise déjà `lb://SKILL` et `lb://PROJECT`. Pour un nouveau microservice (ex. User), il faudra ajouter une route dans la Gateway avec `lb://USER`.

---

## 5. Ajouter un nouveau microservice (nouveau membre)

1. **Créer le module** sous `Microservices/NomDuService/` (même structure que Skill/Project).
2. **Dans le microservice** :
   - `spring.application.name=USER` (ou le nom choisi, en MAJUSCULES pour Eureka).
   - Port unique : ex. `server.port=8085`.
   - `eureka.client.service-url.defaultZone=${EUREKA_URL:http://localhost:8761/eureka/}` (même URL que les autres).
   - Dépendance Eureka Client (comme dans Skill/Project).
3. **Dans l’API Gateway** :
   - Ajouter une route vers `lb://USER` (ou le nom choisi) pour les chemins dédiés (ex. `/api/users/**`).
4. **Documenter** dans ce fichier (ou dans un `MICROSERVICES-REGISTRY.md`) : nom du service, port, responsable.

---

## 6. Ordre de démarrage (intégration)

1. Démarrer **Eureka** (port 8761).
2. Démarrer **API Gateway** (8086).
3. Démarrer **chaque microservice** (Skill 8083, Project 8084, User 8085, etc.).

Vérification : ouvrir `http://<EUREKA_HOST>:8761` ; tous les services doivent apparaître dans le dashboard Eureka.

---

## 7. Résumé rapide

- **Un repo** : Eureka + Gateway + tous les microservices.
- **Une seule URL Eureka** : `EUREKA_URL` (ou `defaultZone`) identique pour tous.
- **Convention** : `spring.application.name` en MAJUSCULES (SKILL, PROJECT, USER) et ports dédiés.
- **Git** : branches par fonctionnalité, PR pour Eureka/Gateway et pour chaque microservice.
- **Nouveau microservice** : même config Eureka, port unique, ajout de la route dans la Gateway et mise à jour de ce doc (ou du registre).

Si vous voulez, on peut ajouter un fichier `MICROSERVICES-REGISTRY.md` qui liste tous les services (nom, port, dépôt Git, responsable) pour que l’équipe ait une seule source de vérité.
