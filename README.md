# MatchFreelance - Plateforme Freelance Full-Stack Microservices

## Overview
Ce projet a ete developpe dans le cadre du programme PI a **Esprit School of Engineering - Tunisia** (annee universitaire 2025-2026).

MatchFreelance est une plateforme full-stack qui connecte freelancers et clients, gere les projets et la collaboration, et integre des modules metier complets: candidatures, contrats, entretiens, formations, evaluation/certification, competences (skills), et espace communautaire (blog/forum).

## Features
- Gestion des comptes utilisateurs, authentification et administration.
- Gestion du cycle de vie projet (creation, suivi, supervision, insights).
- Gestion des candidatures, contrats et jalons de paiement.
- Planification et suivi des entretiens.
- Gestion des formations, modules et inscriptions.
- Evaluation adaptive, certificats et recommandations.
- Gestion des competences, portfolio, CV et parcours intelligent.
- Fonctionnalites communautaires (posts, groupes, messages, moderation).

## Tech Stack
### Frontend
- Angular 21
- TypeScript
- Tailwind CSS

### Backend
- Java 17
- Spring Boot
- Spring Cloud Gateway
- Netflix Eureka (service discovery)
- Spring Cloud Config Server (optionnel)
- MySQL

## Architecture
Le frontend Angular envoie les requetes HTTP vers une API Gateway unique.  
La Gateway route ensuite vers les microservices via Eureka (`lb://SERVICE_NAME`).

Les services sont decouples, chacun avec son domaine metier et sa base de donnees, ce qui facilite la scalabilite, la maintenance et les deploiements independants.

### Vue simplifiee
```text
Frontend Angular (4200)
        |
        v
API Gateway (8050)
        |
        +--> USER
        +--> BLOG
        +--> PROJECT
        +--> FORMATION
        +--> EVALUATION
        +--> SKILL
        +--> CANDIDATURE
        +--> CONTRACT
        +--> INTERVIEW

Eureka Server (8761) : discovery
Config Server (8888) : config centralisee (optionnel)
```

## Microservices
### Services d'infrastructure
- **EurekaServer** (`8761`): registre de decouverte des services.
- **Gateway** (`8050`): point d'entree API, routage et fallback JSON.
- **ConfigServer** (`8888`, optionnel): centralisation des configurations.

### Services metier
- **User** (port variable, fallback code `8098`): authentification, profils, endpoints admin et gestion utilisateur.
- **Blog** (`8078`): forum, groupes, discussions, messages prives, moderation/toxicite, notifications.
- **Project** (`8084`): gestion des projets, supervision, estimation/insights, matching freelancer-projet.
- **Formation** (`8096`): formations, modules, inscriptions.
- **Evaluation** (`8083`): examens, passages, certificats, recommandations/ranking.
- **Skill** (`8079`): competences, CV, portfolio, bio freelancer, parcours intelligent.
- **Candidature** (`8091`): candidatures sur projets et suivi associe.
- **Contract** (`8088`): contrats, jalons, echanges lies au contrat.
- **Interview** (`8089`): orchestration des entretiens.

## Principales routes via la Gateway
- `/api/users/**`, `/api/auth/**`, `/api/admin/**` -> `USER`
- `/api/forums/**`, `/api/groups/**`, `/api/messages/private/**`, `/api/threads/**`, `/api/toxicity/**`, `/api/ai/**`, `/api/friends/**` -> `BLOG`
- `/api/projects/**` -> `PROJECT`
- `/api/formations/**`, `/api/modules/**`, `/api/inscriptions/**` -> `FORMATION`
- `/api/examens/**`, `/api/certificats/**` -> `EVALUATION`
- `/api/skills/**`, `/api/cv/**`, `/api/portfolio/**`, `/api/bio/**` -> `SKILL`
- `/api/candidatures/**` -> `CANDIDATURE`
- `/api/contracts/**` -> `CONTRACT`
- `/api/interviews/**` -> `INTERVIEW`
- `/api/evaluation-reports/**` -> service Python de reporting
- `/api/team-ai/**` -> service Team AI (rewrite vers `/api/**` cote service)

## Structure du projet
```text
.
├── frontend/                    # Application Angular
├── backend/
│   ├── EurekaServer/            # Service discovery
│   ├── Gateway/                 # API Gateway
│   ├── ConfigServer/            # Configuration centralisee (optionnel)
│   └── microservices/
│       ├── User/
│       ├── Blog/
│       ├── Project/
│       ├── Formation/
│       ├── Evaluation/
│       ├── Skill/
│       ├── Candidature/
│       ├── Contract/
│       ├── Interview/
│       └── evaluation-reports-py/
├── python/                      # Scripts/services Python complementaires
├── infra/                       # Kubernetes/Helm et infra
├── docs/                        # Documentation
└── scripts/                     # Scripts utilitaires
```

## Prerequis
- Java 17 (JDK 17) + `JAVA_HOME` correctement configure.
- Maven (ou `mvnw.cmd` dans les services).
- Node.js + npm.
- MySQL actif en local (`localhost:3306`, user `root` par defaut selon les configs).

## Getting Started (Local)
### 1) Cloner le projet
```bash
git clone <url-du-repo>
cd validation
```

### 2) Demarrer les services backend (ordre recommande)
1. EurekaServer (`backend/EurekaServer/EurekaServer`)
2. ConfigServer (`backend/ConfigServer`) - optionnel
3. Gateway (`backend/Gateway`)
4. Microservices metier (`backend/microservices/...`)

Exemple (PowerShell, un terminal par service):
```powershell
# Eureka
cd backend\EurekaServer\EurekaServer
.\mvnw.cmd spring-boot:run

# Config Server (optionnel)
cd backend\ConfigServer
.\mvnw.cmd spring-boot:run

# Gateway
cd backend\Gateway
.\mvnw.cmd spring-boot:run
```

Puis lancer chaque microservice selon besoin:
```powershell
cd backend\microservices\Project     ; .\mvnw.cmd spring-boot:run
cd backend\microservices\Formation   ; .\mvnw.cmd spring-boot:run
cd backend\microservices\Evaluation  ; .\mvnw.cmd spring-boot:run
cd backend\microservices\Skill       ; .\mvnw.cmd spring-boot:run
cd backend\microservices\Candidature ; .\mvnw.cmd spring-boot:run
cd backend\microservices\Contract    ; .\mvnw.cmd spring-boot:run
cd backend\microservices\Interview   ; .\mvnw.cmd spring-boot:run
cd backend\microservices\Blog        ; .\mvnw.cmd spring-boot:run
cd backend\microservices\User        ; .\mvnw.cmd spring-boot:run
```

### 3) Demarrer le frontend
```bash
cd frontend
npm install
npm start
```

Application web: `http://localhost:4200`

## Scripts utiles
Dans `backend/`, des scripts PowerShell sont disponibles:
- `demarrer-gateway-formation.ps1`: Eureka + Gateway + Formation.
- `demarrer-parcours-intelligent.ps1`: Eureka + Gateway + Skill.

## Verification rapide
- Eureka: `http://localhost:8761`
- Gateway: `http://localhost:8050/api`
- Frontend: `http://localhost:4200`

## Deploiement (recommande)
Le projet peut etre deploye via:
- Frontend: Vercel / Render / Railway / GitHub Pages (selon build et routing).
- Backend: Docker/Kubernetes (dossiers `infra/` et manifests disponibles).
- Ressources education: GitHub Education / cloud credits.

## Qualite et bonnes pratiques
- Architecture microservices avec separation par domaine metier.
- Centralisation optionnelle de config via Config Server.
- Discovery dynamique via Eureka.
- Gateway unique pour simplifier securite, routage et exposition API.

## Academic Context
- Institution: **Esprit School of Engineering - Tunisia**
- Program: PI (Projet d'Integration)
- Class: 3A
- Academic Year: 2025-2026
- Mention recommandee: "Developed at Esprit School of Engineering - Tunisia"

## Contributors
- MatchFreelance Team - Esprit School of Engineering
- Ajouter ici les noms, roles et contributions de chaque membre.

## Acknowledgments
Developed at **Esprit School of Engineering** as part of an academic project.
