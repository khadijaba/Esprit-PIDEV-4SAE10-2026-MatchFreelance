output "argocd_namespace" {
  value       = helm_release.argocd.namespace
  description = "Namespace Helm Argo CD."
}

output "argocd_server_hint" {
  value       = "UI Argo (NodePort HTTP) : http://<IP_VM>:30808 — mot de passe initial : kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath=\"{.data.password}\" | base64 -d"
  description = "Accès rapide à l’UI après déploiement."
}
