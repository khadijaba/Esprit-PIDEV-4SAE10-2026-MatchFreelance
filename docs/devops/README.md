# DevOps Setup (WSL) - Frontend, Candidature, Contract

## 1) Jenkins jobs to create

Create exactly these six Pipeline jobs in Jenkins:

- `frontend-ci` -> Script from SCM: `frontend/Jenkinsfile.ci`
- `frontend-cd` -> Script from SCM: `frontend/Jenkinsfile.cd`
- `candidature-ci` -> Script from SCM: `backend/microservices/Candidature/Jenkinsfile.ci`
- `candidature-cd` -> Script from SCM: `backend/microservices/Candidature/Jenkinsfile.cd`
- `contract-ci` -> Script from SCM: `backend/microservices/Contract/Jenkinsfile.ci`
- `contract-cd` -> Script from SCM: `backend/microservices/Contract/Jenkinsfile.cd`

Notes:

- CI pipelines already trigger their matching CD pipeline automatically (`build job: ...`) on success.
- Required Jenkins credentials:
  - `github-credentials`
  - `docker-hub-credentials`
  - `sonar-token`
  - `kubeconfig-prod`
- Required Jenkins tools (names must match **exactly** — see below):
  - `JDK17` and `Maven3` (backend CI/CD)
  - `Node20` is optional for frontend jobs (they download Node via `scripts/ci/bootstrap-node.sh`); keep **Node20** only if you still use it elsewhere.

### Configure tools in Jenkins (fixes `Tool type "nodejs" does not have an install of "Node20"`)

1. Install plugins if needed: **Manage Jenkins → Plugins** → ensure **NodeJS** (and **Pipeline**, **Git**) are installed.
2. **Manage Jenkins → Tools** (or **Global Tool Configuration** on older Jenkins).
3. **JDK** → **Add JDK** → set **Name** to `JDK17` → either point to an existing JDK 17 on the agent or use an installer.
4. **Maven** → **Add Maven** → set **Name** to `Maven3` → use installer or `MAVEN_HOME` on the agent.
5. **NodeJS** → **Add NodeJS** → set **Name** to **`Node20`** (this string is what the pipeline references; spelling and case matter).
   - Enable **Install automatically** and pick a **20.x** version from the list, *or* point **Installation directory** at a folder that already contains Node 20 on the agent.
6. **Save**, then re-run **`frontend-ci`** / **`frontend-cd`**.

**Frontend pipelines:** Node is provisioned with **`scripts/ci/bootstrap-node.sh`** (default **20.19.2**, meets Angular 21’s **≥ 20.19** requirement). You do **not** need the Jenkins **Node20** tool for `frontend-ci` / `frontend-cd`. Override the version with env **`NODE_CI_VERSION`** on the agent if needed.

**Frontend tests in Jenkins:** **`npm run test:ci`** runs **`ng run angular:unit-test`** (Angular **`@angular/build:unit-test`** with **Vitest** in Node — no Chrome). Local **`npm test`** still uses **Karma** + **ChromeHeadless**. Coverage for Sonar stays **`coverage/angular/lcov.info`**; JUnit is **`test-results/unit-tests.xml`**. Requires **`@vitest/coverage-v8`** (already in **`package.json`**).

If you see **`EnvVarsForToolStep` / `getName()` is null**: under **Tools**, delete any **empty** JDK/Maven/Node rows (every installation must have a non-blank **Name**). Backend jobs still use Declarative `tools { jdk … maven … }`.

## 2) SonarQube URL (why `localhost:9000` works in the browser but not in Jenkins)

- **`http://sonarqube:9000`** only resolves when Jenkins and SonarQube share the same Docker network and the Sonar container is named `sonarqube`.
- Your browser uses **`http://localhost:9000`** because SonarQube listens on the machine where you open the browser.
- **Jenkins in Docker** must call the host with **`http://host.docker.internal:9000`** (default in the scoped `Jenkinsfile.ci` files). Override with a Jenkins global or job env var **`SONAR_HOST_URL`** (for example `http://127.0.0.1:9000` if the Jenkins agent runs on the host, not in a container).

## 3) SonarQube projects

Create three Sonar projects and use these keys:

- `matchfreelance-frontend`
- `matchfreelance-candidature`
- `matchfreelance-contract`

Coverage paths used by pipelines:

- Frontend: `coverage/angular/lcov.info`
- Candidature: `target/site/jacoco/jacoco.xml`
- Contract: `target/site/jacoco/jacoco.xml`

## 4) Kubernetes deployment scope

Only these workloads are in this scope:

- Frontend
- Candidature
- Contract
- Monitoring stack (Prometheus, Grafana, Alertmanager)

Apply in this order:

1. `kubectl apply -f k8s/namespace.yaml`
2. `kubectl apply -f k8s/frontend/`
3. `kubectl apply -f k8s/candidature/`
4. `kubectl apply -f k8s/contract/`
5. `kubectl apply -f k8s/monitoring/`

## 5) Demo credentials and URLs

- Grafana default:
  - user: `admin`
  - password: `admin123`
- Prometheus service: `prometheus.matchfreelance.svc.cluster.local:9090`
- Grafana service: `grafana.matchfreelance.svc.cluster.local:3000`

Expose services for demo using `kubectl port-forward` (or NodePort/Ingress if configured).
