# Copilot Working Instruction (Global Frontend + Backend)

This file is a project-local playbook to speed up future coding assistance in this repository.

## 1) Architecture snapshot
- Frontend: Angular app in `angular/`.
- Backend: Spring Boot microservices with Eureka + API Gateway.
- Discovery: `eureka-server` on `8761`.
- Gateway: `api-gateway` on `8081`.
- Services in scope:
  - `project-service` (`/api/projects/**`)
  - `candidature-service` (`/api/candidatures/**`)
  - `contract-service` (`/api/contracts/**`)
  - `interview-service` (`/api/interviews/**`, `/api/availability/**`, `/api/notifications/**`, `/api/reviews/**`)
  - `user-service` (`/api/users/**`)

## 2) Working rules for edits
- Prefer minimal, targeted changes over broad refactors.
- Keep service boundaries clear; avoid cross-domain business logic leakage.
- Preserve backward compatibility of existing endpoints when possible.
- Update gateway routes when introducing new REST prefixes.
- Do not edit generated build outputs under `target/`.

## 3) Quick scan flow before implementing
1. Read root `README.md` and relevant file in `docs/`.
2. Locate impacted controller/service/repository in the target microservice.
3. Check DTOs and validation annotations before adding new fields.
4. Verify route exposure in gateway `application.properties`.
5. Confirm Angular service paths if UI is impacted.

## 4) Code quality checklist
- Domain model has explicit statuses/enums for lifecycle transitions.
- Validation returns clear `400` messages for invalid payloads.
- Search/list endpoints support pagination and filtering when useful.
- Business invariants enforced in service layer, not only controller layer.
- Add tests for happy path + at least one invalid path per new endpoint.

## 5) Run and verify (manual)
Use one terminal per service, from repo root:
- `cd eureka-server; mvn -q -DskipTests spring-boot:run`
- `cd api-gateway; mvn -q -DskipTests spring-boot:run`
- `cd project-service; mvn -q -DskipTests spring-boot:run`
- `cd candidature-service; mvn -q -DskipTests spring-boot:run`
- `cd contract-service; mvn -q -DskipTests spring-boot:run`
- `cd interview-service; mvn -q -DskipTests spring-boot:run`
- `cd user-service; mvn -q -DskipTests spring-boot:run`
- `cd angular; npm install; npx ng serve --open`

## 6) If adding a new microservice
- Create independent data model and persistence.
- Register with Eureka and expose through gateway route `/api/<new-domain>/**`.
- Add OpenAPI docs or endpoint table in `README.md`.
- Add sample seed data for demo scenarios.
- Add one advanced, domain-specific feature beyond CRUD.

## 7) Priority mindset for this project
1. Reliability of workflows (status transitions, no invalid state jumps).
2. Clear API contract and error semantics.
3. Demonstrable advanced feature that has visible user value.
4. Evaluation-friendly docs and reproducible run steps.

## 8) Secondary Spring Boot engineering guidance
- Prefer constructor injection with `private final` dependencies.
- Keep controllers thin, services focused, repositories simple.
- Validate all request payloads/params (`jakarta.validation`) and return explicit errors.
- Use SLF4J logging with parameterized messages (avoid `System.out`).
- Use YAML (`application.yml`) for new services when possible.
- Keep secrets externalized via environment variables for non-local environments.
- Build verification after changes: run Maven `clean package` (or equivalent).


