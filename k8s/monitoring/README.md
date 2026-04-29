# Monitoring Notes

This monitoring stack is intentionally lightweight for PiDev demo:

- Prometheus
- Grafana
- Alertmanager

## Important for WSL

In `prometheus-configmap.yaml`, Jenkins and SonarQube are configured with:

- `host.docker.internal:8080`
- `host.docker.internal:9000`

If those targets do not work in your kubeadm setup, replace them with your host IP reachable from pods.

## Deploy

```bash
kubectl apply -f k8s/monitoring/
```
