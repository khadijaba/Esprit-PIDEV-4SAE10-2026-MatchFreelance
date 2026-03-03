# API Gateway

API Gateway pour la plateforme MatchFreelance.

## Configuration

- **Port**: 8086
- **Eureka**: http://192.168.1.2:8761/eureka/

## Routes

Toutes les routes sont préfixées par `/api` :

- `/api/skills/**` → Skill Service (port 8083)
- `/api/cv/**` → Skill Service - CV endpoints (port 8083)
- `/api/portfolio/**` → Skill Service - Portfolio endpoints (port 8083)
- `/api/projects/**` → Project Service (port 8084)

## Démarrage

```bash
cd ApiGateway
mvn clean install
mvn spring-boot:run
```

L'API Gateway sera accessible sur **http://localhost:8086**

## Configuration Java

Le projet utilise Java 17. Assurez-vous que :
1. Le SDK Java est configuré sur la version 17 dans votre IDE
2. La variable d'environnement JAVA_HOME pointe vers Java 17
3. Le pom.xml contient la configuration du maven-compiler-plugin avec source et target 17

## Intégration Frontend

Le frontend Angular est configuré pour utiliser le proxy vers l'API Gateway sur le port 8086 via le fichier `proxy.conf.json`.
