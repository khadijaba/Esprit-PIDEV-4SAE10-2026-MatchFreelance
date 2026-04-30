# CI/CD Quick Start Guide

## ✅ Files Created Successfully

All CI/CD pipeline files have been created and committed:

### Interview Service
- ✅ `backend/microservices/Interview/Jenkinsfile` - Complete CI/CD pipeline
- ✅ `backend/microservices/Interview/Dockerfile` - Multi-stage build with Java 21

### Productivity Service
- ✅ `backend/microservices/Productivity/Jenkinsfile` - Complete CI/CD pipeline
- ✅ `backend/microservices/Productivity/Dockerfile` - Multi-stage build with Java 21

### Documentation
- ✅ `CI_CD_SETUP_GUIDE.md` - Comprehensive setup instructions
- ✅ `CLEANUP_SUMMARY.md` - Workspace cleanup documentation

## Quick Setup Checklist

### 1. Jenkins Credentials (5 minutes)
```
☐ Add Docker Hub credentials
  - ID: docker-hub-credentials
  - Type: Username with password
  
☐ Verify SonarQube token (already configured)
  - ID: sonar-token
  - Type: Secret text
```

### 2. Jenkins Tools (Already Configured ✅)
```
✅ JDK21 - Java 21 installation
✅ Maven3 - Maven installation
✅ SonarQube Scanner
```

### 3. SonarQube Projects (2 minutes)
```
☐ Create project: interview
  - Project key: interview
  - Display name: Interview Service
  
☐ Create project: productivity
  - Project key: productivity
  - Display name: Productivity Service
```

### 4. Jenkins Pipeline Jobs (5 minutes each)

#### Interview Service Pipeline
```
Job Name: interview-service-pipeline
Type: Pipeline
SCM: Git
Repository: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git
Branch: */douja-with-productivity
Script Path: backend/microservices/Interview/Jenkinsfile
```

#### Productivity Service Pipeline
```
Job Name: productivity-service-pipeline
Type: Pipeline
SCM: Git
Repository: https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance.git
Branch: */douja-with-productivity
Script Path: backend/microservices/Productivity/Jenkinsfile
```

### 5. Docker Access (1 minute)
```bash
# In WSL terminal:
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins

# Or quick fix:
sudo chmod 666 /var/run/docker.sock
```

### 6. First Build (2 minutes)
```
1. Go to Jenkins dashboard
2. Click on interview-service-pipeline
3. Click "Build Now"
4. Monitor Console Output
5. Repeat for productivity-service-pipeline
```

## Pipeline Stages

Each pipeline runs these stages automatically:

1. **Checkout** - Clone repository
2. **Build & Test** - `mvn clean verify`
3. **SonarQube Analysis** - Code quality check
4. **Docker Build** - Create Docker image
5. **Docker Push** - Push to Docker Hub
6. **Cleanup** - Remove unused Docker resources

## Expected Results

### After Successful Build:

✅ **Jenkins**
- Build status: SUCCESS
- All stages green
- Console output shows no errors

✅ **SonarQube**
- Project appears in dashboard
- Code quality metrics displayed
- No critical issues

✅ **Docker Hub**
- New images pushed:
  - `khadijabenayed/interview:1.0.X`
  - `khadijabenayed/interview:latest`
  - `khadijabenayed/productivity:1.0.X`
  - `khadijabenayed/productivity:latest`

## Verify Everything Works

### Check Docker Images
```bash
docker images | grep interview
docker images | grep productivity
```

### Check SonarQube
```
Visit: http://localhost:9000
Navigate to: Projects
Look for: interview and productivity
```

### Check Jenkins Logs
```
Click on build number → Console Output
Look for: "Pipeline completed successfully!"
```

## Common Issues & Quick Fixes

### Issue: Docker permission denied
```bash
sudo chmod 666 /var/run/docker.sock
```

### Issue: Maven build fails
```
Check Java version in Jenkins:
Manage Jenkins > Tools > JDK installations
Verify JDK21 is configured
```

### Issue: SonarQube connection failed
```bash
# Check SonarQube is running:
docker ps | grep sonarqube

# Or start it:
docker start sonarqube
```

### Issue: Git checkout fails
```
Add GitHub credentials in Jenkins:
Manage Jenkins > Credentials > Add Credentials
Type: Username with password (or Personal Access Token)
```

## Next Steps

1. ✅ Push changes to GitHub
   ```bash
   git push origin douja-with-productivity
   ```

2. ⏳ Configure Jenkins credentials

3. ⏳ Create SonarQube projects

4. ⏳ Create Jenkins pipeline jobs

5. ⏳ Run first builds

6. ⏳ Monitor and verify

## Service Details

### Interview Service
- **Port**: 8085
- **Docker Image**: khadijabenayed/interview
- **SonarQube Key**: interview
- **Java Version**: 21

### Productivity Service
- **Port**: 8087
- **Docker Image**: khadijabenayed/productivity
- **SonarQube Key**: productivity
- **Java Version**: 21

## Support

For detailed instructions, see: `CI_CD_SETUP_GUIDE.md`

For troubleshooting, check:
1. Jenkins console output
2. SonarQube logs
3. Docker logs: `docker logs <container-id>`
4. Jenkins logs: `sudo tail -f /var/log/jenkins/jenkins.log`

## Summary

You now have complete CI/CD pipelines that will:
- ✅ Automatically build your code
- ✅ Run tests
- ✅ Analyze code quality
- ✅ Create Docker images
- ✅ Push to Docker Hub
- ✅ Clean up resources

Total setup time: ~20 minutes

Happy CI/CD! 🚀
