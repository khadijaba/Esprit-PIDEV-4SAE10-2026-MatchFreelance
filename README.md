# MatchFreelance — Eureka & service discovery

**MatchFreelance** (PIDEV 4SAE10, Esprit 2025–2026) is a microservices stack. This README documents **Netflix Eureka** as the **service registry**: how instances register, how the **API Gateway** resolves them, and how to run the stack.  
Repository: [Esprit-PIDEV-4SAE10-2026-MatchFreelance](https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance) — branches **`EU`**, **`candidat`**.

Concept overview: [Spring — Service Discovery with Eureka](https://spring.io/guides/gs/service-registration-and-discovery/).

---

## What Eureka does here

Every Spring Cloud microservice registers under a logical **`spring.application.name`**. The **API Gateway** (and other clients) resolve services with **`lb://<application-name>`** instead of fixed hosts/ports.

| Component | Module | Port (host) | Role |
|-----------|--------|-------------|------|
| **Eureka Server** | `eureka-server` | **8761** | Registry UI & API; does **not** register as a client (`register-with-eureka=false`). |
| **Config Server** | `config-server` | **8888** | Central YAML (`optional:configserver:...` at startup). |
| **API Gateway** | `api-gateway` | **8081** | Spring Cloud Gateway + LoadBalancer + Eureka. |

Business services are **Eureka clients**: register on startup, heartbeats ~30s.

---

## URLs

| Environment | Dashboard | `defaultZone` |
|-------------|------------|---------------|
| **Local** | http://localhost:8761 | `http://localhost:8761/eureka/` |
| **Docker** | http://localhost:8761 | `http://eureka-server:8761/eureka/` |

Configure with `eureka.client.service-url.defaultZone` or Docker env `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` (see `docker-compose.yml`, `x-java-common`).

---

## Application names ↔ Eureka ↔ Gateway

Eureka shows names in **UPPERCASE**. They must match **`spring.application.name`** and Gateway **`lb://...`**.

| `spring.application.name` | Eureka (typical) | Notes |
|---------------------------|------------------|--------|
| `api-gateway` | API-GATEWAY | |
| `user-service` | USER-SERVICE | |
| `project-service` | PROJECT-SERVICE | |
| `candidature-service` | CANDIDATURE-SERVICE | |
| `contract-service` | CONTRACT-SERVICE | |
| `interview-service` | INTERVIEW-SERVICE | |
| `evaluation-service` | EVALUATION-SERVICE | |
| `formation-service` | FORMATION-SERVICE | |
| `skill-service` | SKILL-SERVICE | Port **8093**, context `/api` |
| `analytics-service` | ANALYTICS-SERVICE | R2DBC / PostgreSQL |

Example (`api-gateway`):

```properties
spring.cloud.gateway.routes[0].uri=lb://project-service
```

Eureka supplies instances; the **load balancer** chooses one.

---

## Config Server (optional)

```properties
spring.config.import=optional:configserver:http://localhost:8888
```

Shared defaults: `config-server/src/main/resources/config-repo/application.yml`. Per service: `<application-name>.yml`. If Config is down at boot, **`optional:`** still lets apps start from local `application.properties`.

---

## Startup order

1. **MySQL / PostgreSQL** healthy (if used)  
2. **Eureka** (`8761`)  
3. **Config** (`8888`) — recommended before clients  
4. **Microservices**  
5. **Gateway** (`8081`)

**Docker (from repo root):**

```bash
.\scripts\build-jars.ps1
docker compose up -d --build
```

**Eureka UI:** http://localhost:8761  

More ports and services: `MICROSERVICES.md`.

---

## Troubleshooting

| Symptom | What to check |
|---------|----------------|
| Service not in Eureka | Process/container up? Correct `spring.application.name`? `localhost` vs `eureka-server` in Docker? |
| Config `Connection refused` | Race at startup; with `optional:` app continues — or start Config first. |
| Gateway **503** | Target not registered; `lb://` name must match `spring.application.name`. |
| Stale / wrong instance | Lease expiry; restart client; `eureka.instance.prefer-ip-address` on Docker networks. |

---

## Clone & run (minimal)

```bash
git clone https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance
cd Esprit-PIDEV-4SAE10-2026-MatchFreelance
git checkout EU
```

**Local Maven (abbreviated):** `config-server` → `eureka-server` → business services → `api-gateway`. **Frontend:** `cd angular && npm i && ng serve`.

---

## References

- [Spring Cloud Netflix Eureka](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/#spring-cloud-eureka-server) (client against this Eureka server).
- [Spring — Service Discovery with Eureka](https://spring.io/guides/gs/service-registration-and-discovery/).

## Contributors

Emna Dorai · Khadija Ben Ayed · Ahmed Bel Haj Dahmen · Aziz Ben Fedhila · Med Amine Bejaoui  

**Esprit School of Engineering – Tunisia** · PIDEV 4SAE10 · 2025–2026
