# MatchFreelance

## Overview

This project was developed as part of the PIDEV – 4th Year Engineering Program at Esprit School of Engineering (Academic Year 2025–2026). It consists of a full-stack web application that connects freelancers with clients, allowing them to post missions, apply for projects, and collaborate efficiently.

This repository (branch **candidat**) focuses on the **Candidature** and **Contract** microservices: applications, ranking, interviews, and contract lifecycle (chat, progress, payments).

## Features

- Freelancer profile management
- Mission posting and applications
- Client and freelancer matching system
- **Candidature**: apply for projects, pitch analysis, ranking, interviews
- **Contract**: contract creation, chat, progress tracking, payments and milestones
- Project tracking and collaboration

## Tech Stack

### Frontend

- Angular

### Backend

- Spring Boot
- MySQL (user, project, candidature, contract services)
- PostgreSQL + **WebFlux + R2DBC** (`analytics-service` — advanced stack)
- REST APIs
- JWT Authentication

## Architecture

The system follows a **Microservices Architecture** using Spring Cloud components:

### Infrastructure Components

- **Spring Cloud Config Server** (port 8888, central YAML in `config-server`)
- **API Gateway**
- **Eureka Service Registry**

**Eureka documentation:** [docs/EUREKA.md](docs/EUREKA.md) — registry URLs, `spring.application.name` ↔ Gateway `lb://` routes, Docker env, troubleshooting.

### Business Microservices

- User Microservice
- Project Microservice
- Skills Microservice
- Microformation Microservice
- Evaluation Microservice
- Application Microservice
- **Contrat Microservice**
- **Interview Microservice**
- Forum Microservice

## Contributors

- Emna Dorai
- Khadija Ben Ayed
- Ahmed Bel Haj Dahmen
- Aziz Ben Fedhila
- Med Amine Bejaoui

## Academic Context

Developed at **Esprit School of Engineering – Tunisia**  
PIDEV – 4SAE10 | 2025–2026

## Getting Started

```bash
git clone https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance
cd Esprit-PIDEV-4SAE10-2026-MatchFreelance
git checkout EU
# or: git checkout candidat
```

### Backend

Start infrastructure and microservices (order matters). From the project root:

```bash
# 0. Config Server (optional; services fall back to local properties if down)
cd config-server
mvn spring-boot:run

# 1. Eureka Server
cd eureka-server
mvn spring-boot:run

# 2. User, Project, Candidature, Contract (each in its own terminal)
cd user-service && mvn spring-boot:run
cd project-service && mvn spring-boot:run
cd candidature-service && mvn spring-boot:run
cd contract-service && mvn spring-boot:run

# 3. Analytics (PostgreSQL: run scripts/init-postgres-analytics.sql first)
cd analytics-service && mvn spring-boot:run

# 4. API Gateway
cd api-gateway && mvn spring-boot:run
```

See `MICROSERVICES.md` for ports, Config/Eureka/Gateway details, MySQL and PostgreSQL setup.

**Docker:** with Docker Desktop, from the repo root run `docker compose up --build` — UI at http://localhost:4200 (details in `MICROSERVICES.md`).

### Frontend

```bash
cd angular
npm install
ng serve
```

## Acknowledgments

Thanks to Esprit School of Engineering supervisors and teaching staff.

---

*esprit-school-of-engineering · academic-project · esprit-pidev · 2025-2026 · angular · spring-boot · mysql*
