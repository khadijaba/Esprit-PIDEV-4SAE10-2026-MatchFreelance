# Backend — Plateforme Freelancers & Project Matching

Backend (Gateway + microservices). Le frontend Angular est à la racine du projet : **`frontend`**.

## Architecture

| Microservice | Port | Rôle |
|--------------|------|------|
| **EurekaServer** | 8761 | Découverte des services |
| **Gateway** | 8050 | Point d'entrée unique, routage vers les microservices |
| **User** | 8085 | Auth (JWT), utilisateurs (Admin, Freelancer, Client) |
| **Formation** | 8081 | Formations et inscriptions freelancers |
| **Evaluation** | 8083 | Examens, passages, certificats |
| **Skill** | 8086 | Parcours Intelligent : compétences, gaps, proposition formations |

## Structure

```
backend/
├── EurekaServer/        # Discovery (port 8761)
├── Gateway/             # API Gateway (port 8050)
└── microservices/
    ├── User/            # Auth, rôles (port 8085)
    ├── Formation/       # Formations, inscriptions (port 8081)
    ├── Evaluation/      # Examens, certificats (port 8083)
    └── Skill/           # Parcours Intelligent (port 8086)
```

## Démarrer l'ensemble

1. **Eureka** : lancer `EurekaServer` (8761).
2. **Gateway** : lancer `Gateway` (8050).
3. **Microservices** : lancer **User** (8085), **Formation** (8081), **Evaluation** (8083), **Skill** (8086).
4. **Frontend** : dans `frontend`, exécuter `npm install` puis `npm start` (4200).

Le front appelle `/api/*` ; le proxy Angular redirige vers la Gateway (8050), qui route vers les microservices.

### Démarrer pour Formations + Modules (Eureka + Gateway + Formation)

Pour pouvoir **ajouter des modules** depuis l’admin (et éviter « Connexion impossible ») :

Depuis le dossier **`backend`**, exécuter :

```powershell
.\demarrer-gateway-formation.ps1
```

Cela ouvre 3 fenêtres : **Eureka** (8761), **Gateway** (8050), **Formation** (8081). Attendre que chaque service affiche `Started ...`, puis lancer le frontend (`ng serve`) et aller sur **Admin → Formations → détail d’une formation → Ajouter un module**.

### Démarrer uniquement le Parcours Intelligent (Eureka + Gateway + Skill)

Depuis `backend`, exécuter le script PowerShell :

```powershell
.\demarrer-parcours-intelligent.ps1
```

Cela ouvre 3 fenêtres (Eureka, Gateway, Skill). **Le microservice Skill nécessite JDK 17** (le script définit `JAVA_HOME` vers `C:\Users\benay\.jdks\ms-17.0.18`). Si votre JDK 17 est ailleurs, éditez la variable `$jdk17` dans le script.

À la main (dans 3 terminaux) :

```powershell
# Terminal 1 - Eureka
cd backend\EurekaServer\EurekaServer
.\mvnw.cmd spring-boot:run

# Terminal 2 - Gateway
cd backend\Gateway
.\mvnw.cmd spring-boot:run

# Terminal 3 - Skill (JDK 17 requis)
$env:JAVA_HOME = "C:\Users\benay\.jdks\ms-17.0.18"
cd backend\microservices\Skill
.\mvnw.cmd spring-boot:run
```

## Routes Gateway → Microservices

| Préfixe | Service | Exemples |
|---------|---------|----------|
| `/api/users` | USER | `POST /api/users/auth/register`, `POST /api/users/auth/login`, `GET /api/users/me` |
| `/api/formations` | FORMATION | `GET/POST/PUT/DELETE /api/formations`, `/api/formations/{id}` |
| `/api/modules` | FORMATION | `GET /api/modules/formation/{id}`, `POST /api/modules` (modules courts) |
| `/api/inscriptions` | FORMATION | `/api/inscriptions/...` |
| `/api/examens` | EVALUATION | `/api/examens`, `/api/examens/**` |
| `/api/certificats` | EVALUATION | `/api/certificats`, `/api/certificats/**` |
| `/api/skills` | SKILL | `/api/skills`, `/api/skills/parcours/intelligent?freelancerId=...` |

## Microservice User

- **Rôle** : Authentification JWT, inscription, connexion, rôles (ADMIN, FREELANCER, CLIENT).
- **Endpoints** : `POST /api/users/auth/register`, `POST /api/users/auth/login`, `GET /api/users/me`, `GET /api/users` (avec token).
- **Base de données** : MySQL `user` (voir `microservices/User/src/main/resources/application.properties`).

## Microservice Formation

- **Rôle** : Gestion des formations et inscriptions (freelancers).
- **Base de données** : MySQL `FormationDB`.

## Microservice Evaluation

- **Contexte** : Examens, passages, certificats sous `/api/examens` et `/api/certificats`.
- **Base de données** : MySQL `EvaluationDB`.

## Microservice Skill (Parcours Intelligent)

- **Rôle** : Parcours personnalisé, analyse des compétences actuelles, détection des gaps (domaines à renforcer), proposition de formations ciblées (appel au microservice Formation).
- **Endpoints** : `GET /api/skills/freelancer/{id}`, `GET /api/skills/parcours/intelligent?freelancerId=1`, CRUD `/api/skills`.
- **Base de données** : MySQL `skill`.
