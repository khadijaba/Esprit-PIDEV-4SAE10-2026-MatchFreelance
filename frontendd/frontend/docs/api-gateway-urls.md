# Configuration API Gateway et URLs

## Flux des requêtes

1. **Frontend** (Angular) appelle des URLs relatives : `/api/formations`, `/api/projects`, etc.
2. **Proxy Angular** (`proxy.conf.json`) redirige tout `/api` vers `http://localhost:8086`.
3. **API Gateway** (port 8086) route selon le path vers le bon microservice.

## Routes configurées dans l’API Gateway

| Path frontend (après /api) | Route Gateway      | Microservice / URI                    |
|----------------------------|--------------------|--------------------------------------|
| `/api/skills/**`           | skill-service      | SKILL (Eureka)                       |
| `/api/cv/**`               | cv-service         | SKILL                                |
| `/api/portfolio/**`        | portfolio-service  | SKILL                                |
| `/api/bio/**`              | bio-service        | SKILL                                |
| `/api/projects/**`         | project-service    | PROJECT (Eureka)                    |
| `/api/users/**`            | user-service       | `user.service.uri` (ex. 8085)        |
| `/api/formations/**`       | formation-service  | FORMATION (Eureka) ou URL directe   |
| `/api/examens/**`          | examen-service     | même URI que Formation              |
| `/api/inscriptions/**`     | inscription-service| même URI que Formation              |

## Si « Not Found » à la création d’une formation

1. **Vérifier que l’API Gateway est démarrée** sur le port **8086**.
2. **Vérifier que le microservice Formation est démarré** et qu’il expose :
   - `POST /formations` (création)
   - `GET /formations` (liste)
   - `GET /formations/ouvertes` (liste des formations ouvertes)
   - `GET /formations/{id}` (détail)
   - `PUT /formations/{id}` (modification)
   - `DELETE /formations/{id}` (suppression)

3. **Si le microservice Formation n’est pas enregistré dans Eureka**  
   Dans `ApiGateway/src/main/resources/application.properties` :
   ```properties
   formation.service.uri=http://localhost:PORT_FORMATION
   ```
   Décommenter et remplacer `PORT_FORMATION` par le port réel du microservice Formation (ex. 8087).

4. **Si Formation, Examens et Inscriptions sont des services séparés**  
   Adapter `ApiGatewayApplication.java` pour utiliser des `@Value` et des routes distinctes (ex. `examen.service.uri`, `inscription.service.uri`).

## Proxy frontend (déjà configuré)

- Fichier : `frontend/proxy.conf.json`
- Toutes les requêtes vers `/api` sont envoyées à `http://localhost:8086`.
- Lancer le frontend avec : `ng serve` (le proxy est utilisé automatiquement).
