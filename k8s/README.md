# Kubernetes Manifests - User Microservice (DevOps Namespace)

## Overview
This directory contains Kubernetes manifests for deploying the User microservice to the `devops` namespace.

## Files

### Core Manifests
- **namespace-devops.yaml** - Creates the `devops` namespace
- **configmap-devops.yaml** - Application configuration (non-sensitive)
- **secret-devops.yaml** - Database credentials (base64 encoded)
- **deployment-devops.yaml** - Deployment specification
- **service-devops.yaml** - Service (ClusterIP) specification

### Deployment Scripts
- **apply-all.sh** - Bash script to deploy all resources (Linux/Mac)
- **apply-all.bat** - Batch script to deploy all resources (Windows)

### Legacy Manifests (Other Namespaces)
- **namespace.yaml** - Original namespaces (microservices, microservices-staging)
- **configmap.yaml** - Original ConfigMaps
- **secret-template.yaml** - Secret template for other namespaces

## Quick Start

### Prerequisites
1. kubectl installed and configured
2. Access to Kubernetes cluster
3. MySQL database running at `mysql:3306` with database `UserDB`

### Deploy All Resources

#### Linux/Mac
```bash
cd User/k8s
chmod +x apply-all.sh
./apply-all.sh
```

#### Windows
```cmd
cd User\k8s
apply-all.bat
```

#### Manual Deployment
```bash
# Apply manifests in order
kubectl apply -f namespace-devops.yaml
kubectl apply -f configmap-devops.yaml
kubectl apply -f secret-devops.yaml
kubectl apply -f deployment-devops.yaml
kubectl apply -f service-devops.yaml
```

## Configuration Details

### Namespace
```yaml
Name: devops
Labels:
  - environment: development
  - managed-by: jenkins
```

### ConfigMap (user-service-config)
Contains non-sensitive configuration:
- Database URL: `jdbc:mysql://mysql:3306/UserDB`
- Server port: `8082`
- Spring profiles: `production`
- JPA settings
- Actuator endpoints
- Logging configuration

### Secret (user-service-secret)
Contains sensitive data (base64 encoded):
- Database username: `pidev`
- Database password: `pidev`

**Note**: These are default credentials. For production, update with secure values:
```bash
# Encode new credentials
echo -n 'your-username' | base64
echo -n 'your-password' | base64

# Update secret-devops.yaml with encoded values
# Then apply: kubectl apply -f secret-devops.yaml
```

### Deployment (user-service)
- **Replicas**: 1
- **Image**: amine002002/user-service:latest
- **Image Pull Policy**: Always
- **Container Port**: 8082
- **Resources**:
  - Requests: 512Mi memory, 250m CPU
  - Limits: 1Gi memory, 500m CPU

#### Health Checks
- **Liveness Probe**: `/actuator/health/liveness`
  - Initial delay: 60s
  - Period: 10s
  - Timeout: 5s
  - Failure threshold: 3

- **Readiness Probe**: `/actuator/health/readiness`
  - Initial delay: 30s
  - Period: 10s
  - Timeout: 5s
  - Failure threshold: 3

- **Startup Probe**: `/actuator/health`
  - Initial delay: 10s
  - Period: 10s
  - Timeout: 5s
  - Failure threshold: 30

### Service (user-service)
- **Type**: ClusterIP (internal only)
- **Port**: 8082
- **Target Port**: 8082
- **Selector**: app=user-service

## Verification Commands

### Check All Resources
```bash
kubectl get all -n devops
```

### Check Specific Resources
```bash
# Namespace
kubectl get namespace devops

# ConfigMap
kubectl get configmap user-service-config -n devops
kubectl describe configmap user-service-config -n devops

# Secret
kubectl get secret user-service-secret -n devops
kubectl describe secret user-service-secret -n devops

# Deployment
kubectl get deployment user-service -n devops
kubectl describe deployment user-service -n devops

# Pods
kubectl get pods -n devops
kubectl get pods -l app=user-service -n devops

# Service
kubectl get service user-service -n devops
kubectl describe service user-service -n devops
```

### View Logs
```bash
# Follow logs
kubectl logs -f deployment/user-service -n devops

# View logs from specific pod
kubectl logs <pod-name> -n devops

# View previous logs (if pod crashed)
kubectl logs <pod-name> -n devops --previous

# Logs with timestamps
kubectl logs deployment/user-service -n devops --timestamps
```

### Debug Pod Issues
```bash
# Describe pod
kubectl describe pod <pod-name> -n devops

# Get pod events
kubectl get events -n devops --sort-by='.lastTimestamp'

# Execute command in pod
kubectl exec -it <pod-name> -n devops -- sh

# Port forward to local machine
kubectl port-forward deployment/user-service 8082:8082 -n devops
# Then access: http://localhost:8082/actuator/health
```

## Updating the Deployment

### Update Image Tag
```bash
# Using kubectl set image
kubectl set image deployment/user-service \
  user-service=amine002002/user-service:v1.2.3 \
  -n devops

# Or edit deployment directly
kubectl edit deployment user-service -n devops
```

### Update ConfigMap
```bash
# Edit configmap-devops.yaml
# Then apply changes
kubectl apply -f configmap-devops.yaml

# Restart pods to pick up new config
kubectl rollout restart deployment/user-service -n devops
```

### Update Secret
```bash
# Edit secret-devops.yaml (remember to base64 encode values)
# Then apply changes
kubectl apply -f secret-devops.yaml

# Restart pods to pick up new secret
kubectl rollout restart deployment/user-service -n devops
```

## Scaling

### Manual Scaling
```bash
# Scale to 3 replicas
kubectl scale deployment user-service --replicas=3 -n devops

# Verify scaling
kubectl get pods -n devops
```

### Update Deployment Manifest
```bash
# Edit deployment-devops.yaml
# Change spec.replicas to desired number
# Then apply
kubectl apply -f deployment-devops.yaml
```

## Rollout Management

### View Rollout Status
```bash
kubectl rollout status deployment/user-service -n devops
```

### View Rollout History
```bash
kubectl rollout history deployment/user-service -n devops
```

### Rollback to Previous Version
```bash
kubectl rollout undo deployment/user-service -n devops
```

### Rollback to Specific Revision
```bash
# View history with revisions
kubectl rollout history deployment/user-service -n devops

# Rollback to specific revision
kubectl rollout undo deployment/user-service --to-revision=2 -n devops
```

### Pause/Resume Rollout
```bash
# Pause rollout
kubectl rollout pause deployment/user-service -n devops

# Resume rollout
kubectl rollout resume deployment/user-service -n devops
```

## Monitoring

### Check Resource Usage
```bash
# Pod resource usage
kubectl top pods -n devops

# Node resource usage
kubectl top nodes
```

### Access Actuator Endpoints
```bash
# Port forward to access actuator
kubectl port-forward deployment/user-service 8082:8082 -n devops

# Then access endpoints:
# http://localhost:8082/actuator/health
# http://localhost:8082/actuator/info
# http://localhost:8082/actuator/metrics
# http://localhost:8082/actuator/prometheus
```

### Prometheus Metrics
The service is annotated for Prometheus scraping:
```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8082"
  prometheus.io/path: "/actuator/prometheus"
```

## Cleanup

### Delete All Resources
```bash
# Delete entire namespace (removes all resources)
kubectl delete namespace devops
```

### Delete Individual Resources
```bash
# Delete service
kubectl delete service user-service -n devops

# Delete deployment
kubectl delete deployment user-service -n devops

# Delete configmap
kubectl delete configmap user-service-config -n devops

# Delete secret
kubectl delete secret user-service-secret -n devops
```

## Troubleshooting

### Pod Not Starting
```bash
# Check pod status
kubectl get pods -n devops

# Describe pod for events
kubectl describe pod <pod-name> -n devops

# Check logs
kubectl logs <pod-name> -n devops

# Common issues:
# - Image pull errors: Check image name and DockerHub
# - CrashLoopBackOff: Check application logs
# - Pending: Check resource availability
```

### Health Check Failures
```bash
# Check if actuator endpoints are accessible
kubectl port-forward <pod-name> 8082:8082 -n devops
curl http://localhost:8082/actuator/health

# Check probe configuration
kubectl describe pod <pod-name> -n devops | grep -A 10 "Liveness\|Readiness"

# Adjust probe timings if needed
# Edit deployment-devops.yaml and increase initialDelaySeconds
```

### Database Connection Issues
```bash
# Check if MySQL service exists
kubectl get svc mysql -n devops

# Test DNS resolution from pod
kubectl exec -it <pod-name> -n devops -- nslookup mysql

# Check database credentials
kubectl get secret user-service-secret -n devops -o yaml

# View decoded secret
kubectl get secret user-service-secret -n devops -o jsonpath='{.data.SPRING_DATASOURCE_USERNAME}' | base64 -d
kubectl get secret user-service-secret -n devops -o jsonpath='{.data.SPRING_DATASOURCE_PASSWORD}' | base64 -d
```

### Service Not Accessible
```bash
# Check service endpoints
kubectl get endpoints user-service -n devops

# Check if pods are ready
kubectl get pods -l app=user-service -n devops

# Test service from another pod
kubectl run test-pod --image=curlimages/curl -it --rm -n devops -- sh
# Inside pod:
curl http://user-service:8082/actuator/health
```

## Security Best Practices

### Secrets Management
- ✅ Use Kubernetes secrets for sensitive data
- ✅ Base64 encode secret values
- ⚠️ Consider using external secret management:
  - HashiCorp Vault
  - AWS Secrets Manager
  - Azure Key Vault
  - Sealed Secrets
  - External Secrets Operator

### Container Security
- ✅ Run as non-root user (UID: 1000)
- ✅ Security context configured
- ✅ Resource limits enforced
- ✅ Health checks configured

### Network Security
- ✅ ClusterIP service (internal only)
- ⚠️ Use NetworkPolicies to restrict traffic
- ⚠️ Use Ingress with TLS for external access

## Integration with CI/CD

### Jenkins CD Pipeline
The CD pipeline (Jenkinsfile.cd) uses these manifests to deploy:

```groovy
kubectl set image deployment/user-service \
  user-service=amine002002/user-service:${IMAGE_TAG} \
  -n devops
```

### Manual Deployment from CI/CD
```bash
# After building Docker image in CI
docker build -t amine002002/user-service:${BUILD_NUMBER} .
docker push amine002002/user-service:${BUILD_NUMBER}

# Deploy to Kubernetes
kubectl set image deployment/user-service \
  user-service=amine002002/user-service:${BUILD_NUMBER} \
  -n devops

# Wait for rollout
kubectl rollout status deployment/user-service -n devops
```

## Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus Monitoring](https://prometheus.io/docs/introduction/overview/)

## Support
For issues or questions, contact the DevOps team at devops-team@esprit.tn
