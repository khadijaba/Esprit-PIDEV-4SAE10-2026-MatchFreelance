# ========================================
# Terraform Configuration for User Microservice
# ========================================

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  
  backend "local" {
    path = "terraform.tfstate"
  }
}

# ========================================
# Provider Configuration
# ========================================

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

# ========================================
# Variables
# ========================================

variable "namespace" {
  description = "Kubernetes namespace"
  type        = string
  default     = "production"
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "user-service"
}

variable "replicas" {
  description = "Number of replicas"
  type        = number
  default     = 3
}

variable "docker_image" {
  description = "Docker image"
  type        = string
  default     = "your-dockerhub-username/user-microservice:latest"
}

# ========================================
# Namespace
# ========================================

resource "kubernetes_namespace" "production" {
  metadata {
    name = var.namespace
    labels = {
      name        = var.namespace
      environment = "production"
    }
  }
}

# ========================================
# ConfigMap
# ========================================

resource "kubernetes_config_map" "user_service_config" {
  metadata {
    name      = "${var.app_name}-config"
    namespace = kubernetes_namespace.production.metadata[0].name
  }

  data = {
    "database.url" = "jdbc:mysql://mysql-service:3306/UserDB?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC"
  }
}

# ========================================
# Secrets
# ========================================

resource "kubernetes_secret" "user_service_secrets" {
  metadata {
    name      = "${var.app_name}-secrets"
    namespace = kubernetes_namespace.production.metadata[0].name
  }

  type = "Opaque"

  data = {
    "database.username" = base64encode("userapp")
    "database.password" = base64encode("userpassword")
    "jwt.secret"        = base64encode("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
    "sendgrid.api.key"  = base64encode("YOUR_SENDGRID_API_KEY")
  }
}

# ========================================
# Deployment
# ========================================

resource "kubernetes_deployment" "user_service" {
  metadata {
    name      = var.app_name
    namespace = kubernetes_namespace.production.metadata[0].name
    labels = {
      app     = var.app_name
      version = "v1"
    }
  }

  spec {
    replicas = var.replicas

    selector {
      match_labels = {
        app = var.app_name
      }
    }

    template {
      metadata {
        labels = {
          app     = var.app_name
          version = "v1"
        }
      }

      spec {
        container {
          name  = var.app_name
          image = var.docker_image
          image_pull_policy = "Always"

          port {
            container_port = 9090
            name           = "http"
            protocol       = "TCP"
          }

          env {
            name = "SPRING_DATASOURCE_URL"
            value_from {
              config_map_key_ref {
                name = kubernetes_config_map.user_service_config.metadata[0].name
                key  = "database.url"
              }
            }
          }

          env {
            name = "SPRING_DATASOURCE_USERNAME"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.user_service_secrets.metadata[0].name
                key  = "database.username"
              }
            }
          }

          env {
            name = "SPRING_DATASOURCE_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.user_service_secrets.metadata[0].name
                key  = "database.password"
              }
            }
          }

          env {
            name = "JWT_SECRET"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.user_service_secrets.metadata[0].name
                key  = "jwt.secret"
              }
            }
          }

          resources {
            requests = {
              memory = "512Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "1Gi"
              cpu    = "500m"
            }
          }

          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = 9090
            }
            initial_delay_seconds = 60
            period_seconds        = 10
            timeout_seconds       = 5
            failure_threshold     = 3
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = 9090
            }
            initial_delay_seconds = 30
            period_seconds        = 5
            timeout_seconds       = 3
            failure_threshold     = 3
          }
        }
      }
    }
  }
}

# ========================================
# Service
# ========================================

resource "kubernetes_service" "user_service" {
  metadata {
    name      = var.app_name
    namespace = kubernetes_namespace.production.metadata[0].name
    labels = {
      app = var.app_name
    }
  }

  spec {
    type = "LoadBalancer"

    selector = {
      app = var.app_name
    }

    port {
      name        = "http"
      port        = 9090
      target_port = 9090
      protocol    = "TCP"
    }

    session_affinity = "ClientIP"
  }
}

# ========================================
# Outputs
# ========================================

output "namespace" {
  description = "Kubernetes namespace"
  value       = kubernetes_namespace.production.metadata[0].name
}

output "service_name" {
  description = "Service name"
  value       = kubernetes_service.user_service.metadata[0].name
}

output "deployment_name" {
  description = "Deployment name"
  value       = kubernetes_deployment.user_service.metadata[0].name
}
