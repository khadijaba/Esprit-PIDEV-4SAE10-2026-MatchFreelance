# User Microservice - DevOps CI/CD Setup

## Overview
This document describes the complete CI/CD pipeline setup for the User microservice using Jenkins, SonarQube, Docker, and Kubernetes.

## Infrastructure Components

### Jenkins
- **URL**: http://192.168.86.135:8080
- **Pipelines**:
  - `Jenkinsfile.ci` - Continuous Integration pipeline
  - `Jenkinsfile.cd` - Continuous Deployment pipeline

### SonarQube
- **URL**: http://sonarqube:9000
- **Project Key**: user-service

### Docker Registry
- **DockerHub Account**: amine002002
- **Image Name**: amine002002/user-service

### Kubernetes
- **Namespace**: devops
- **Service Port**: 8082

## Prerequisites

### Jenkins Configuration

#### 1. Credentials
Ensure the following credentials are configured in Jenkins:

```bash
# SonarQube Token (Secret Text)
ID: sonarqube-token
Description: SonarQube authentication token

# DockerHub Credentials (Username/Password)
ID: dockerhub-credentials
Username: amine002002
Password: <your-dockerhub-password>

# GitHub Credentials (Username/Password)
ID: github-credentials
Username: <your-github-username>
Password: <your-github-token>
```

#### 2. Tools Configuration
Configure the following tools in Jenkins (Manage Jenkins → Tools):

```yaml
JDK:
  Name: JDK21
  JAVA_HOME: /opt/java/openjdk

Maven:
  Name: Maven3
  Version: 3.9.9

SonarQube Scanner:
  Name: SonarQube-Scanner
  Version: Latest
```

#### 3. SonarQube Server Configuration
Configure SonarQube server in Jenkins (Manage Jenkins → Configure System):

```yaml
Name: SonarQube
Server URL: http://sonarqube:9000
Server authentication token: sonarqube-token (credential)
```

### Kubernetes Setup

#### 1. Create Namespace
```bash
kubectl apply -f k8s/namespace-devops.yaml
```

#### 2. Create ConfigMap
```bash
kubectl apply -f k8s/configmap-devops.yaml
```

#### 3. Create Secret
```bash
kubectl apply -f k8s/secret-devops.yaml
```

#### 4. Deploy Application
```bash
kubectl apply -f k8s/deployment-devops.yaml
kubectl apply -f k8s/service-devops.yaml
```

#### 5. Verify Deployment
```bash
# Check namespace
kubectl get namespace devops

# Check all resources
kubectl get all -n devops

# Check pods
kubectl get pods -n devops

# Check service
kubectl get svc -n devops

# Check deployment
kubectl get deployment user-service -n devops

# View logs
kubectl logs -f deployment/user-service -n devops
```

## CI Pipeline (Jenkinsfile.ci)

### Pipeline Stages

1. **Checkout**
   - Clones the repository from GitHub (User branch)
   - Uses github-credentials

2. **Build & Unit Tests**
   - Executes: `mvn clean verify`
   - Runs unit tests
   - Generates test reports

3. **JaCoCo Code Coverage Report**
   - Publishes code coverage metrics
   - Pattern: `**/target/jacoco.exec`
   - Excludes: config, dto, entity, exception packages

4. **SonarQube Analysis**
   - Performs static code analysis
   - Sends results to SonarQube server
   - Project key: user-service

5. **Quality Gate**
   - Waits for SonarQube quality gate result
   - Timeout: 5 minutes
   - Aborts pipeline if quality gate fails

6. **Docker Build**
   - Builds Docker image
   - Tags: `BUILD_NUMBER` and `latest`

7. **Docker Push**
   - Pushes images to DockerHub
   - Repository: amine002002/user-service

### Post Actions
- Archives JaCoCo reports
- Publishes JUnit test results
- Cleans up Docker images

### Running the CI Pipeline

```bash
# From Jenkins UI
1. Create new Pipeline job
2. Configure SCM: Git
3. Repository URL: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance
4. Branch: User
5. Script Path: User/Jenkinsfile.ci
6. Save and Build
```

## CD Pipeline (Jenkinsfile.cd)

### Pipeline Parameters

- **IMAGE_TAG**: Docker image tag to deploy (default: latest)

### Pipeline Stages

1. **Checkout**
   - Clones Kubernetes manifests from repository

2. **Deploy to Kubernetes**
   - Updates deployment with new image
   - Command: `kubectl set image deployment/user-service user-service=amine002002/user-service:${IMAGE_TAG} -n devops`

3. **Verify Rollout**
   - Waits for rollout completion (timeout: 5 minutes)
   - Displays deployment, pod, and service status

### Post Actions
- **Success**: Displays deployment information
- **Failure**: Automatically rolls back to previous version
- **Always**: Shows deployment logs

### Running the CD Pipeline

```bash
# From Jenkins UI
1. Create new Pipeline job
2. Configure SCM: Git
3. Repository URL: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance
4. Branch: User
5. Script Path: User/Jenkinsfile.cd
6. Save and Build with Parameters
7. Enter IMAGE_TAG (e.g., latest, 123, v1.0.0)
```

## Code Coverage with JaCoCo

### Maven Configuration
The `pom.xml` includes JaCoCo plugin with the following configuration:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Coverage Reports
- **Execution Data**: `target/jacoco.exec`
- **HTML Report**: `target/site/jacoco/index.html`
- **XML Report**: `target/site/jacoco/jacoco.xml`

### Local Testing
```bash
# Run tests with coverage
mvn clean verify

# View coverage report
open target/site/jacoco/index.html
```

## Kubernetes Resources

### Deployment Configuration
- **Name**: user-service
- **Namespace**: devops
- **Replicas**: 1
- **Image**: amine002002/user-service:latest
- **Image Pull Policy**: Always
- **Container Port**: 8082

### Resource Limits
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

### Health Checks
- **Liveness Probe**: `/actuator/health/liveness` (port 8082)
- **Readiness Probe**: `/actuator/health/readiness` (port 8082)
- **Startup Probe**: `/actuator/health` (port 8082)

### Service Configuration
- **Type**: ClusterIP
- **Port**: 8082
- **Target Port**: 8082

### Environment Variables

#### From ConfigMap (user-service-config)
- `SPRING_DATASOURCE_URL`: jdbc:mysql://mysql:3306/UserDB
- `SERVER_PORT`: 8082
- `SPRING_APPLICATION_NAME`: user-service
- `SPRING_PROFILES_ACTIVE`: production

#### From Secret (user-service-secret)
- `SPRING_DATASOURCE_USERNAME`: pidev (base64 encoded)
- `SPRING_DATASOURCE_PASSWORD`: pidev (base64 encoded)

## Monitoring

### Prometheus Metrics
The service exposes Prometheus metrics at:
```
http://user-service:8082/actuator/prometheus
```

### Actuator Endpoints
Available endpoints:
- `/actuator/health` - Health check
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## Troubleshooting

### CI Pipeline Issues

#### Build Failures
```bash
# Check Maven logs in Jenkins console output
# Verify JDK and Maven tool configurations
# Ensure dependencies are accessible
```

#### SonarQube Connection Issues
```bash
# Verify SonarQube server is running
curl http://sonarqube:9000/api/system/status

# Check SonarQube token in Jenkins credentials
# Verify SonarQube server configuration in Jenkins
```

#### Docker Build/Push Failures
```bash
# Verify DockerHub credentials
# Check Docker daemon is running on Jenkins agent
docker info

# Test Docker login
docker login -u amine002002
```

### CD Pipeline Issues

#### Deployment Failures
```bash
# Check Kubernetes cluster connectivity
kubectl cluster-info

# Verify namespace exists
kubectl get namespace devops

# Check deployment status
kubectl describe deployment user-service -n devops

# View pod logs
kubectl logs -f deployment/user-service -n devops
```

#### Image Pull Errors
```bash
# Verify image exists in DockerHub
docker pull amine002002/user-service:latest

# Check image pull policy
kubectl get deployment user-service -n devops -o yaml | grep imagePullPolicy
```

#### Health Check Failures
```bash
# Check if application is running
kubectl get pods -n devops

# View pod events
kubectl describe pod <pod-name> -n devops

# Check application logs
kubectl logs <pod-name> -n devops

# Test health endpoint
kubectl port-forward deployment/user-service 8082:8082 -n devops
curl http://localhost:8082/actuator/health
```

### Database Connection Issues
```bash
# Verify MySQL service is running
kubectl get svc mysql -n devops

# Check database credentials in secret
kubectl get secret user-service-secret -n devops -o yaml

# Test database connection from pod
kubectl exec -it <pod-name> -n devops -- sh
# Inside pod:
# curl http://mysql:3306 (should get MySQL handshake)
```

## Security Considerations

### Secrets Management
- Database credentials are stored in Kubernetes secrets
- Secrets are base64 encoded (not encrypted)
- For production, consider using:
  - HashiCorp Vault
  - AWS Secrets Manager
  - Sealed Secrets
  - External Secrets Operator

### Container Security
- Running as non-root user (UID: 1000)
- Security context configured
- Resource limits enforced

### Network Security
- Service type: ClusterIP (internal only)
- No external exposure by default
- Use Ingress or LoadBalancer for external access

## Maintenance

### Updating the Application
```bash
# 1. Make code changes
# 2. Commit and push to User branch
# 3. Run CI pipeline to build new image
# 4. Run CD pipeline with new IMAGE_TAG
```

### Scaling the Deployment
```bash
# Scale to 3 replicas
kubectl scale deployment user-service --replicas=3 -n devops

# Or update deployment-devops.yaml and apply
kubectl apply -f k8s/deployment-devops.yaml
```

### Rolling Back
```bash
# View rollout history
kubectl rollout history deployment/user-service -n devops

# Rollback to previous version
kubectl rollout undo deployment/user-service -n devops

# Rollback to specific revision
kubectl rollout undo deployment/user-service --to-revision=2 -n devops
```

### Viewing Logs
```bash
# Current logs
kubectl logs -f deployment/user-service -n devops

# Previous logs (if pod crashed)
kubectl logs deployment/user-service -n devops --previous

# Logs from specific pod
kubectl logs <pod-name> -n devops

# Logs with timestamp
kubectl logs deployment/user-service -n devops --timestamps
```

## Complete Deployment Workflow

### Initial Setup
```bash
# 1. Create namespace
kubectl apply -f k8s/namespace-devops.yaml

# 2. Create ConfigMap
kubectl apply -f k8s/configmap-devops.yaml

# 3. Create Secret
kubectl apply -f k8s/secret-devops.yaml

# 4. Deploy application
kubectl apply -f k8s/deployment-devops.yaml

# 5. Create service
kubectl apply -f k8s/service-devops.yaml

# 6. Verify deployment
kubectl get all -n devops
```

### CI/CD Workflow
```bash
# 1. Developer commits code to User branch
git add .
git commit -m "Feature: Add new endpoint"
git push origin User

# 2. Trigger CI pipeline in Jenkins
# - Builds application
# - Runs tests
# - Generates coverage report
# - Performs SonarQube analysis
# - Builds Docker image
# - Pushes to DockerHub

# 3. Trigger CD pipeline in Jenkins
# - Deploys new image to Kubernetes
# - Verifies rollout
# - Monitors health checks

# 4. Verify deployment
kubectl get pods -n devops
kubectl logs -f deployment/user-service -n devops
```

## Contact & Support
- **DevOps Team**: devops-team@esprit.tn
- **Repository**: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance
- **Branch**: User
