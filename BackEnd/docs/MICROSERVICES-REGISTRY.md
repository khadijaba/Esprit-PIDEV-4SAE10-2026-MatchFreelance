# Registre des microservices (équipe)

À mettre à jour à chaque nouveau microservice. Tous s'enregistrent sur **le même Eureka** (`EUREKA_URL`).

| Service      | spring.application.name | Port | Dossier / Responsable | Routes Gateway (ex.)   |
|-------------|-------------------------|------|------------------------|-------------------------|
| Eureka      | -                       | 8761 | Eureka/eureka          | -                       |
| API Gateway | ApiGateway              | 8086 | ApiGateway             | /api/* → microservices  |
| Skill       | SKILL                   | 8083 | Microservices/Skill    | /api/skills, cv, portfolio, bio |
| Project     | PROJECT                 | 8084 | Microservices/Project  | /api/projects           |
| User        | USER                    | 8085 | Microservices/User     | /api/users (à ajouter)  |

## Ajouter un service (nouveau membre)

1. Créer le module sous `Microservices/NomService/`.
2. Mettre `spring.application.name=NOM` (MAJUSCULES), port unique, `eureka.client.service-url.defaultZone=${EUREKA_URL:http://localhost:8761/eureka/}`.
3. Dans **ApiGateway** : ajouter une route vers `lb://NOM` dans `ApiGatewayApplication.java` (voir Skill/Project).
4. Ajouter une ligne dans ce tableau.
