# MatchFreelance – Plateforme Freelancing

## Overview

This project was developed as part of the **PIDEV – 4th Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

It is a full-stack freelancing platform that connects freelancers with clients: projects, candidatures (postuler), contracts, and **interviews** (scheduling, availability, in-app visio, reviews). The architecture is **microservices** (Spring Boot, Eureka, API Gateway) with an **Angular** front-end. Roles: **Admin**, **Client** (project owner), **Freelancer**.

## Features

- **Projects:** CRUD, search, filter (project microservice).
- **Candidatures:** Apply to projects (postuler), list by project (candidature microservice).
- **Contracts:** Contract lifecycle (contract microservice).
- **Interviews (Interview microservice):**
  - **Availability:** Freelancers define time slots; batch and weekly generation.
  - **Interviews:** Full CRUD, search with filters and pagination, lifecycle (propose → confirm / reject → complete / cancel / no-show).
  - **Notifications:** In-app list, mark as read, automatic reminders (24h, 1h).
  - **Cancellation policy:** Cancel allowed only if more than 24h before start.
  - **Visio:** In-app meeting (Jitsi) or external link; access only within interview time window.
  - **Reviews:** Create after COMPLETED (score 1–5, comment); list by interview or by reviewee; aggregate.
  - **iCalendar:** Download .ics from list or interview detail.
  - **Advanced:** Alternative slot suggestions, reliability index (freelancer/owner), workload classification, top N freelancers (reliability + review score).
- **Users:** Login, roles (user microservice).

## Tech Stack

### Frontend

- Angular 18+
- TypeScript
- Standalone components
- Proxy `/api` to API Gateway

### Backend

- Java 17+
- Spring Boot 3
- Spring Cloud (Eureka Server, API Gateway)
- Spring Data JPA
- MySQL (e.g. XAMPP)

## Architecture

- **Eureka Server** (port 8761): service discovery; all microservices register here.
- **API Gateway** (port 8081): single entry point; routes `/api/*` to the appropriate microservice.
- **Microservices:**  
  project-service (8082), candidature-service (8083), contract-service (8084), **interview-service** (8085), user-service (8086).  
  Each can use its own database (e.g. `freelancing_interview` for interview-service).
- **Angular** (port 4200): SPA; consumes APIs via the gateway.

Detailed run order and ports: see **Getting Started** and `docs/RUN_PLATFORM_FOR_INTERVIEW_TEST.md`.

## Contributors

- [Add team member names and roles]

## Academic Context

Developed at **Esprit School of Engineering – Tunisia**.  
**PIDEV – 4SAE | 2025–2026**

## Getting Started

**Prerequisites:** Java 17+, Maven, Node.js/npm, MySQL (e.g. XAMPP).

Start services **in this order** (each in its own terminal, from project root):

1. **Eureka Server** (8761)  
   `cd eureka-server && mvn -q -DskipTests spring-boot:run`

2. **API Gateway** (8081)  
   `cd api-gateway && mvn -q -DskipTests spring-boot:run`

3. **Project service** (8082)  
   `cd project-service && mvn -q -DskipTests spring-boot:run`

4. **Candidature service** (8083)  
   `cd candidature-service && mvn -q -DskipTests spring-boot:run`

5. **Interview service** (8085)  
   `cd interview-service && mvn -q -DskipTests spring-boot:run`

6. **User service** (8086, if used)  
   `cd user-service && mvn -q -DskipTests spring-boot:run`

7. **Angular** (4200)  
   `cd angular && npx ng serve --open`

Then open **http://localhost:4200**. Use **http://localhost:8081** for direct API calls (gateway).

**Documentation:**

- `docs/INTERVIEW_MICROSERVICE_FEATURES_AND_TESTING.md` – Interview & availability features and how to test.
- `docs/INTERVIEW_CRUD_AND_ADVANCED_FUNCTIONS.txt` – CRUD and advanced functions, code locations, UI test steps.
- `docs/RUN_PLATFORM_FOR_INTERVIEW_TEST.md` – Short run guide and UI entry points.
- `docs/GRILLE_EVALUATION.md` – Evaluation grid (SAE Sprint 1).

## Acknowledgments

Thanks to **Esprit School of Engineering** supervisors and teaching staff.

---

**Repository naming (Esprit):** `Esprit-PIDEV-4SAE10-2026-MatchFreelance` (adapt class/year as needed).  
**Description (GitHub):** Include *Developed at Esprit School of Engineering – Tunisia*, academic year, and main technologies.  
**Topics (minimum):** `esprit-school-of-engineering`, `academic-project`, `esprit-pidev`, `2025-2026`, `angular`, `spring-boot`, `microservices`.
