variable "namespace" {
  description = "Kubernetes namespace for deployment"
  type        = string
  default     = "production"
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "user-service"
}

variable "replicas" {
  description = "Number of pod replicas"
  type        = number
  default     = 3
  
  validation {
    condition     = var.replicas > 0 && var.replicas <= 10
    error_message = "Replicas must be between 1 and 10."
  }
}

variable "docker_image" {
  description = "Docker image for the application"
  type        = string
  default     = "your-dockerhub-username/user-microservice:latest"
}

variable "database_url" {
  description = "Database connection URL"
  type        = string
  default     = "jdbc:mysql://mysql-service:3306/UserDB?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC"
}

variable "database_username" {
  description = "Database username"
  type        = string
  default     = "userapp"
  sensitive   = true
}

variable "database_password" {
  description = "Database password"
  type        = string
  default     = "userpassword"
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT secret key"
  type        = string
  sensitive   = true
  default     = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
}

variable "sendgrid_api_key" {
  description = "SendGrid API key"
  type        = string
  sensitive   = true
  default     = "YOUR_SENDGRID_API_KEY"
}
