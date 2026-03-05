# Freelancing Platform - Microservices Architecture

## Architecture

```
    Angular (4200)          Eureka (8761)
           |                      |
           | /api/*               | (service discovery)
           v                      v
    +--------------+      +------------------+
    | API Gateway  |<-----+|  Eureka Server   |
    |  (port 8081) |      +------------------+
    +------+-------+
           |
     +-----+-----+-----+-----+
     |     |     |     |     |
     v     v     v     v     v
  /api/projects  /api/candidatures  /api/contracts  /api/users
     |     |     |     |     |
     v     v     v     v     v
+---------+ +-------------+ +-------------+ +-------------+
| project | | candidature | |  contract   | |    user    |
| (8082)  | |   (8083)    | |   (8084)    | |   (8085)   |
+----+----+ +------+------+ +------+------+ +------+-----+
     |             |             |                  ^
     |             |             |                  |
     +-------------+-------------+------------------+--resolve clientId/freelancerId to names
```

## Services

| Service | Port | Eureka Name | Description |
|---------|------|-------------|-------------|
| Eureka Server | 8761 | eureka-server | Service discovery |
| API Gateway | 8081 | api-gateway | Routes /api/* to microservices |
| user-service | 8085 | user-service | User CRUD (email, name, role CLIENT/FREELANCER) |
| project-service | 8082 | project-service | Projects CRUD, Task planning, resolves clientId via user-service |
| candidature-service | 8083 | candidature-service | Candidatures, Interview scheduling, resolves freelancerId via user-service |
| contract-service | 8084 | contract-service | Contracts CRUD, resolves clientId/freelancerId via user-service |

## How to Run

**Order matters.** Start Eureka first, then user-service (other services resolve user IDs via it), then the rest, then the API Gateway, then Angular.

1. **Eureka Server**
   ```bash
   cd eureka-server && mvn spring-boot:run
   ```

2. **user-service**
   ```bash
   cd user-service && mvn spring-boot:run
   ```

3. **project-service**
   ```bash
   cd project-service && mvn spring-boot:run
   ```

4. **candidature-service**
   ```bash
   cd candidature-service && mvn spring-boot:run
   ```

5. **contract-service**
   ```bash
   cd contract-service && mvn spring-boot:run
   ```

6. **API Gateway**
   ```bash
   cd api-gateway && mvn spring-boot:run
   ```

7. **Angular**
   ```bash
   cd angular && ng serve
   ```

## URLs

- **Frontoffice:** http://localhost:4200/
- **Admin:** http://localhost:4200/admin
- **Client:** http://localhost:4200/client
- **API Gateway:** http://localhost:8081 (all /api/* requests)
- **Eureka Dashboard:** http://localhost:8761/

## Advanced features (see them live)

After running all services and Angular:

1. **Candidature ranking & budget analytics (Client)**
   - Log in as **CLIENT**, go to **Client** → open a project that has applications.
   - You’ll see **Budget analytics** (applications count, avg/median budget, recommended range).
   - Switch the applications view to **Ranked**: candidates are ordered by composite score (AI + budget + response speed + proposal quality) with score and formula shown.

2. **Contract financial summary & health**
   - **Client:** On a project with an active/completed contract, click **Finance** or **Health** on the contract card to load and show financial summary (total, platform fee, freelancer net, releasable by progress) and contract health (score, level, timeline status, flags).
   - **Freelancer:** Open **Contracts** → a contract → use **Finance** / **Health** to see the same.
   - **Admin:** **Contracts** → open a contract → **Financial summary** / **Health** buttons.

## Database

**Default:** H2 in-memory (data is lost on restart).

**MySQL:** To use MySQL with persistent storage:

1. Install MySQL and ensure it's running on `localhost:3306`.

2. Create databases (or let them auto-create with `createDatabaseIfNotExist=true`):
   ```sql
   CREATE DATABASE IF NOT EXISTS freelancing_users;
   CREATE DATABASE IF NOT EXISTS freelancing_projects;
   CREATE DATABASE IF NOT EXISTS freelancing_candidatures;
   CREATE DATABASE IF NOT EXISTS freelancing_contracts;
   ```

3. Set credentials in each service's `application-mysql.properties`:
   - `spring.datasource.username` (default: root)
   - `spring.datasource.password` (your MySQL password)

4. Run each service with the `mysql` profile:
   ```bash
   cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   cd project-service && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   cd candidature-service && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   cd contract-service && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

   Or set `SPRING_PROFILES_ACTIVE=mysql` in your environment.

## Task Planning & Interview

- **Tasks:** `GET/POST /api/projects/{projectId}/tasks`, `PUT/DELETE /api/projects/{projectId}/tasks/{taskId}` – manage tasks per project (TODO, IN_PROGRESS, DONE)
- **Interviews:** `GET/POST /api/candidatures/{candidatureId}/interviews`, `PUT/DELETE /api/candidatures/{candidatureId}/interviews/{interviewId}` – schedule and manage interviews per candidature (SCHEDULED, COMPLETED, CANCELLED, NO_SHOW)

## Inter-Service Communication

- **project-service** calls **user-service** via `http://user-service/api/users/{id}` and `/api/users/ids?ids=...` to resolve clientId to clientName
- **candidature-service** calls **project-service** via `http://project-service/api/projects/{id}` (Eureka resolves the host)
- **candidature-service** calls **contract-service** via `http://contract-service/api/contracts` to create contracts when a candidature is accepted
- **candidature-service** calls **user-service** via `http://user-service/api/users/{id}` and `/api/users/ids?ids=...` to resolve freelancerId to freelancerName
- **contract-service** calls **user-service** via `http://user-service/api/users/{id}` and `/api/users/ids?ids=...` to resolve clientId/freelancerId to names
