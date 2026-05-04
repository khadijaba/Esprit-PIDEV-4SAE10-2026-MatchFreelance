variable "kubeconfig_path" {
  type        = string
  description = "Chemin du kubeconfig pointant vers le cluster kubeadm (VM)."
  default     = "~/.kube/config"
}

variable "argocd_chart_version" {
  type        = string
  description = "Version du chart Helm argo-cd (argo-helm)."
  default     = "7.6.12"
}
