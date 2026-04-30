# CI/CD Pipeline Setup Guide for Interview & Productivity Services

## Overview
This guide will help you set up Jenkins CI/CD pipelines for the Interview and Productivity microservices using your WSL environment with Docker, Jenkins, SonarQube, Git, and Java 21.

## Prerequisites ✅
- [x] WSL installed on Windows
- [x] Docker installed in WSL
- [x] Jenkins installed in WSL
- [x] SonarQube installed in WSL
- [x] Git installed in WSL
- [x] Java 21 installed in WSL
- [x] SonarQube credentials configured in Jenkins
- [x] Java 21 tool configured in Jenkins
- [x] SonarQube tool configured in Jenkins

## Services Configuration

### Interview Service
- **Port**: 8085
- **Docker Image**: `khadijabenayed/interview`
- **SonarQube Project Key**: `interview`
- **Artifact**: `interview-service-0.0.1-SNAPSHOT.jar`

### Productivity Service
- **Port**: 8087
- **Docker Image**: `khadijabenayed/productivity`
- **SonarQube Project Key**: `productivity`
- **Artifact**: `productivity-service-1.0.0.jar`

## Step 1: Configure Jenkins Credentials

### 1.1 Docker Hub Credentials
```bash
# In Jenkins UI:
# Navigate to: Manage Jenkins > Credentials > System > Global credentials
# Click "Add Credentials"
```

**Add Docker Hub Credentials:**
- Kind: `Username with password`
- Scope: `Global`
- Username: `<your-dockerhub-username>`
- Password: `<your-dockerhub-password>`
- ID: `docker-hub-credentials`
- Description: `Docker Hub Credentials`

### 1.2 SonarQube Token (Already Configured ✅)
- Kind: `Secret text`
- Scope: `Global`
- Secret: `<your-sonarqube-token>`
- ID: `sonar-token`
- Description: `SonarQube Authentication Token`

## Step 2: Configure Jenkins Tools

### 2.1 Java 21 Configuration (Already Configured ✅)
```bash
# In Jenkins UI:
# Navigate to: Manage Jenkins > Tools > JDK installations
```

**Verify JDK21 Configuration:**
- Name: `JDK21`
- JAVA_HOME: `/usr/lib/jvm/java-21-openjdk-amd64` (or your Java 21 path)
- Install automatically: Unchecked (if manually installed)

### 2.2 Maven Configuration
```bash
# Navigate to: Manage Jenkins > Tools > Maven installations
```

**Add Maven3:**
- Name: `Maven3`
- Install automatically: Checked
- Version: `3.9.9` (or latest)

### 2.3 SonarQube Scanner (Already Configured ✅)
```bash
# Navigate to: Manage Jenkins > Tools > SonarQube Scanner installations
```

**Verify Configuration:**
- Name: `SonarScanner`
- Install automatically: Checked
- Version: Latest

## Step 3: Configure SonarQube Server in Jenkins

```bash
# Navigate to: Manage Jenkins > System > SonarQube servers
```

**Add SonarQube Server:**
- Name: `SonarQube`
- Server URL: `http://localhost:9000`
- Server authentication token: Select `sonar-token` credential

## Step 4: Create SonarQube Projects

### 4.1 Create Interview Project in SonarQube
```bash
# Access SonarQube UI: http://localhost:9000
# Login with admin credentials
# Navigate to: Projects > Create Project
```

**Interview Project:**
- Project key: `interview`
- Display name: `Interview Service`
- Main branch name: `main` or `douja`

### 4.2 Create Productivity Project in SonarQube
**Productivity Project:**
- Project key: `productivity`
- Display name: `Productivity Service`
- Main branch name: `main` or `douja`

## Step 5: Create Jenkins Pipeline Jobs

### 5.1 Create Interview Pipeline Job

```bash
# In Jenkins UI:
# Click "New Item"
```

**Job Configuration:**
1. **Item name**: `interview-service-pipeline`
2. **Type**: `Pipeline`
3. **Click OK**

**Pipeline Configuration:**
- **Description**: `CI/CD Pipeline for Interview Microservice`
- **Build Triggers**: 
  - ☑ Poll SCM: `H/5 * * * *` (every 5 minutes)
  - OR ☑ GitHub hook trigger for GITScm polling
- **Pipeline**:
  - Definition: `Pipeline script from SCM`
  - SCM: `Git`
  - Repository URL: `https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git`
  - Credentials: Add your GitHub credentials
  - Branch: `*/douja` (or your target branch)
  - Script Path: `backend/microservices/Interview/Jenkinsfile`

### 5.2 Create Productivity Pipeline Job

**Job Configuration:**
1. **Item name**: `productivity-service-pipeline`
2. **Type**: `Pipeline`
3. **Click OK**

**Pipeline Configuration:**
- **Description**: `CI/CD Pipeline for Productivity Microservice`
- **Build Triggers**: 
  - ☑ Poll SCM: `H/5 * * * *`
  - OR ☑ GitHub hook trigger for GITScm polling
- **Pipeline**:
  - Definition: `Pipeline script from SCM`
  - SCM: `Git`
  - Repository URL: `https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git`
  - Credentials: Add your GitHub credentials
  - Branch: `*/douja`
  - Script Path: `backend/microservices/Productivity/Jenkinsfile`

## Step 6: Verify Docker Access in Jenkins

### 6.1 Add Jenkins User to Docker Group (WSL)
```bash
# In WSL terminal:
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins

# Or if using Docker Desktop:
sudo chmod 666 /var/run/docker.sock
```

### 6.2 Test Docker Access
```bash
# In Jenkins job console or Jenkins script console:
docker --version
docker ps
```

## Step 7: Pipeline Stages Explained

### Stage 1: Checkout
- Clones the Git repository
- Checks out the specified branch

### Stage 2: Build & Test
- Runs `mvn clean verify`
- Compiles code
- Runs unit tests
- Creates JAR artifact

### Stage 3: SonarQube Analysis
- Performs code quality analysis
- Checks for bugs, vulnerabilities, code smells
- Sends results to SonarQube server
- Uses project keys: `interview` and `productivity`

### Stage 4: Docker Build
- Builds Docker image using multi-stage Dockerfile
- Tags image with build number: `1.0.${BUILD_NUMBER}`
- Tags image as `latest`

### Stage 5: Docker Push
- Authenticates with Docker Hub
- Pushes versioned image
- Pushes latest image

### Post Actions
- **Success**: Logs success message
- **Failure**: Logs failure message
- **Always**: Cleans up Docker system

## Step 8: Run Your First Build

### 8.1 Manual Build
1. Go to Jenkins dashboard
2. Click on `interview-service-pipeline`
3. Click `Build Now`
4. Monitor the build in `Console Output`

### 8.2 Verify Build Success
```bash
# Check Docker images:
docker images | grep interview
docker images | grep productivity

# Check SonarQube:
# Visit: http://localhost:9000
# Navigate to Projects > interview or productivity
```

## Step 9: Troubleshooting

### Issue 1: Maven Build Fails
```bash
# Check Java version in Jenkins:
java -version

# Should show Java 21
# If not, verify JDK21 tool configuration
```

### Issue 2: Docker Permission Denied
```bash
# In WSL:
sudo chmod 666 /var/run/docker.sock

# Or add jenkins to docker group:
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### Issue 3: SonarQube Connection Failed
```bash
# Check SonarQube is running:
docker ps | grep sonarqube

# Or if running as service:
sudo systemctl status sonarqube

# Verify SonarQube URL in Jenkins:
# Should be: http://localhost:9000
```

### Issue 4: Docker Hub Push Fails
```bash
# Verify credentials in Jenkins
# Test manual login:
docker login

# Check image name format:
# Should be: dockerhub-username/image-name:tag
```

### Issue 5: Git Checkout Fails
```bash
# Add GitHub credentials in Jenkins
# Or use HTTPS with personal access token
# Or configure SSH keys
```

## Step 10: Pipeline Monitoring

### 10.1 Jenkins Dashboard
- View build history
- Check build status (Success/Failure)
- View console output
- Check build duration

### 10.2 SonarQube Dashboard
- Code coverage
- Code quality metrics
- Bugs and vulnerabilities
- Technical debt

### 10.3 Docker Hub
- Verify images are pushed
- Check image tags
- View image size

## Step 11: Advanced Configuration (Optional)

### 11.1 Add Email Notifications
```groovy
post {
    failure {
        emailext (
            subject: "Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
            body: "Check console output at ${env.BUILD_URL}",
            to: "your-email@example.com"
        )
    }
}
```

### 11.2 Add Slack Notifications
```groovy
post {
    success {
        slackSend (
            color: 'good',
            message: "Build Successful: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
        )
    }
}
```

### 11.3 Add Quality Gate Check
```groovy
stage('Quality Gate') {
    steps {
        timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate abortPipeline: true
        }
    }
}
```

## Step 12: Continuous Deployment (Optional)

### 12.1 Add Deployment Stage
```groovy
stage('Deploy to Dev') {
    steps {
        sh '''
            docker-compose -f docker-compose.dev.yml up -d interview
            # or
            kubectl apply -f k8s/interview-deployment.yaml
        '''
    }
}
```

## Quick Reference Commands

### Start Services in WSL
```bash
# Start Docker
sudo service docker start

# Start Jenkins
sudo systemctl start jenkins

# Start SonarQube (if using Docker)
docker start sonarqube

# Check status
sudo systemctl status jenkins
docker ps
```

### View Logs
```bash
# Jenkins logs
sudo tail -f /var/log/jenkins/jenkins.log

# Docker logs
docker logs -f <container-id>

# SonarQube logs
docker logs -f sonarqube
```

### Useful Jenkins CLI Commands
```bash
# Trigger build
java -jar jenkins-cli.jar -s http://localhost:8080/ build interview-service-pipeline

# Get build status
java -jar jenkins-cli.jar -s http://localhost:8080/ get-job interview-service-pipeline
```

## Pipeline Files Created

1. ✅ `backend/microservices/Interview/Jenkinsfile`
2. ✅ `backend/microservices/Productivity/Jenkinsfile`
3. ✅ `backend/microservices/Interview/Dockerfile` (Updated to Java 21)
4. ✅ `backend/microservices/Productivity/Dockerfile` (Updated to Java 21)

## Next Steps

1. ✅ Commit and push the Jenkinsfiles to your repository
2. ⏳ Configure Jenkins credentials (Docker Hub)
3. ⏳ Create SonarQube projects
4. ⏳ Create Jenkins pipeline jobs
5. ⏳ Run first builds
6. ⏳ Monitor and verify results

## Support

If you encounter any issues:
1. Check Jenkins console output
2. Check SonarQube logs
3. Check Docker logs
4. Verify all credentials are correct
5. Ensure all services are running

## Summary

You now have:
- ✅ Complete CI/CD pipelines for Interview and Productivity services
- ✅ Multi-stage Docker builds using Java 21
- ✅ SonarQube code quality analysis
- ✅ Automated Docker image building and pushing
- ✅ Post-build cleanup
- ✅ Comprehensive error handling

Your pipelines will automatically:
1. Build and test your code
2. Analyze code quality
3. Create Docker images
4. Push to Docker Hub
5. Clean up resources

Happy CI/CD! 🚀
