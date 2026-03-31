# BackEnd - Architecture Microservices FreelanceHub

Ce dossier contient tous les microservices Spring Boot de la plateforme FreelanceHub.

## Structure

```
BackEnd/
├── Eureka/
│   └── eureka/                    # Service de découverte (Port 8761)
├── ApiGateway/                    # API Gateway (Port 8086)
└── Microservices/
    ├── Skill/                     # Microservice Skills (Port 8083)
    └── Project/                   # Microservice Project (Port 8084)
```

## Microservices

### 1. Eureka Server (Port 8761)
Service de découverte pour tous les microservices.
- **Chemin**: `Eureka/eureka/`
- **URL**: http://192.168.1.2:8761

### 2. API Gateway (Port 8086)
Point d'entrée unique pour toutes les requêtes API.
- **Chemin**: `ApiGateway/`
- **URL**: http://localhost:8086
- **Routes**:
  - `/api/skills/**` → Skill Service
  - `/api/cv/**` → Skill Service (CV)
  - `/api/portfolio/**` → Skill Service (Portfolio)
  - `/api/projects/**` → Project Service

### 3. Skill Service (Port 8083)
Gestion des compétences des freelancers, CV et portfolio.
- **Chemin**: `Microservices/Skill/`
- **URL**: http://localhost:8083
- **Endpoints**:
  - `GET /skills` - Liste de tous les skills
  - `GET /skills/{id}` - Récupérer un skill par ID
  - `GET /skills/freelancer/{freelancerId}` - Skills d'un freelancer
  - `GET /skills/category/{category}` - Skills par catégorie
  - `POST /skills` - Ajouter un skill
  - `PUT /skills/{id}` - Mettre à jour un skill
  - `DELETE /skills/{id}` - Supprimer un skill
  - `POST /cv/upload/{freelancerId}` - Upload CV
  - `GET /cv/freelancer/{freelancerId}` - Récupérer CV
  - `DELETE /cv/freelancer/{freelancerId}` - Supprimer CV
  - `POST /portfolio/{freelancerId}` - Ajouter portfolio
  - `GET /portfolio/freelancer/{freelancerId}` - Récupérer portfolio
  - `PUT /portfolio/{freelancerId}` - Mettre à jour portfolio
  - `DELETE /portfolio/freelancer/{freelancerId}` - Supprimer portfolio

### 4. Project Service (Port 8084)
Gestion des projets avec compétences requises.
- **Chemin**: `Microservices/Project/`
- **URL**: http://localhost:8084
- **Endpoints**:
  - `GET /projects` - Liste de tous les projets
  - `GET /projects/{id}` - Récupérer un projet par ID
  - `GET /projects/status/{status}` - Projets par statut
  - `GET /projects/owner/{projectOwnerId}` - Projets d'un owner
  - `GET /projects/search?title=...` - Rechercher par titre
  - `GET /projects/skill/{skill}` - Projets nécessitant un skill
  - `POST /projects` - Créer un projet
  - `PUT /projects/{id}` - Mettre à jour un projet
  - `DELETE /projects/{id}` - Supprimer un projet

## Catégories de Skills

- WEB_DEVELOPMENT
- MOBILE_DEVELOPMENT
- DATA_SCIENCE
- DESIGN
- MARKETING
- WRITING
- VIDEO_EDITING
- PHOTOGRAPHY
- CONSULTING
- OTHER

## Statuts de Projet

- OPEN - Projet ouvert aux candidatures
- IN_PROGRESS - Projet en cours
- COMPLETED - Projet terminé
- CANCELLED - Projet annulé

## Base de Données

- **Skill Service**: MySQL database `skill_db`
- **Project Service**: MySQL database `project_db`

Assurez-vous que MySQL est démarré et que les bases de données existent.

## Ordre de Démarrage

1. **Eureka Server** (doit démarrer en premier)
   ```bash
   cd Eureka/eureka
   mvn spring-boot:run
   ```

2. **API Gateway**
   ```bash
   cd ApiGateway
   mvn spring-boot:run
   ```

3. **Skill Service**
   ```bash
   cd Microservices/Skill
   mvn spring-boot:run
   ```

4. **Project Service**
   ```bash
   cd Microservices/Project
   mvn spring-boot:run
   ```

## Configuration

Tous les microservices utilisent `application.properties` pour la configuration.

### Eureka
- IP: `192.168.1.2` (à adapter selon votre réseau)
- Port: `8761`

### Base de Données MySQL
- Host: `localhost:3306`
- Username: `root`
- Password: (vide par défaut, à configurer)

## Technologies

- Spring Boot 4.0.2
- Spring Cloud 2025.1.0
- Spring Cloud Gateway
- Netflix Eureka
- Spring Data JPA
- MySQL
- Lombok
