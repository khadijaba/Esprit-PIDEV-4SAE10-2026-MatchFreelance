# Documentation API (Swagger / OpenAPI 3)

Les microservices utilisent **springdoc-openapi** : interface **Swagger UI** et spec **OpenAPI** generees a partir des controllers Spring.

## Acces par defaut (developpement local)

Ouvrir **Swagger UI** via **`/swagger-ui.html`** (redirection) ou directement **`/swagger-ui/index.html`**.

| Service | Port | Swagger UI | OpenAPI (JSON) |
|---------|------|------------|----------------|
| Formation | 8081 | http://localhost:8081/swagger-ui/index.html | http://localhost:8081/v3/api-docs |
| Skill | 8083 | http://localhost:8083/swagger-ui/index.html | http://localhost:8083/v3/api-docs |
| Project | 8084 | http://localhost:8084/swagger-ui/index.html | http://localhost:8084/v3/api-docs |
| User | 8085 | http://localhost:8085/swagger-ui/index.html | http://localhost:8085/v3/api-docs |
| Candidature | 8087 | http://localhost:8087/swagger-ui/index.html | http://localhost:8087/v3/api-docs |
| Contract | 8088 | http://localhost:8088/swagger-ui/index.html | http://localhost:8088/v3/api-docs |
| Interview | 8089 | http://localhost:8089/swagger-ui/index.html | http://localhost:8089/v3/api-docs |

## Si vous avez une erreur 404

1. **Reconstruire le JAR** apres ajout de springdoc : `mvn clean package -pl Microservices/Project` (ou depuis le dossier `Microservices/Project`).
2. **Redemarrer** le service (IDE ou `java -jar` / Docker). Une image Docker basee sur un ancien `target/*.jar` n’inclut pas Swagger tant que vous n’avez pas rebuild + rebuild image.
3. Verifier que **http://localhost:PORT/v3/api-docs** repond (JSON). Si ce endpoint est aussi en 404, springdoc n’est pas dans le classpath ou l’auto-configuration ne s’active pas.
4. Sous **Spring Boot 4**, les POM utilisent **`spring-boot-starter-webmvc`** (et non seulement l’ancien `spring-boot-starter-web` deprecie) pour que l’auto-configuration standard et springdoc se chargent correctement.

## Notes

- **User** : Spring Security est actif. Les chemins `/swagger-ui/**` et `/v3/api-docs/**` sont publics ; le reste des endpoints necessite en general un JWT (bouton *Authorize* dans Swagger UI si vous configurez le schema securite plus tard).
- Les ports peuvent changer via Config Server ou variables d'environnement ; verifiez `server.port` dans chaque service.
- L'API Gateway (8086) ne centralise pas la documentation dans cette configuration : ouvrez Swagger sur le port du microservice concerne.

## Dependance Maven

`org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2` (aligne sur Spring Boot 4.0.x).
