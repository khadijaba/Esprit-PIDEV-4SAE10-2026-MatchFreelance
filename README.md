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
- MySQL
- REST APIs
- JWT Authentication

## Architecture

The system follows a **Microservices Architecture** using Spring Cloud components:

### Infrastructure Components

- **API Gateway**
- **Eureka Service Registry**

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
git checkout candidat
```

### Backend

Start infrastructure and microservices (order matters). From the project root:

```bash
# 1. Eureka Server
cd eureka-server
./mvnw spring-boot:run

# 2. API Gateway (in another terminal)
cd api-gateway
./mvnw spring-boot:run

# 3. User, Project, Candidature, Contract services (each in its folder)
cd user-service && ./mvnw spring-boot:run
cd project-service && ./mvnw spring-boot:run
cd candidature-service && ./mvnw spring-boot:run
cd contract-service && ./mvnw spring-boot:run
```

See `SETUP_GUIDE.md` and `MICROSERVICES.md` for detailed setup and MySQL configuration.

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
