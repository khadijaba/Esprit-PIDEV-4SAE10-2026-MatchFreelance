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
- Required Jenkins tools:
  - `JDK17`
  - `Maven3`
  - `Node20`

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
