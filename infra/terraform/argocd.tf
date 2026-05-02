# Installe Argo CD dans le cluster (une fois le cluster kubeadm prêt).
# Sur la VM : installer Terraform + export KUBECONFIG si besoin, puis :
#   cd infra/terraform && terraform init && terraform apply

resource "helm_release" "argocd" {
  name             = "argocd"
  namespace        = var.argocd_namespace
  create_namespace = true

  repository = "https://argoproj.github.io/argo-helm"
  chart      = "argo-cd"
  # version  = "x.y.z"  # optionnel : figer une version du chart

  # Valeurs minimales pour petit cluster ; ajuster via fichier values si besoin
  set {
    name  = "server.service.type"
    value = "NodePort"
  }
}
