#!/bin/bash

# =============================================================================
# User Service Deployment Script
# =============================================================================
# This script deploys the User microservice to Kubernetes using Helm
# Usage: ./deploy.sh [environment] [image_tag] [namespace]
# Example: ./deploy.sh staging v1.2.3 microservices-staging
# =============================================================================

set -euo pipefail

# Default values
ENVIRONMENT=${1:-staging}
IMAGE_TAG=${2:-latest}
NAMESPACE=${3:-microservices-${ENVIRONMENT}}
HELM_RELEASE_NAME="user-service-${ENVIRONMENT}"
CHART_PATH="./helm/user-service"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validation functions
validate_environment() {
    case "$ENVIRONMENT" in
        staging|production)
            log_info "Deploying to environment: $ENVIRONMENT"
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT. Must be 'staging' or 'production'"
            exit 1
            ;;
    esac
}

validate_prerequisites() {
    log_info "Validating prerequisites..."
    
    # Check if kubectl is installed and configured
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check if helm is installed
    if ! command -v helm &> /dev/null; then
        log_error "helm is not installed or not in PATH"
        exit 1
    fi
    
    # Check if we can connect to the cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check if the chart exists
    if [ ! -f "$CHART_PATH/Chart.yaml" ]; then
        log_error "Helm chart not found at $CHART_PATH"
        exit 1
    fi
    
    log_success "Prerequisites validated"
}

create_namespace() {
    log_info "Creating namespace: $NAMESPACE"
    
    if kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_info "Namespace $NAMESPACE already exists"
    else
        kubectl create namespace "$NAMESPACE"
        kubectl label namespace "$NAMESPACE" environment="$ENVIRONMENT" managed-by="helm"
        log_success "Namespace $NAMESPACE created"
    fi
}

check_secrets() {
    log_info "Checking if secrets exist..."
    
    SECRET_NAME="user-service-secrets"
    if kubectl get secret "$SECRET_NAME" -n "$NAMESPACE" &> /dev/null; then
        log_success "Secret $SECRET_NAME exists in namespace $NAMESPACE"
    else
        log_warning "Secret $SECRET_NAME does not exist in namespace $NAMESPACE"
        log_warning "Please create the secret before deployment:"
        log_warning "kubectl create secret generic $SECRET_NAME \\"
        log_warning "  --from-literal=SPRING_DATASOURCE_PASSWORD=your_password \\"
        log_warning "  --from-literal=JWT_SECRET=your_jwt_secret \\"
        log_warning "  -n $NAMESPACE"
        
        read -p "Do you want to continue without secrets? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_error "Deployment cancelled"
            exit 1
        fi
    fi
}

deploy_with_helm() {
    log_info "Deploying with Helm..."
    log_info "Release: $HELM_RELEASE_NAME"
    log_info "Chart: $CHART_PATH"
    log_info "Namespace: $NAMESPACE"
    log_info "Image Tag: $IMAGE_TAG"
    
    # Prepare Helm values
    VALUES_FILE="/tmp/user-service-values-${ENVIRONMENT}.yaml"
    
    cat > "$VALUES_FILE" << EOF
image:
  tag: "$IMAGE_TAG"

config:
  springProfiles: "$ENVIRONMENT"

environment: "$ENVIRONMENT"

# Environment-specific overrides
$(if [ "$ENVIRONMENT" = "production" ]; then
    echo "replicaCount: 3"
    echo "resources:"
    echo "  limits:"
    echo "    cpu: 1000m"
    echo "    memory: 1Gi"
    echo "  requests:"
    echo "    cpu: 500m"
    echo "    memory: 512Mi"
else
    echo "replicaCount: 1"
    echo "resources:"
    echo "  limits:"
    echo "    cpu: 250m"
    echo "    memory: 256Mi"
    echo "  requests:"
    echo "    cpu: 125m"
    echo "    memory: 128Mi"
fi)

ingress:
  hosts:
    - host: user-service-${ENVIRONMENT}.yourdomain.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: user-service-${ENVIRONMENT}-tls
      hosts:
        - user-service-${ENVIRONMENT}.yourdomain.com
EOF
    
    # Deploy or upgrade
    if helm list -n "$NAMESPACE" | grep -q "$HELM_RELEASE_NAME"; then
        log_info "Upgrading existing release..."
        helm upgrade "$HELM_RELEASE_NAME" "$CHART_PATH" \
            --namespace "$NAMESPACE" \
            --values "$VALUES_FILE" \
            --wait \
            --timeout=10m \
            --history-max=5
    else
        log_info "Installing new release..."
        helm install "$HELM_RELEASE_NAME" "$CHART_PATH" \
            --namespace "$NAMESPACE" \
            --values "$VALUES_FILE" \
            --wait \
            --timeout=10m \
            --create-namespace
    fi
    
    # Cleanup temporary values file
    rm -f "$VALUES_FILE"
    
    log_success "Helm deployment completed"
}

wait_for_rollout() {
    log_info "Waiting for deployment rollout..."
    
    DEPLOYMENT_NAME="user-service-${ENVIRONMENT}"
    
    if kubectl rollout status deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout=600s; then
        log_success "Deployment rollout completed successfully"
    else
        log_error "Deployment rollout failed or timed out"
        
        log_info "Deployment status:"
        kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE"
        
        log_info "Pod status:"
        kubectl get pods -l app.kubernetes.io/name=user-service -n "$NAMESPACE"
        
        log_info "Recent events:"
        kubectl get events -n "$NAMESPACE" --sort-by='.lastTimestamp' | tail -10
        
        exit 1
    fi
}

run_health_check() {
    log_info "Running health checks..."
    
    # Get service URL
    if [ "$ENVIRONMENT" = "production" ]; then
        SERVICE_URL="https://user-service.yourdomain.com"
    else
        SERVICE_URL="https://user-service-${ENVIRONMENT}.yourdomain.com"
    fi
    
    log_info "Service URL: $SERVICE_URL"
    
    # Wait for service to be available
    MAX_ATTEMPTS=30
    ATTEMPT=1
    
    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
        log_info "Health check attempt $ATTEMPT/$MAX_ATTEMPTS..."
        
        if curl -f -s --max-time 10 "$SERVICE_URL/actuator/health" > /dev/null 2>&1; then
            log_success "Service is healthy!"
            break
        fi
        
        if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
            log_error "Service health check failed after $MAX_ATTEMPTS attempts"
            
            # Show pod logs for debugging
            log_info "Pod logs:"
            kubectl logs -l app.kubernetes.io/name=user-service -n "$NAMESPACE" --tail=50
            
            exit 1
        fi
        
        sleep 10
        ATTEMPT=$((ATTEMPT + 1))
    done
}

generate_deployment_report() {
    log_info "Generating deployment report..."
    
    REPORT_FILE="deployment-report-${ENVIRONMENT}-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$REPORT_FILE" << EOF
# 🚀 Deployment Report

**Environment**: $ENVIRONMENT
**Image Tag**: $IMAGE_TAG
**Namespace**: $NAMESPACE
**Helm Release**: $HELM_RELEASE_NAME
**Deployment Time**: $(date)

## 📊 Deployment Status

\`\`\`
$(helm status "$HELM_RELEASE_NAME" -n "$NAMESPACE")
\`\`\`

## 🏗️ Pod Status

\`\`\`
$(kubectl get pods -l app.kubernetes.io/name=user-service -n "$NAMESPACE")
\`\`\`

## 🌐 Service Status

\`\`\`
$(kubectl get service -l app.kubernetes.io/name=user-service -n "$NAMESPACE")
\`\`\`

## 🔗 Ingress Status

\`\`\`
$(kubectl get ingress -l app.kubernetes.io/name=user-service -n "$NAMESPACE")
\`\`\`

## 📈 Resource Usage

\`\`\`
$(kubectl top pods -l app.kubernetes.io/name=user-service -n "$NAMESPACE" 2>/dev/null || echo "Metrics not available")
\`\`\`

## 🔍 Recent Events

\`\`\`
$(kubectl get events -n "$NAMESPACE" --sort-by='.lastTimestamp' | tail -10)
\`\`\`
EOF
    
    log_success "Deployment report generated: $REPORT_FILE"
}

cleanup_on_failure() {
    log_error "Deployment failed. Cleaning up..."
    
    # Rollback if this was an upgrade
    if helm list -n "$NAMESPACE" | grep -q "$HELM_RELEASE_NAME"; then
        log_info "Rolling back to previous version..."
        helm rollback "$HELM_RELEASE_NAME" -n "$NAMESPACE"
    fi
}

main() {
    log_info "Starting deployment of User Service"
    log_info "Environment: $ENVIRONMENT"
    log_info "Image Tag: $IMAGE_TAG"
    log_info "Namespace: $NAMESPACE"
    
    # Set up error handling
    trap cleanup_on_failure ERR
    
    # Run deployment steps
    validate_environment
    validate_prerequisites
    create_namespace
    check_secrets
    deploy_with_helm
    wait_for_rollout
    run_health_check
    generate_deployment_report
    
    log_success "🎉 Deployment completed successfully!"
    log_info "Service URL: https://user-service-${ENVIRONMENT}.yourdomain.com"
    log_info "Health Check: https://user-service-${ENVIRONMENT}.yourdomain.com/actuator/health"
}

# Show usage if no arguments provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 [environment] [image_tag] [namespace]"
    echo "Example: $0 staging v1.2.3 microservices-staging"
    echo ""
    echo "Environments: staging, production"
    echo "Default image_tag: latest"
    echo "Default namespace: microservices-[environment]"
    exit 1
fi

# Run main function
main "$@"