# Eureka — Service Registry (MatchFreelance)

Netflix **Eureka** is the **service discovery** layer: every Spring Cloud microservice registers under a logical name; the **API Gateway** and other clients resolve instances via `lb://<application-name>` instead of hard-coded hosts.

Official Spring Cloud Netflix Eureka reference: [Service Discovery with Eureka](https://spring.io/guides/gs/service-registration-and-discovery/) (concept overview).

---

## Role in this stack

| Component | Module | Default port (host) | Responsibility |
|-----------|--------|---------------------|----------------|
| **Eureka Server** | `eureka-server` | **8761** | Registry UI & REST API; does **not** register itself (`register-with-eureka=false`). |
| **Config Server** | `config-server` | **8888** | Central configuration (optional at startup: `optional:configserver:...`). |
| **API Gateway** | `api-gateway` | **8081** | Routes to microservices using **Spring Cloud Gateway** + **LoadBalancer** + Eureka. |

All business microservices are **Eureka clients**: they register on startup and send heartbeats (renew interval ~30s).

---

## URLs

| Environment | Eureka dashboard | Register endpoint |
|-------------|------------------|-------------------|
| **Local** (IDE / `java -jar`) | http://localhost:8761 | `http://localhost:8761/eureka/` |
| **Docker Compose** | http://localhost:8761 | `http://eureka-server:8761/eureka/` |

Clients set this via:

- **Properties:** `eureka.client.service-url.defaultZone=...`
- **Docker:** `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/` (see `docker-compose.yml` anchor `x-java-common`).

---

## Application names (what appears in Eureka)

Eureka displays names in **uppercase** (e.g. `USER-SERVICE`). The value comes from **`spring.application.name`** and must match **Gateway** routes using `lb://<name>`.

| `spring.application.name` | Typical Eureka row | Notes |
|---------------------------|--------------------|--------|
| `eureka-server` | (server only) | Not registered as a client. |
| `config-server` | CONFIG-SERVER | If enabled as client in your build. |
| `api-gateway` | API-GATEWAY | Discovers other services for routing. |
| `user-service` | USER-SERVICE | |
| `project-service` | PROJECT-SERVICE | |
| `candidature-service` | CANDIDATURE-SERVICE | |
| `contract-service` | CONTRACT-SERVICE | |
| `interview-service` | INTERVIEW-SERVICE | |
| `evaluation-service` | EVALUATION-SERVICE | |
| `formation-service` | FORMATION-SERVICE | |
| `skill-service` | SKILL-SERVICE | Skills / portfolio / bio / CV (`8093`). |
| `analytics-service` | ANALYTICS-SERVICE | R2DBC / PostgreSQL. |

---

## Gateway ↔ Eureka

The gateway resolves routes with **`lb://<spring.application.name>`** (see `api-gateway/src/main/resources/application.properties`).

Example:

```properties
spring.cloud.gateway.routes[0].uri=lb://project-service
```

No static host/port in the gateway for business services: Eureka provides the instance list; the **load balancer** picks one.

---

## Config Server (optional)

Services import config with:

```properties
spring.config.import=optional:configserver:http://localhost:8888
```

Shared defaults for Eureka clients live in `config-server/src/main/resources/config-repo/application.yml`; per-service overrides use `<application-name>.yml` (e.g. `skill-service.yml`).

If Config Server is down at first contact, **`optional:`** allows the app to start with local `application.properties` / profile files.

---

## Run order (local or Docker)

1. **MySQL / PostgreSQL** (if used) healthy  
2. **Eureka Server** (`8761`)  
3. **Config Server** (`8888`) — recommended before clients  
4. **Microservices** (any order after Eureka is up)  
5. **API Gateway** (`8081`) — after services it routes to are registered (or use `depends_on` in Compose)

From repo root (JARs built first):

```bash
.\scripts\build-jars.ps1
docker compose up -d --build
```

Eureka UI: http://localhost:8761  

---

## Troubleshooting

| Symptom | Check |
|--------|--------|
| Service missing in Eureka | Container/process running? Correct `spring.application.name`? Firewall? Eureka URL (`localhost` vs `eureka-server` in Docker). |
| `Connection refused` to Config | Normal race at boot if `optional:` — app continues; or start Config before clients. |
| Gateway `503` / unknown host | Target service not registered; verify `lb://` name matches `spring.application.name`. |
| Wrong instance / stale | Wait for lease expiry; restart client; check `eureka.instance.prefer-ip-address` in Docker networks. |

---

## Branch / repository note (**EU**)

This documentation applies to the **integrated microservices layout** on branch **`EU`** (and **`candidat`**): infrastructure and services at repository root (`eureka-server/`, `api-gateway/`, `docker-compose.yml`, etc.). Older layouts under only `backend/` may differ; always confirm `spring.application.name` and ports in each module’s `application.properties`.

---

## References

- [Spring Cloud Netflix Eureka Client](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/#spring-cloud-eureka-server) (reference documentation; project uses the client against a custom Eureka server).
- Repository: [Esprit-PIDEV-4SAE10-2026-MatchFreelance](https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance) — branch **EU**.
