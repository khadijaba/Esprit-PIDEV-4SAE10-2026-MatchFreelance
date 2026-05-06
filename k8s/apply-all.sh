#!/bin/bash

# ========================================
# Kubernetes Deployment Script
# User Microservice - DevOps Namespace
# ========================================

set -e  # Exit on error

echo "=========================================="
echo "Deploying User Microservice to Kubernetes"
echo "Namespace: devops"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}→ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed. Please install kubectl first."
    exit 1
fi

# Check if cluster is accessible
if ! kubectl cluster-info &> /dev/null; then
    print_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
    exit 1
fi

print_success "Connected to Kubernetes cluster"

# Step 1: Create Namespace
print_info "Creating namespace..."
kubectl apply -f namespace-devops.yaml
print_success "Namespace created/updated"

# Step 2: Create ConfigMap
print_info "Creating ConfigMap..."
kubectl apply -f configmap-devops.yaml
print_success "ConfigMap created/updated"

# Step 3: Create Secret
print_info "Creating Secret..."
kubectl apply -f secret-devops.yaml
print_success "Secret created/updated"

# Step 4: Deploy Application
print_info "Deploying application..."
kubectl apply -f deployment-devops.yaml
print_success "Deployment created/updated"

# Step 5: Create Service
print_info "Creating service..."
kubectl apply -f service-devops.yaml
print_success "Service created/updated"

# Wait for deployment to be ready
print_info "Waiting for deployment to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/user-service -n devops

print_success "Deployment is ready!"

# Display deployment status
echo ""
echo "=========================================="
echo "Deployment Status"
echo "=========================================="

echo ""
print_info "Namespace:"
kubectl get namespace devops

echo ""
print_info "Deployment:"
kubectl get deployment user-service -n devops

echo ""
print_info "Pods:"
kubectl get pods -l app=user-service -n devops

echo ""
print_info "Service:"
kubectl get service user-service -n devops

echo ""
print_info "ConfigMap:"
kubectl get configmap user-service-config -n devops

echo ""
print_info "Secret:"
kubectl get secret user-service-secret -n devops

echo ""
echo "=========================================="
print_success "Deployment completed successfully!"
echo "=========================================="

echo ""
print_info "Useful commands:"
echo "  View logs:        kubectl logs -f deployment/user-service -n devops"
echo "  Describe pod:     kubectl describe pod <pod-name> -n devops"
echo "  Port forward:     kubectl port-forward deployment/user-service 8082:8082 -n devops"
echo "  Get all:          kubectl get all -n devops"
echo "  Delete all:       kubectl delete namespace devops"
