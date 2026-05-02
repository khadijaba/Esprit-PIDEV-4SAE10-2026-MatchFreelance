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

  # NodePort : ne pas utiliser 30080 / 30090 (réservés à Grafana / Prometheus dans kube-prometheus-stack).
  set {
    name  = "server.service.type"
    value = "NodePort"
  }
  set {
    name  = "server.service.nodePortHttp"
    value = "30808"
  }
  set {
    name  = "server.service.nodePortHttps"
    value = "30809"
  }

  # Gros dépôt Git (~300 Mo+) : sans ça, le repo-server dépasse souvent le délai → ComparisonError / DeadlineExceeded.
  values = [
    yamlencode({
      controller = {
        extraArgs = ["--repo-server-timeout-seconds=600"]
      }
      repoServer = {
        env = [
          {
            name  = "ARGOCD_EXEC_TIMEOUT"
            value = "900s"
          }
        ]
      }
    })
  ]
}
