# Separate CI/CD Pipelines Guide

## Overview

This project uses **separate pipelines** for Continuous Integration (CI) and Continuous Deployment (CD):

- **CI Pipeline** (`Jenkinsfile.ci`): Build, Test, Quality Analysis, Docker Build & Push
- **CD Pipeline** (`Jenkinsfile.cd`): Deploy to Dev/Staging/Production environments

## Pipeline Files Structure

```
backend/microservices/
├── Interview/
│   ├── Jenkinsfile       # Default (redirects to CI)
│   ├── Jenkinsfile.ci    # ✅ Continuous Integration
│   └── Jenkinsfile.cd    # ✅ Continuous Deployment
└── Productivity/
    ├── Jenkinsfile       # Default (redirects to CI)
    ├── Jenkinsfile.ci    # ✅ Continuous Integration
    └── Jenkinsfile.cd    # ✅ Continuous Deployment
```

---

## CI Pipeline (Jenkinsfile.ci)

### Purpose
Automates the build, test, and packaging process. Runs on every code commit.

### Stages

1. **Checkout** - Clone repository
2. **Build** - Compile source code (`mvn clean compile`)
3. **Unit Tests** - Run tests (`mvn test`)
4. **Package** - Create JAR file (`mvn package`)
5. **SonarQube Analysis** - Code quality check
6. **Quality Gate** - Enforce quality standards
7. **Docker Build** - Create Docker image
8. **Docker Push** - Push to Docker Hub
9. **Archive Artifacts** - Save JAR files

### Triggers
- ✅ Automatic on Git push
- ✅ Scheduled builds
- ✅ Manual trigger

### Output
- Docker images: `khadijabenayed/interview:1.0.X` and `:latest`
- Docker images: `khadijabenayed/productivity:1.0.X` and `:latest`
- Archived JAR files
- SonarQube reports

---

## CD Pipeline (Jenkinsfile.cd)

### Purpose
Deploys Docker images to different environments. Runs manually or after successful CI.

### Stages

1. **Validate Parameters** - Check environment and image tag
2. **Pull Docker Image** - Get image from Docker Hub
3. **Stop Old Container** - Remove existing deployment
4. **Deploy to Environment** - Start new container
5. **Health Check** - Verify service is running
6. **Verify Deployment** - Check logs and status

### Deployment Environments

| Environment | Interview Port | Productivity Port | Approval Required |
|-------------|---------------|-------------------|-------------------|
| Development | 8085          | 8087              | No                |
| Staging     | 8185          | 8187              | No                |
| Production  | 8285          | 8287              | **Yes**           |

### Parameters

- **ENVIRONMENT**: Choose `dev`, `staging`, or `production`
- **IMAGE_TAG**: Specify version (default: `latest`)

### Triggers
- ✅ Manual with parameters
- ✅ Triggered after successful CI
- ✅ Scheduled deployments

---

## Jenkins Job Setup

### Step 1: Create CI Pipeline Jobs

#### Interview CI Job
```
Job Name: interview-service-ci
Type: Pipeline
Description: CI Pipeline for Interview Service

Pipeline Configuration:
- Definition: Pipeline script from SCM
- SCM: Git
- Repository URL: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git
- Branch: */douja-with-productivity
- Script Path: backend/microservices/Interview/Jenkinsfile.ci

Build Triggers:
☑ Poll SCM: H/5 * * * * (every 5 minutes)
☑ GitHub hook trigger for GITScm polling
```

#### Productivity CI Job
```
Job Name: productivity-service-ci
Type: Pipeline
Description: CI Pipeline for Productivity Service

Pipeline Configuration:
- Definition: Pipeline script from SCM
- SCM: Git
- Repository URL: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git
- Branch: */douja-with-productivity
- Script Path: backend/microservices/Productivity/Jenkinsfile.ci

Build Triggers:
☑ Poll SCM: H/5 * * * *
☑ GitHub hook trigger for GITScm polling
```

### Step 2: Create CD Pipeline Jobs

#### Interview CD Job
```
Job Name: interview-service-cd
Type: Pipeline
Description: CD Pipeline for Interview Service

Pipeline Configuration:
- Definition: Pipeline script from SCM
- SCM: Git
- Repository URL: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git
- Branch: */douja-with-productivity
- Script Path: backend/microservices/Interview/Jenkinsfile.cd

This project is parameterized:
☑ Yes (parameters defined in Jenkinsfile)
```

#### Productivity CD Job
```
Job Name: productivity-service-cd
Type: Pipeline
Description: CD Pipeline for Productivity Service

Pipeline Configuration:
- Definition: Pipeline script from SCM
- SCM: Git
- Repository URL: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git
- Branch: */douja-with-productivity
- Script Path: backend/microservices/Productivity/Jenkinsfile.cd

This project is parameterized:
☑ Yes (parameters defined in Jenkinsfile)
```

---

## Workflow Examples

### Scenario 1: Development Workflow

```bash
# Developer pushes code
git push origin douja-with-productivity

# CI Pipeline automatically triggers
1. Code is built and tested
2. SonarQube analyzes code quality
3. Docker image is created and pushed
4. Artifacts are archived

# Deploy to Development (Manual)
1. Go to Jenkins → interview-service-cd
2. Click "Build with Parameters"
3. Select ENVIRONMENT: dev
4. Select IMAGE_TAG: latest (or specific version)
5. Click "Build"
6. Service deploys to http://localhost:8085 (Interview)
   or http://localhost:8087 (Productivity)
```

### Scenario 2: Staging Deployment

```bash
# After successful CI build
1. Go to Jenkins → productivity-service-cd
2. Click "Build with Parameters"
3. Select ENVIRONMENT: staging
4. Select IMAGE_TAG: 1.0.42 (specific build)
5. Click "Build"
6. Service deploys to http://localhost:8187
```

### Scenario 3: Production Deployment

```bash
# Production deployment requires approval
1. Go to Jenkins → interview-service-cd
2. Click "Build with Parameters"
3. Select ENVIRONMENT: production
4. Select IMAGE_TAG: 1.0.50 (tested version)
5. Click "Build"
6. Pipeline pauses at "Deploy to Production" stage
7. Click "Deploy" to approve
8. Service deploys to http://localhost:8285
```

---

## CI Pipeline Features

### ✅ Automated Testing
- Unit tests run automatically
- Test results published in Jenkins
- Failed tests stop the pipeline

### ✅ Code Quality Gates
- SonarQube analysis on every build
- Quality gate must pass to continue
- Configurable quality thresholds

### ✅ Docker Image Versioning
- Each build creates versioned image: `1.0.${BUILD_NUMBER}`
- Latest tag always points to most recent build
- Easy rollback to previous versions

### ✅ Artifact Management
- JAR files archived in Jenkins
- Accessible for manual deployment
- Fingerprinting for traceability

---

## CD Pipeline Features

### ✅ Multi-Environment Support
- Development: Quick testing
- Staging: Pre-production validation
- Production: Live deployment

### ✅ Zero-Downtime Deployment
- Old container stopped gracefully
- New container starts immediately
- Health checks verify deployment

### ✅ Rollback Capability
- Deploy any previous version
- Automatic rollback on failure
- Manual rollback option

### ✅ Production Safety
- Manual approval required
- Deployment confirmation
- Automatic health checks

---

## Monitoring & Verification

### Check CI Build Status
```bash
# Jenkins Dashboard
- Green: Build successful
- Red: Build failed
- Yellow: Build unstable

# SonarQube Dashboard
http://localhost:9000
- Check code coverage
- Review quality metrics
- View technical debt
```

### Check CD Deployment Status
```bash
# View running containers
docker ps | grep interview-service
docker ps | grep productivity-service

# Check container logs
docker logs interview-service-dev
docker logs productivity-service-staging

# Test service endpoints
curl http://localhost:8085/actuator/health  # Interview Dev
curl http://localhost:8087/actuator/health  # Productivity Dev
curl http://localhost:8185/actuator/health  # Interview Staging
curl http://localhost:8187/actuator/health  # Productivity Staging
```

---

## Troubleshooting

### CI Pipeline Issues

#### Build Fails
```bash
# Check Maven logs in Jenkins console
# Common issues:
- Compilation errors
- Test failures
- Dependency problems

# Solution:
1. Fix code issues
2. Push to Git
3. CI automatically retries
```

#### SonarQube Quality Gate Fails
```bash
# Check SonarQube dashboard
# Common issues:
- Code coverage too low
- Too many bugs/vulnerabilities
- Code smells exceed threshold

# Solution:
1. Fix quality issues
2. Push to Git
3. CI automatically retries
```

#### Docker Push Fails
```bash
# Check Docker Hub credentials
# Verify in Jenkins:
Manage Jenkins → Credentials → docker-hub-credentials

# Test manually:
docker login
docker push khadijabenayed/interview:latest
```

### CD Pipeline Issues

#### Container Won't Start
```bash
# Check container logs
docker logs interview-service-dev

# Common issues:
- Port already in use
- Database connection failed
- Eureka registration failed

# Solution:
docker stop <conflicting-container>
# Retry deployment
```

#### Health Check Fails
```bash
# Check if service is running
docker ps | grep interview-service

# Check logs
docker logs --tail 50 interview-service-dev

# Common issues:
- Service taking too long to start
- Configuration errors
- Database not accessible
```

---

## Best Practices

### CI Pipeline
1. ✅ Run CI on every commit
2. ✅ Keep builds fast (<10 minutes)
3. ✅ Fix broken builds immediately
4. ✅ Monitor code quality trends
5. ✅ Archive important artifacts

### CD Pipeline
1. ✅ Test in Dev before Staging
2. ✅ Test in Staging before Production
3. ✅ Use specific versions in Production
4. ✅ Always verify health checks
5. ✅ Keep rollback plan ready

### General
1. ✅ Use semantic versioning
2. ✅ Tag releases in Git
3. ✅ Document deployment procedures
4. ✅ Monitor application logs
5. ✅ Regular security updates

---

## Quick Reference

### CI Pipeline Commands
```bash
# Trigger CI build manually
# Jenkins → interview-service-ci → Build Now

# View build logs
# Jenkins → interview-service-ci → #42 → Console Output

# Check artifacts
# Jenkins → interview-service-ci → #42 → Artifacts
```

### CD Pipeline Commands
```bash
# Deploy to Development
# Jenkins → interview-service-cd → Build with Parameters
# ENVIRONMENT: dev, IMAGE_TAG: latest

# Deploy to Staging
# Jenkins → interview-service-cd → Build with Parameters
# ENVIRONMENT: staging, IMAGE_TAG: 1.0.42

# Deploy to Production
# Jenkins → interview-service-cd → Build with Parameters
# ENVIRONMENT: production, IMAGE_TAG: 1.0.50
# Approve deployment when prompted
```

### Docker Commands
```bash
# List all service containers
docker ps -a | grep -E "interview|productivity"

# Stop all service containers
docker stop $(docker ps -q --filter name=interview-service)
docker stop $(docker ps -q --filter name=productivity-service)

# Remove all service containers
docker rm $(docker ps -aq --filter name=interview-service)
docker rm $(docker ps -aq --filter name=productivity-service)

# View logs
docker logs -f interview-service-dev
docker logs -f productivity-service-staging
```

---

## Summary

### CI Pipeline (Jenkinsfile.ci)
- ✅ Automated build and test
- ✅ Code quality analysis
- ✅ Docker image creation
- ✅ Artifact archiving
- ⏱️ Runs automatically on commit

### CD Pipeline (Jenkinsfile.cd)
- ✅ Multi-environment deployment
- ✅ Parameterized builds
- ✅ Health checks
- ✅ Rollback support
- ⏱️ Runs manually with parameters

### Total Jobs Created
- `interview-service-ci` - CI Pipeline
- `interview-service-cd` - CD Pipeline
- `productivity-service-ci` - CI Pipeline
- `productivity-service-cd` - CD Pipeline

**Total: 4 Jenkins Jobs** (2 CI + 2 CD)

---

## Next Steps

1. ✅ Commit and push Jenkinsfiles
2. ⏳ Create 4 Jenkins jobs (2 CI + 2 CD per service)
3. ⏳ Configure credentials
4. ⏳ Run first CI builds
5. ⏳ Deploy to Development
6. ⏳ Test and verify
7. ⏳ Deploy to Staging
8. ⏳ Deploy to Production

Happy CI/CD! 🚀
