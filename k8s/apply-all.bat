@echo off
REM ========================================
REM Kubernetes Deployment Script
REM User Microservice - DevOps Namespace
REM ========================================

echo ==========================================
echo Deploying User Microservice to Kubernetes
echo Namespace: devops
echo ==========================================
echo.

REM Check if kubectl is installed
kubectl version --client >nul 2>&1
if errorlevel 1 (
    echo [ERROR] kubectl is not installed. Please install kubectl first.
    exit /b 1
)

REM Check if cluster is accessible
kubectl cluster-info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Cannot connect to Kubernetes cluster. Please check your kubeconfig.
    exit /b 1
)

echo [SUCCESS] Connected to Kubernetes cluster
echo.

REM Step 1: Create Namespace
echo [INFO] Creating namespace...
kubectl apply -f namespace-devops.yaml
if errorlevel 1 (
    echo [ERROR] Failed to create namespace
    exit /b 1
)
echo [SUCCESS] Namespace created/updated
echo.

REM Step 2: Create ConfigMap
echo [INFO] Creating ConfigMap...
kubectl apply -f configmap-devops.yaml
if errorlevel 1 (
    echo [ERROR] Failed to create ConfigMap
    exit /b 1
)
echo [SUCCESS] ConfigMap created/updated
echo.

REM Step 3: Create Secret
echo [INFO] Creating Secret...
kubectl apply -f secret-devops.yaml
if errorlevel 1 (
    echo [ERROR] Failed to create Secret
    exit /b 1
)
echo [SUCCESS] Secret created/updated
echo.

REM Step 4: Deploy Application
echo [INFO] Deploying application...
kubectl apply -f deployment-devops.yaml
if errorlevel 1 (
    echo [ERROR] Failed to create Deployment
    exit /b 1
)
echo [SUCCESS] Deployment created/updated
echo.

REM Step 5: Create Service
echo [INFO] Creating service...
kubectl apply -f service-devops.yaml
if errorlevel 1 (
    echo [ERROR] Failed to create Service
    exit /b 1
)
echo [SUCCESS] Service created/updated
echo.

REM Wait for deployment to be ready
echo [INFO] Waiting for deployment to be ready...
kubectl wait --for=condition=available --timeout=300s deployment/user-service -n devops
if errorlevel 1 (
    echo [WARNING] Deployment may not be ready yet
)
echo.

REM Display deployment status
echo ==========================================
echo Deployment Status
echo ==========================================
echo.

echo [INFO] Namespace:
kubectl get namespace devops
echo.

echo [INFO] Deployment:
kubectl get deployment user-service -n devops
echo.

echo [INFO] Pods:
kubectl get pods -l app=user-service -n devops
echo.

echo [INFO] Service:
kubectl get service user-service -n devops
echo.

echo [INFO] ConfigMap:
kubectl get configmap user-service-config -n devops
echo.

echo [INFO] Secret:
kubectl get secret user-service-secret -n devops
echo.

echo ==========================================
echo [SUCCESS] Deployment completed successfully!
echo ==========================================
echo.

echo [INFO] Useful commands:
echo   View logs:        kubectl logs -f deployment/user-service -n devops
echo   Describe pod:     kubectl describe pod ^<pod-name^> -n devops
echo   Port forward:     kubectl port-forward deployment/user-service 8082:8082 -n devops
echo   Get all:          kubectl get all -n devops
echo   Delete all:       kubectl delete namespace devops
echo.

pause
