output "namespace" {
  description = "Kubernetes namespace where resources are deployed"
  value       = kubernetes_namespace.production.metadata[0].name
}

output "service_name" {
  description = "Kubernetes service name"
  value       = kubernetes_service.user_service.metadata[0].name
}

output "service_type" {
  description = "Kubernetes service type"
  value       = kubernetes_service.user_service.spec[0].type
}

output "deployment_name" {
  description = "Kubernetes deployment name"
  value       = kubernetes_deployment.user_service.metadata[0].name
}

output "replicas" {
  description = "Number of pod replicas"
  value       = kubernetes_deployment.user_service.spec[0].replicas
}

output "docker_image" {
  description = "Docker image used in deployment"
  value       = var.docker_image
}
