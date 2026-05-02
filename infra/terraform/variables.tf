variable "kubeconfig_path" {
  type        = string
  description = "Chemin vers kubeconfig (VM Ubuntu : ~/.kube/config)"
  default     = "~/.kube/config"
}

variable "argocd_namespace" {
  type    = string
  default = "argocd"
}
