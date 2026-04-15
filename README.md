# MatchFreelance

**Plateforme de mise en relation Freelancers / Clients et gestion de projets** — Projet académique Esprit School of Engineering – Tunisia.

---

## Overview

MatchFreelance est une application full-stack en **architecture microservices** permettant de gérer des freelancers, des clients, des projets, des formations, des compétences (skills), des examens et certificats. Le frontend Angular communique avec les microservices via une **API Gateway** et une **découverte de services (Eureka)**.

---

## Features

- **CRUD complet** : Projets, Formations, Compétences (Skills), Examens, Inscriptions, Modules.
- **Pagination** : Liste des projets avec pagination fonctionnelle (taille de page configurable, navigation Début / Précédent / Suivant / Fin).
- **Contrôle de saisie** : Tous les formulaires (projet, formation, compétence, examen, etc.) ont des validations (required, minLength, min, max, pattern) et affichent des messages d’erreur clairs.
- **Interfaces personnalisées et ergonomiques** : Dashboard admin, espaces Freelancer / Client, listes avec filtres et recherche, toasts, mise en page responsive (Tailwind CSS).
- **Fonctionnalités avancées** (exemples) :
  - **Parcours intelligent** (microservice Skill) : analyse des compétences du freelancer, détection des gaps, proposition de formations ciblées.
  - **Examens et certificats** : passer un examen, génération de certificat.
  - **Config Server** : centralisation de la configuration des microservices (Spring Cloud Config).

---

## Tech Stack

| Couche | Technologies |
|--------|--------------|
| **Frontend** | Angular 21, TypeScript, Tailwind CSS, standalone components |
| **Backend** | Java 17, Spring Boot 3.x / 4.x, Spring Cloud (Eureka, Gateway, Config Server) |
| **Microservices** | Formation, Evaluation (examens, certificats), Skill (compétences, parcours intelligent), Project |
| **Infrastructure** | Netflix Eureka (découverte), Spring Cloud Gateway (routage), MySQL (bases par service) |
| **Outils** | Maven, Node.js / npm |

---

## Architecture

```
                    ┌─────────────────┐
                    │   Angular SPA   │  (port 4200)
                    │   (Frontend)    │
                    └────────┬────────┘
                             │ /api/*
                    ┌────────▼────────┐
                    │  API Gateway    │  (port 8050)
                    │  (Spring Cloud) │
                    └────────┬────────┘
                             │ lb://SERVICE_ID
              ┌──────────────┼──────────────┬──────────────┬──────────────┐
              ▼              ▼              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
        │  Eureka  │  │  Config  │  │ Formation│  │Evaluation│  │   ...    │
        │  8761    │  │  8888    │  │  8081    │  │  8083    │  │          │
        └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘
                             ▲
                    Tous les microservices s’enregistrent dans Eureka
                    et peuvent optionnellement charger la config depuis Config Server.
```

- **Eureka** : annuaire des services (obligatoire pour le routage Gateway).
- **Config Server** : configuration centralisée (optionnel ; si absent, chaque service utilise son `application.properties` local).
- **Gateway** : route `/api/formations/**` → Formation, `/api/projects/**` → Project, `/api/examens/**` → Evaluation, etc.

---

## Contributors

- Équipe projet MatchFreelance — Esprit School of Engineering.

*(Renseigner les noms et rôles des membres de l’équipe.)*

---

## Academic Context

- **Établissement** : Esprit School of Engineering – Tunisia  
- **Année universitaire** : *(à compléter, ex. 2024–2025)*  
- **Projet** : Projet d’intégration (PI) — Architecture microservices, full-stack Angular / Spring Boot.

---

## Getting Started

Chaque membre de l’équipe peut exécuter le projet sur sa machine en suivant les étapes ci-dessous.

### Prérequis

- **Java 17** (JDK 17 requis pour compiler et lancer le backend ; définir `JAVA_HOME` sur ce JDK) et **Maven**
- **Node.js** (LTS recommandé) et **npm**
- **MySQL** (port 3306, utilisateur `root`, mot de passe vide par défaut dans les configs de dev)

### 1. Cloner le dépôt

```bash
git clone <url-du-repo>
cd <nom-du-repo>
```

### 2. Démarrer MySQL

Assurez-vous que MySQL est lancé et accessible sur `localhost:3306`. Les microservices créent les bases automatiquement (`createDatabaseIfNotExist=true`) si besoin.

### 3. Démarrer le backend (ordre important)

Ouvrir **plusieurs terminaux** (ou utiliser un script de démarrage) et lancer dans l’ordre :

| Ordre | Service        | Dossier                          | Port |
|-------|-----------------|----------------------------------|------|
| 1     | Eureka          | `backend/EurekaServer/`          | 8761 |
| 2     | Config Server   | `backend/ConfigServer/`          | 8888 (optionnel) |
| 3     | Gateway         | `backend/Gateway/`               | 8050 |
| 4     | Formation       | `backend/microservices/Formation/` | 8081 |
| 5     | Evaluation      | `backend/microservices/Evaluation/` | 8083 |
| 6     | Skill           | `backend/microservices/Skill/`   | 8086 |
| 7     | Project         | `backend/microservices/Project/` | 8084 |

Exemple (un terminal par service) :

```bash
cd backend/EurekaServer && mvn spring-boot:run
# Attendre le démarrage, puis :
cd backend/ConfigServer && mvn spring-boot:run
cd backend/Gateway && mvn spring-boot:run
# ... idem pour Formation, Evaluation, Skill, Project
```

Vérifications utiles :

- Eureka : http://localhost:8761 (tableau de bord des services enregistrés)
- Gateway : http://localhost:8050/api (réponse JSON de bienvenue)

### 4. Démarrer le frontend

```bash
cd frontend
npm install
npm start
```

Puis ouvrir **http://localhost:4200**.

En mode dev, le proxy Angular envoie `/api/*` vers la Gateway (8050), sauf `/api/projects` et `/api/modules` qui peuvent être redirigés vers les microservices directs selon `proxy.conf.cjs`. Pour une expérience complète, lancer **tous** les microservices listés ci-dessus.

### 5. Comptes de test

*(À compléter selon les données seed ou fixtures : ex. admin@esprit.tn / password, freelancer@test.com, etc.)*

---

## Structure du projet

```
├── frontend/                 # Application Angular
│   ├── src/app/
│   │   ├── components/       # Composants (login, project-list, formation-form, etc.)
│   │   ├── services/         # Services API (auth, project, formation, skill, etc.)
│   │   ├── guards/           # Gardes de route (auth)
│   │   └── interceptors/     # Intercepteur HTTP (token)
│   └── proxy.conf.cjs        # Proxy dev vers backend
├── backend/
│   ├── EurekaServer/         # Serveur de découverte
│   ├── ConfigServer/         # Serveur de configuration centralisée
│   ├── Gateway/              # API Gateway
│   └── microservices/
│       ├── Formation/        # Formations, modules, inscriptions
│       ├── Evaluation/       # Examens, certificats
│       ├── Skill/            # Compétences, parcours intelligent
│       └── Project/          # Projets
├── python/                   # Scripts (rappels, recommandations, badges, ML)
├── DEMARRAGE_PLATEFORME.md   # Détails démarrage et dépannage
└── README.md                 # Ce fichier
```

---

## Dépannage

| Problème | Vérification |
|----------|----------------|
| 404 sur /api/... | Gateway et Eureka sont-ils démarrés ? Les microservices sont-ils visibles dans Eureka (8761) ? |
| Liste projets vide | Le microservice Project (8084) est-il lancé ? MySQL démarré ? |
| Erreur 401 / session | Se reconnecter (login). Vérifier que le token est bien envoyé (interceptor). |
| Config Server absent | Optionnel : les services démarrent sans lui en utilisant leur config locale. |

Plus de détails : voir **DEMARRAGE_PLATEFORME.md**.

---

## Licence

Projet académique — Esprit School of Engineering – Tunisia.
