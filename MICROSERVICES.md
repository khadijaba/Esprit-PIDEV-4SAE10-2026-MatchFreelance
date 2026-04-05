# Freelancing Platform - Microservices Architecture

## Architecture

```
    Angular (4200)     Config Server (8888)     Eureka (8761)
           |                    |                     |
           | /api/*             | (central YAML)    | (discovery)
           v                    v                     v
    +--------------+     +--------------+      +------------------+
    | API Gateway  |     | Config Server|      |  Eureka Server   |
    |  (port 8081) |     |  (native)    |      +------------------+
    +------+-------+     +--------------+
           |
     +-----+-----+-----+-----+-----+
     |     |     |     |     |     |
     v     v     v     v     v     v
  /api/projects ... /api/analytics ...
     |     |     |     |     |     |
     v     v     v     v     v     v
+---------+ +-------------+ +-------------+ +-------------+ +----------------+
| project | | candidature | |  contract   | |    user     | | analytics      |
| (8082)  | |   (8083)    | |   (8084)    | |   (8085)    | | (8095)         |
| MySQL   | |   MySQL     | |   MySQL     | |   MySQL     | | PostgreSQL+R2DBC|
+----+----+ +------+------+ +------+------+ +------+------+ +----------------+
     |             |             |                  ^
     |             |             |                  |
     +-------------+-------------+------------------+--resolve clientId/freelancerId to names
```

- **Config Server** (`config-server`, port **8888**): serves shared `application.yml` and per-service files from `config-server/src/main/resources/config-repo/`. All business services and the gateway use `spring.config.import=optional:configserver:http://localhost:8888`.
- **analytics-service**: **Spring WebFlux + R2DBC + PostgreSQL** (advanced stack vs JPA/MySQL elsewhere). Create DB with `scripts/init-postgres-analytics.sql`.

## Services

| Service | Port | Eureka Name | Description |
|---------|------|-------------|-------------|
| Config Server | 8888 | config-server | Central configuration (native classpath repo) |
| Eureka Server | 8761 | eureka-server | Service discovery |
| API Gateway | 8081 | api-gateway | Routes /api/* to microservices |
| user-service | 8085 | user-service | User CRUD (email, name, role CLIENT/FREELANCER) |
| project-service | 8082 | project-service | Projects CRUD, Task planning, resolves clientId via user-service |
| candidature-service | 8083 | candidature-service | Candidatures, Interview scheduling, resolves freelancerId via user-service |
| interview-service | 8086 | interview-service | Interviews, availability slots, notifications, reviews ([branch interview](https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance/tree/interview/interview-service)) |
| evaluation-service | 8090 | evaluation-service | Examens, certificats, passages ([master/Evaluation](https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance/tree/master/backend/microservices/Evaluation)) |
| formation-service | 8092 | formation-service | Formations, modules, inscriptions ([master/Formation](https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance/tree/master/backend/microservices/Formation)) |
| contract-service | 8084 | contract-service | Contracts CRUD, resolves clientId/freelancerId via user-service |
| analytics-service | 8095 | analytics-service | Platform metrics (WebFlux + R2DBC + PostgreSQL) |

## How to Run

**Order matters.** Start **Config Server** (optional but recommended for full stack demo), **Eureka**, then **user-service**, then other microservices, **analytics-service** (PostgreSQL must be running and DB created), then **API Gateway**, then Angular.

1. **Config Server**
   ```bash
   cd config-server && mvn spring-boot:run
   ```

2. **Eureka Server**
   ```bash
   cd eureka-server && mvn spring-boot:run
   ```

3. **user-service**
   ```bash
   cd user-service && mvn spring-boot:run
   ```

4. **project-service**
   ```bash
   cd project-service && mvn spring-boot:run
   ```

5. **candidature-service**
   ```bash
   cd candidature-service && mvn spring-boot:run
   ```

6. **contract-service**
   ```bash
   cd contract-service && mvn spring-boot:run
   ```

7. **interview-service** (MySQL DB `freelancing_interviews`; run migration SQL from `interview-service/src/main/resources/db/migration/` if needed)
   ```bash
   cd interview-service && mvn spring-boot:run
   ```

8. **evaluation-service** (MySQL `EvaluationDB`)
   ```bash
   cd evaluation-service && mvn spring-boot:run
   ```

9. **formation-service** (MySQL `FormationDB`)
   ```bash
   cd formation-service && mvn spring-boot:run
   ```

10. **analytics-service** (requires PostgreSQL + `matchfreelance_analytics` database)
   ```bash
   cd analytics-service && mvn spring-boot:run
   ```

11. **API Gateway**
   ```bash
   cd api-gateway && mvn spring-boot:run
   ```

12. **Angular**
   ```bash
   cd angular && ng serve
   ```

## URLs

- **Frontoffice:** http://localhost:4200/
- **Admin:** http://localhost:4200/admin
- **Client:** http://localhost:4200/client
- **API Gateway:** http://localhost:8081 (all /api/* requests)
- **Eureka Dashboard:** http://localhost:8761/
- **Config Server:** http://localhost:8888 (e.g. `http://localhost:8888/user-service/default` for resolved config)
- **Analytics (via Gateway):** `GET http://localhost:8081/api/analytics/info` and `/api/analytics/metrics`

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

**PostgreSQL (analytics-service only):** install PostgreSQL, then run `scripts/init-postgres-analytics.sql` (or `CREATE DATABASE matchfreelance_analytics;`). Default user/password in config: `postgres` / `postgres` (override in `config-repo/analytics-service.yml` or local `application.properties`).

**Smoke test without PostgreSQL:** run analytics with in-memory H2 (R2DBC):  
`cd analytics-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev-h2`  
(Config Server is skipped for that profile so R2DBC URL is not overwritten.)

**Default:** H2 in-memory (data is lost on restart).

**MySQL:** To use MySQL with persistent storage:

1. Install MySQL and ensure it's running on `localhost:3306`.

2. Create databases (or let them auto-create with `createDatabaseIfNotExist=true`):
   ```sql
   CREATE DATABASE IF NOT EXISTS freelancing_users;
   CREATE DATABASE IF NOT EXISTS freelancing_projects;
   CREATE DATABASE IF NOT EXISTS freelancing_candidatures;
   CREATE DATABASE IF NOT EXISTS freelancing_contracts;
   CREATE DATABASE IF NOT EXISTS freelancing_interviews;
   CREATE DATABASE IF NOT EXISTS EvaluationDB;
   CREATE DATABASE IF NOT EXISTS FormationDB;
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

## Docker Compose (full stack)

Images follow the **atelier** (“Dockerisation d’un MS”) pattern: **Maven on the host** produces `target/*.jar`, then `docker/Dockerfile.atelier` does `ADD target/<artifact>.jar` + `java -jar` (same structure as the PDF: base image Java 17, `EXPOSE`, `ADD`, `ENTRYPOINT`). The PDF uses `FROM openjdk:17`; Dockerfiles here use **`eclipse-temurin:17-jre-jammy`** because `openjdk` was removed from Docker Hub — same role. Part II (Docker Hub `tag` / `push`, Play with Docker) stays manual if you need it.

With **Docker Desktop** running, from the repository root:

```bash
# PowerShell
.\scripts\build-jars.ps1
docker compose up --build
```

```bash
# Git Bash / Linux / macOS
bash scripts/build-jars.sh
docker compose up --build
```

**Single microservice (ex. candidature)** — comme dans l’atelier, depuis le module :

```bash
cd candidature-service
mvn clean package -DskipTests
docker build -t mf-candidature .
docker run -p 8083:8083 --rm mf-candidature
```

(Le `Dockerfile` dans `candidature-service/` est le modèle minimal du PDF. Pour les autres services, utilise `docker build -f ../docker/Dockerfile.atelier .` avec `--build-arg JAR_FILE=…` et `--build-arg EXPOSE_PORT=…`, ou le script ci-dessus + Compose.)

**Alternative sans Maven local** : image multi-étapes `docker/Dockerfile.service` (Maven dans Docker) — remplace `dockerfile: docker/Dockerfile.atelier` + `context` par l’ancien couple `context: .` + `dockerfile: docker/Dockerfile.service` et `args: MODULE_DIR` / `JAR_FILE` comme avant.

**Ports**

| URL | Service |
|-----|---------|
| http://localhost:4200 | Angular UI (nginx → proxy `/api` → gateway) |
| http://localhost:8081 | API Gateway |
| http://localhost:8761 | Eureka |
| http://localhost:8888 | Config Server |
| http://localhost:3307 | MySQL (`root` / `matchfreelance`) — mapped to container 3306 |
| http://localhost:5433 | PostgreSQL (host → container 5432, analytics DB) |

**Notes**

- First `docker compose` build after `build-jars` is faster than the old all-in-Docker Maven path; Angular image still builds from `./angular`.
- MySQL data persists in volume `matchfreelance_mf_mysql_data`. To re-run init SQL, remove the volume: `docker compose down -v`.
- MySQL is published on host **3307** by default so it does not clash with a local MySQL on **3306** (e.g. XAMPP). Inside Docker, services still connect to `mysql:3306`.
- Config repo in `config-server` uses `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` so Eureka works inside Docker.

## Task Planning & Interview

- **Tasks:** `GET/POST /api/projects/{projectId}/tasks`, `PUT/DELETE /api/projects/{projectId}/tasks/{taskId}` – manage tasks per project (TODO, IN_PROGRESS, DONE)
- **Interviews:** `GET/POST /api/candidatures/{candidatureId}/interviews`, `PUT/DELETE /api/candidatures/{candidatureId}/interviews/{interviewId}` – schedule and manage interviews per candidature (SCHEDULED, COMPLETED, CANCELLED, NO_SHOW)

## Inter-Service Communication

- **project-service** calls **user-service** via `http://user-service/api/users/{id}` and `/api/users/ids?ids=...` to resolve clientId to clientName
- **candidature-service** calls **project-service** via `http://project-service/api/projects/{id}` (Eureka resolves the host)
- **candidature-service** calls **contract-service** via `http://contract-service/api/contracts` to create contracts when a candidature is accepted
- **candidature-service** calls **user-service** via `http://user-service/api/users/{id}` and `/api/users/ids?ids=...` to resolve freelancerId to freelancerName
- **contract-service** calls **user-service** via `http://user-service/api/users/{id}` and `/api/users/ids?ids=...` to resolve clientId/freelancerId to names
