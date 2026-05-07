# DevOps Pipeline Setup Summary

## Overview
Complete CI/CD pipeline setup for Interview, User microservices, and Angular frontend, following the exact structure of the Productivity microservice.

## Services Configured

### 1. Interview Microservice
**Location:** `backend/microservices/Interview/`

**Files Created:**
- `Dockerfile` - Multi-stage build with Maven 3.9.9 and Java 21
- `Jenkinsfile.ci` - CI pipeline with build, test, JaCoCo, SonarQube, Docker
- `Jenkinsfile.cd` - CD pipeline with GitOps and ArgoCD
- `k8s/deployment.yml` - Kubernetes deployment and service manifests

**Configuration:**
- Port: 8089
- Docker Image: `benhajdahmenahmed/interview`
- Database: `freelancing_interviews`
- JAR: `interview-service-0.0.1-SNAPSHOT.jar`
- SonarQube Project: `interview` / "Interview Service"
- CD Job: `interview-service-cd`
- K8s: 2 replicas, ClusterIP service
- NodePort: 30089 (for external access if needed)

**POM Updates:**
- Added JaCoCo plugin (version 0.8.11)
- Added Micrometer Prometheus registry
- Configured code coverage minimum: 50%

---

### 2. User Microservice
**Location:** `backend/microservices/User/`

**Files Created:**
- `Dockerfile` - Multi-stage build with Maven 3.9.9 and Java 21
- `Jenkinsfile.ci` - CI pipeline with build, test, JaCoCo, SonarQube, Docker
- `Jenkinsfile.cd` - CD pipeline with GitOps and ArgoCD
- `k8s/deployment.yml` - Kubernetes deployment and service manifests

**Configuration:**
- Port: 8086
- Docker Image: `benhajdahmenahmed/user-service`
- Database: `freelancing_users`
- JAR: `User-0.0.1-SNAPSHOT.jar`
- SonarQube Project: `user-service` / "User Service"
- CD Job: `user-service-cd`
- K8s: 2 replicas, ClusterIP service
- NodePort: 30086 (for external access if needed)

**POM Updates:**
- Added JaCoCo plugin (version 0.8.11)
- Configured code coverage minimum: 50%
- Note: Already has Micrometer Prometheus

---

### 3. Angular Frontend
**Location:** `frontend/`

**Files Created:**
- `Dockerfile` - Multi-stage build (Node 18 + Nginx Alpine)
- `nginx.conf` - Nginx configuration for Angular routing
- `Jenkinsfile.ci` - CI pipeline with npm build and Docker
- `Jenkinsfile.cd` - CD pipeline with GitOps and ArgoCD
- `k8s/deployment.yml` - Kubernetes deployment and service manifests

**Configuration:**
- Port: 80
- Docker Image: `benhajdahmenahmed/frontend`
- Build Output: `dist/angular/browser`
- CD Job: `frontend-cd`
- K8s: 2 replicas, NodePort service
- NodePort: 30080 (public access)
- No SonarQube analysis (frontend)

**Nginx Features:**
- Angular routing support (try_files)
- Gzip compression
- Static asset caching (1 year)
- Security headers

---

### 4. Productivity Microservice (Updated)
**Location:** `backend/microservices/Productivity/`

**Updates:**
- `k8s/deployment.yml` - Updated to 2 replicas, ClusterIP service
- Added proper database URL: `jdbc:mysql://mysql-service:3306/productivity`
- Updated Eureka URL: `http://eureka-service:8761/eureka/`

---

## Global Kubernetes Manifests
**Location:** `k8s/`

### namespace.yaml
- Creates `matchfreelance` namespace
- Labels: name=matchfreelance, environment=production

### mysql-deployment.yaml
- MySQL 8.0 deployment
- PersistentVolumeClaim: 10Gi storage
- Service name: `mysql-service`
- Root password: `pidev`
- Default database: `freelancing`
- Resources: 512Mi-1Gi memory, 500m-1000m CPU

### configmap.yaml
- Shared environment variables
- MySQL, Eureka, Config Server, Gateway configuration
- Spring profiles and connection strings

---

## CI Pipeline Structure (All Services)

### Stages:
1. **Checkout** - Clone repository
2. **Build** - Maven/npm compile
3. **Unit Tests** - Run tests with JUnit reports
4. **Code Coverage** - JaCoCo report generation (backend only)
5. **Package** - Create JAR/build artifacts
6. **SonarQube Analysis** - Code quality analysis (backend only)
7. **Quality Gate** - Wait for SonarQube gate (backend only)
8. **Docker Build** - Build and tag images
9. **Docker Push** - Push to DockerHub
10. **Archive Artifacts** - Store build artifacts
11. **Trigger CD** - Trigger CD pipeline

### Tools Used:
- JDK21 (backend)
- Maven3 (backend)
- NodeJS18 (frontend)

### Credentials:
- `dockerhub-credentials` - DockerHub login
- `sonarqube-token` - SonarQube authentication

---

## CD Pipeline Structure (All Services)

### Stages:
1. **Validate Parameters** - Check IMAGE_TAG parameter
2. **Pull Docker Image** - Pull from DockerHub
3. **Update Manifest & Push to Git** - GitOps workflow
   - Update image tag in K8s manifest
   - Commit and push to GitHub
4. **Sync ArgoCD** - Deploy to Kubernetes
   - Login to ArgoCD
   - Sync application
   - Wait for health check

### Environment Variables:
- `IMAGE_NAME` - DockerHub image name
- `SERVICE_NAME` - Kubernetes service name
- `NAMESPACE` - Kubernetes namespace (default)
- `ARGOCD_SERVER` - ArgoCD server (192.168.70.128:32345)
- `GIT_BRANCH` - Git branch (douja-with-productivity)
- `MANIFEST_PATH` - Path to K8s manifest

### Credentials:
- `dockerhub-credentials` - DockerHub login
- `github-credentials` - GitHub push access
- `argocd-credentials` - ArgoCD login

---

## Kubernetes Deployment Configuration

### Backend Microservices (Interview, User, Productivity):
- **Replicas:** 2
- **Service Type:** ClusterIP (internal only)
- **Image Pull Secret:** dockerhub-secret
- **Resources:**
  - Requests: 256Mi memory, 250m CPU
  - Limits: 512Mi memory, 500m CPU
- **Environment Variables:**
  - SPRING_PROFILES_ACTIVE=mysql
  - SPRING_DATASOURCE_URL (service-specific)
  - SPRING_DATASOURCE_USERNAME=root
  - SPRING_DATASOURCE_PASSWORD=pidev
  - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
  - CONFIG_SERVER_HOST (Interview, User only)

### Frontend:
- **Replicas:** 2
- **Service Type:** NodePort (external access)
- **NodePort:** 30080
- **Image Pull Secret:** dockerhub-secret
- **Resources:**
  - Requests: 128Mi memory, 100m CPU
  - Limits: 512Mi memory, 500m CPU

---

## Service URLs

### Internal Services (ClusterIP):
- Interview: `http://interview-service:8089`
- User: `http://user-service:8086`
- Productivity: `http://productivity-service:8087`
- MySQL: `mysql-service:3306`

### External Access (NodePort):
- Frontend: `http://192.168.70.128:30080`
- ArgoCD: `http://192.168.70.128:32345`

---

## Jenkins Job Names

### CI Jobs:
- `interview-service-ci`
- `user-service-ci`
- `productivity-service-ci`
- `frontend-ci`

### CD Jobs:
- `interview-service-cd`
- `user-service-cd`
- `productivity-service-cd`
- `frontend-cd`

---

## Docker Images

All images on DockerHub under `benhajdahmenahmed/`:
- `benhajdahmenahmed/interview:latest` (and versioned tags)
- `benhajdahmenahmed/user-service:latest` (and versioned tags)
- `benhajdahmenahmed/productivity:latest` (and versioned tags)
- `benhajdahmenahmed/frontend:latest` (and versioned tags)

---

## SonarQube Projects

- **Interview Service**
  - Project Key: `interview`
  - Project Name: "Interview Service"

- **User Service**
  - Project Key: `user-service`
  - Project Name: "User Service"

- **Productivity Service**
  - Project Key: `productivity`
  - Project Name: "Productivity Service"

---

## Code Coverage

All backend services configured with JaCoCo:
- Minimum line coverage: 50%
- Reports: `target/site/jacoco/index.html`
- XML reports sent to SonarQube
- Jenkins JaCoCo plugin integration

---

## GitOps Workflow

1. CI pipeline builds and pushes Docker image with version tag
2. CI triggers CD pipeline with IMAGE_TAG parameter
3. CD pulls Docker image from DockerHub
4. CD updates K8s manifest with new image tag
5. CD commits and pushes manifest to GitHub
6. CD triggers ArgoCD sync
7. ArgoCD detects manifest change and deploys to K8s
8. ArgoCD waits for deployment health check

---

## Prerequisites

### Jenkins Configuration:
- JDK21 tool configured
- Maven3 tool configured
- NodeJS18 tool configured
- SonarQube server configured

### Credentials in Jenkins:
- `dockerhub-credentials` (username/password)
- `github-credentials` (username/password)
- `sonarqube-token` (secret text)
- `argocd-credentials` (username/password)

### Kubernetes:
- Cluster accessible from Jenkins
- kubectl configured
- ArgoCD installed and accessible
- DockerHub secret created: `dockerhub-secret`

---

## Deployment Order

1. **Global Resources:**
   ```bash
   kubectl apply -f k8s/namespace.yaml
   kubectl apply -f k8s/configmap.yaml
   kubectl apply -f k8s/mysql-deployment.yaml
   ```

2. **Backend Services:**
   ```bash
   kubectl apply -f backend/microservices/Productivity/k8s/deployment.yml
   kubectl apply -f backend/microservices/Interview/k8s/deployment.yml
   kubectl apply -f backend/microservices/User/k8s/deployment.yml
   ```

3. **Frontend:**
   ```bash
   kubectl apply -f frontend/k8s/deployment.yml
   ```

---

## Monitoring & Observability

All backend services expose Prometheus metrics at:
- `/actuator/prometheus`
- `/actuator/health`
- `/actuator/info`

Configure Prometheus to scrape these endpoints for monitoring.

---

## Notes

- All pipelines follow the exact same structure as Productivity service
- No modifications were made to Productivity microservice code
- All services use Java 21 and Spring Boot 4.0.2 (Interview, User) or 3.2.2 (Productivity)
- Frontend uses Node 18 and Angular 21
- All services configured for MySQL with proper database names
- GitOps approach ensures all deployments are tracked in Git
- ArgoCD provides automated deployment and health monitoring

---

## Next Steps

1. Create Jenkins jobs for all CI/CD pipelines
2. Configure ArgoCD applications for each service
3. Set up Prometheus and Grafana for monitoring
4. Configure ingress controller for external access
5. Set up SSL/TLS certificates
6. Configure horizontal pod autoscaling
7. Set up backup strategy for MySQL PVC
8. Configure log aggregation (ELK/Loki)
