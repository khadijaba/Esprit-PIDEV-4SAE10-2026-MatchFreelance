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

## 2) SonarQube projects

Create three Sonar projects and use these keys:

- `matchfreelance-frontend`
- `matchfreelance-candidature`
- `matchfreelance-contract`

Coverage paths used by pipelines:

- Frontend: `coverage/angular/lcov.info`
- Candidature: `target/site/jacoco/jacoco.xml`
- Contract: `target/site/jacoco/jacoco.xml`

## 3) Kubernetes deployment scope

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

## 4) Demo credentials and URLs

- Grafana default:
  - user: `admin`
  - password: `admin123`
- Prometheus service: `prometheus.matchfreelance.svc.cluster.local:9090`
- Grafana service: `grafana.matchfreelance.svc.cluster.local:3000`

Expose services for demo using `kubectl port-forward` (or NodePort/Ingress if configured).
