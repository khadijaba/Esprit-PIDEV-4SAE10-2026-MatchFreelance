# 📊 Monitoring - User Microservice

## 🎯 Vue d'Ensemble

Stack de monitoring complet pour le microservice User avec **déploiement automatique** via GitHub Actions.

---

## 🚀 Démarrage Rapide

### Option 1 : Déploiement Automatique (Recommandé)

Le monitoring se déploie **automatiquement** à chaque push sur les branches :
- `User`
- `main`
- `master`
- `develop`

**Aucune action manuelle requise !** ✨

📖 **Documentation** : [MONITORING-AUTO-DEPLOY.md](MONITORING-AUTO-DEPLOY.md)

### Option 2 : Déploiement Manuel

```bash
# Linux/Mac
./scripts/start-monitoring.sh

# Windows
scripts\start-monitoring.bat
```

📖 **Documentation** : [MONITORING-GUIDE.md](MONITORING-GUIDE.md)

---

## 📊 Services Inclus

| Service | Port | Description | Accès |
|---------|------|-------------|-------|
| **Grafana** | 3001 | Visualisation des métriques | http://localhost:3001 |
| **Prometheus** | 9091 | Collecte des métriques | http://localhost:9091 |
| **Alertmanager** | 9093 | Gestion des alertes | http://localhost:9093 |
| **Node Exporter** | 9100 | Métriques système | http://localhost:9100 |
| **MySQL Exporter** | 9104 | Métriques base de données | http://localhost:9104 |

### Identifiants Grafana
- **Username** : `admin`
- **Password** : `admin123`

---

## 📈 Dashboards Disponibles

### 1. User Service - Overview (Pré-configuré)
Dashboard automatiquement importé avec :
- ✅ Service Status (UP/DOWN)
- ✅ Requests/sec
- ✅ Response Time (P50, P95, P99)
- ✅ CPU Usage
- ✅ Memory Usage (Heap)
- ✅ HTTP Requests by Status (2xx, 4xx, 5xx)

### 2. Dashboards Additionnels (À importer)
- **JVM Dashboard** : ID 4701
- **Spring Boot Dashboard** : ID 12900
- **MySQL Dashboard** : ID 7362

---

## 🚨 Alertes Configurées

### Critiques (Email + Webhook)
- 🔴 Service DOWN (1 min)
- 🔴 MySQL DOWN (1 min)
- 🔴 Database Connection Timeout (1 min)
- 🔴 Disk Space < 10% (5 min)

### Warning (Email)
- 🟡 Error Rate > 5% (5 min)
- 🟡 Response Time > 1s (5 min)
- 🟡 CPU > 80% (5 min)
- 🟡 Memory > 85% (5 min)
- 🟡 Threads > 200 (5 min)
- 🟡 GC > 10/sec (5 min)

### Sécurité (Email Sécurité)
- 🔒 Failed Login Attempts > 10 (2 min)
- 🔒 Unauthorized Requests > 1/sec (5 min)

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| **[MONITORING-AUTO-DEPLOY.md](MONITORING-AUTO-DEPLOY.md)** | 🚀 Déploiement automatique via GitHub Actions |
| **[MONITORING-GUIDE.md](MONITORING-GUIDE.md)** | 📖 Guide complet d'utilisation |
| **[docker-compose-monitoring.yml](docker-compose-monitoring.yml)** | 🐳 Configuration Docker Compose |
| **[monitoring/prometheus/prometheus.yml](monitoring/prometheus/prometheus.yml)** | ⚙️ Configuration Prometheus |
| **[monitoring/prometheus/alerts.yml](monitoring/prometheus/alerts.yml)** | 🚨 Règles d'alerte |
| **[monitoring/alertmanager/alertmanager.yml](monitoring/alertmanager/alertmanager.yml)** | 📧 Configuration Alertmanager |

---

## 🛠️ Commandes Utiles

### Démarrer
```bash
docker-compose -f docker-compose-monitoring.yml up -d
```

### Arrêter
```bash
docker-compose -f docker-compose-monitoring.yml down
```

### Voir les logs
```bash
docker-compose -f docker-compose-monitoring.yml logs -f
```

### Redémarrer
```bash
docker-compose -f docker-compose-monitoring.yml restart
```

### Vérifier l'état
```bash
docker-compose -f docker-compose-monitoring.yml ps
```

---

## 🔍 Vérification Rapide

### 1. Services Accessibles
```bash
# Prometheus
curl http://localhost:9091/-/healthy

# Grafana
curl http://localhost:3001/api/health

# Alertmanager
curl http://localhost:9093/-/healthy
```

### 2. Targets Prometheus
Ouvrez : http://localhost:9091/targets

Vérifiez que tous les targets sont **UP** :
- ✅ user-service
- ✅ prometheus
- ✅ node-exporter
- ✅ mysql-exporter
- ✅ alertmanager

### 3. Dashboard Grafana
1. Ouvrez : http://localhost:3001
2. Connectez-vous : `admin` / `admin123`
3. Allez dans **Dashboards**
4. Ouvrez **"User Service - Overview"**

---

## 📊 Métriques Collectées

### Application (Spring Boot Actuator)
- HTTP requests (count, duration, status)
- JVM memory (heap, non-heap)
- JVM threads (live, daemon, peak)
- JVM GC (count, duration)
- CPU usage
- Database connections (HikariCP)

### Système (Node Exporter)
- CPU usage
- Memory usage
- Disk I/O
- Network I/O
- Filesystem usage

### Base de Données (MySQL Exporter)
- Connections
- Queries per second
- Slow queries
- Table locks
- Buffer pool usage

---

## 🔧 Configuration

### Modifier les Alertes
Éditez : `monitoring/prometheus/alerts.yml`

### Configurer les Notifications Email
Éditez : `monitoring/alertmanager/alertmanager.yml`

```yaml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'votre-email@gmail.com'
  smtp_auth_username: 'votre-email@gmail.com'
  smtp_auth_password: 'votre-app-password'
```

### Ajouter des Targets Prometheus
Éditez : `monitoring/prometheus/prometheus.yml`

```yaml
scrape_configs:
  - job_name: 'mon-nouveau-service'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

---

## 🚨 Troubleshooting

### Prometheus ne collecte pas les métriques

**Problème** : Target "user-service" est DOWN

**Solutions** :
1. Vérifiez que le service User est démarré : http://localhost:9090/actuator/health
2. Sur Linux, remplacez `host.docker.internal` par `172.17.0.1` dans `prometheus.yml`

### Grafana ne se connecte pas à Prometheus

**Problème** : "Bad Gateway" ou "Connection refused"

**Solutions** :
1. Vérifiez que Prometheus est démarré :
   ```bash
   docker-compose -f docker-compose-monitoring.yml ps prometheus
   ```
2. Vérifiez la datasource dans Grafana : Settings → Data Sources

### MySQL Exporter ne fonctionne pas

**Problème** : Target "mysql-exporter" est DOWN

**Solutions** :
1. Vérifiez que MySQL est accessible
2. Vérifiez la chaîne de connexion dans `docker-compose-monitoring.yml`
3. Si vous avez un mot de passe MySQL, ajoutez-le :
   ```yaml
   DATA_SOURCE_NAME=root:votre_password@tcp(host.docker.internal:3306)/UserDB
   ```

---

## 📈 Requêtes PromQL Utiles

### Taux de requêtes
```promql
rate(http_server_requests_seconds_count{job="user-service"}[5m])
```

### Temps de réponse P95
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job="user-service"}[5m])) by (le))
```

### Taux d'erreur
```promql
(sum(rate(http_server_requests_seconds_count{status=~"5..",job="user-service"}[5m])) / sum(rate(http_server_requests_seconds_count{job="user-service"}[5m]))) * 100
```

### Utilisation CPU
```promql
process_cpu_usage{job="user-service"} * 100
```

### Utilisation mémoire
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

---

## ✅ Checklist

### Déploiement Initial
- [ ] Docker et Docker Compose installés
- [ ] Microservice User démarré sur le port 9090
- [ ] MySQL démarré sur le port 3306
- [ ] Stack de monitoring démarré
- [ ] Tous les targets Prometheus sont UP
- [ ] Grafana accessible avec dashboard visible
- [ ] (Optionnel) Email configuré dans Alertmanager

### Après Chaque Push
- [ ] Workflow GitHub Actions terminé avec succès
- [ ] Rapport de déploiement vérifié
- [ ] Services accessibles
- [ ] Métriques visibles dans Grafana
- [ ] Alertes fonctionnelles

---

## 🎯 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Actions                           │
│  (Déploiement automatique à chaque push)                   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Docker Compose Stack                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │  Prometheus  │◄───│ User Service │    │   Grafana    │ │
│  │   :9091      │    │    :9090     │    │    :3001     │ │
│  └──────┬───────┘    └──────────────┘    └──────▲───────┘ │
│         │                                         │         │
│         │            ┌──────────────┐            │         │
│         ├───────────►│ Alertmanager │            │         │
│         │            │    :9093     │            │         │
│         │            └──────────────┘            │         │
│         │                                         │         │
│         │            ┌──────────────┐            │         │
│         ├───────────►│Node Exporter │            │         │
│         │            │    :9100     │            │         │
│         │            └──────────────┘            │         │
│         │                                         │         │
│         │            ┌──────────────┐            │         │
│         └───────────►│MySQL Exporter│            │         │
│                      │    :9104     │            │         │
│                      └──────────────┘            │         │
│                                                   │         │
│                      Prometheus ─────────────────┘         │
│                      (Datasource)                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎉 Résumé

✅ **Déploiement automatique** à chaque push  
✅ **5 services** de monitoring  
✅ **Dashboards** pré-configurés  
✅ **15+ alertes** configurées  
✅ **3 types de métriques** : Application, Système, Base de données  
✅ **Documentation complète**  

**Le monitoring est maintenant opérationnel ! 🚀**

---

## 📞 Support

- **Documentation** : Consultez les fichiers `.md` dans ce dossier
- **Logs** : `docker-compose -f docker-compose-monitoring.yml logs -f`
- **Issues** : Créées automatiquement en cas d'échec de déploiement

---

**Dernière mise à jour** : 5 mai 2026
