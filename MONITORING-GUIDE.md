# 📊 Guide Complet : Monitoring avec Grafana et Prometheus

## 🎯 Vue d'Ensemble

Ce guide vous explique comment configurer et utiliser le monitoring complet pour le microservice User avec :
- **Prometheus** : Collecte des métriques
- **Grafana** : Visualisation des métriques
- **Alertmanager** : Gestion des alertes
- **Node Exporter** : Métriques système
- **MySQL Exporter** : Métriques base de données

---

## 📋 Prérequis

- Docker et Docker Compose installés
- Microservice User en cours d'exécution sur le port 9090
- MySQL en cours d'exécution sur le port 3306

---

## 🚀 Démarrage Rapide

### 1. Démarrer le Stack de Monitoring

```bash
cd User
docker-compose -f docker-compose-monitoring.yml up -d
```

### 2. Vérifier que tous les services sont démarrés

```bash
docker-compose -f docker-compose-monitoring.yml ps
```

Vous devriez voir :
- ✅ user-service-prometheus (port 9091)
- ✅ user-service-grafana (port 3001)
- ✅ user-service-alertmanager (port 9093)
- ✅ user-service-node-exporter (port 9100)
- ✅ user-service-mysql-exporter (port 9104)

### 3. Accéder aux Interfaces

| Service | URL | Identifiants |
|---------|-----|--------------|
| **Grafana** | http://localhost:3001 | admin / admin123 |
| **Prometheus** | http://localhost:9091 | - |
| **Alertmanager** | http://localhost:9093 | - |

---

## 📊 Utilisation de Grafana

### Première Connexion

1. Ouvrez http://localhost:3001
2. Connectez-vous avec :
   - **Username** : `admin`
   - **Password** : `admin123`
3. (Optionnel) Changez le mot de passe

### Dashboard Pré-configuré

Un dashboard "User Service - Overview" est automatiquement configuré avec :

#### 📈 Métriques Principales
- **Service Status** : UP/DOWN
- **Requests/sec** : Nombre de requêtes par seconde
- **Response Time P95** : Temps de réponse 95e percentile
- **CPU Usage** : Utilisation CPU du service
- **Memory Usage** : Utilisation mémoire heap

#### 📊 Graphiques
- **HTTP Requests by Status** : Répartition 2xx/4xx/5xx
- **Response Time Percentiles** : P50, P95, P99

### Créer un Nouveau Dashboard

1. Cliquez sur **"+"** → **"Dashboard"**
2. Cliquez sur **"Add new panel"**
3. Sélectionnez **"Prometheus"** comme source de données
4. Entrez une requête PromQL (exemples ci-dessous)
5. Configurez la visualisation
6. Cliquez sur **"Apply"**

---

## 🔍 Requêtes PromQL Utiles

### Métriques Application

```promql
# Nombre total de requêtes
sum(http_server_requests_seconds_count{job="user-service"})

# Taux de requêtes par seconde
rate(http_server_requests_seconds_count{job="user-service"}[5m])

# Temps de réponse moyen
rate(http_server_requests_seconds_sum{job="user-service"}[5m]) 
/ 
rate(http_server_requests_seconds_count{job="user-service"}[5m])

# Taux d'erreur (%)
(
  sum(rate(http_server_requests_seconds_count{status=~"5..",job="user-service"}[5m]))
  /
  sum(rate(http_server_requests_seconds_count{job="user-service"}[5m]))
) * 100

# Requêtes par endpoint
sum(rate(http_server_requests_seconds_count{job="user-service"}[5m])) by (uri)
```

### Métriques JVM

```promql
# Utilisation CPU
process_cpu_usage{job="user-service"} * 100

# Mémoire heap utilisée (%)
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# Nombre de threads actifs
jvm_threads_live{job="user-service"}

# Fréquence du Garbage Collection
rate(jvm_gc_pause_seconds_count{job="user-service"}[5m])

# Temps passé en GC
rate(jvm_gc_pause_seconds_sum{job="user-service"}[5m])
```

### Métriques Base de Données

```promql
# Connexions actives
hikaricp_connections_active{job="user-service"}

# Connexions en attente
hikaricp_connections_pending{job="user-service"}

# Timeouts de connexion
hikaricp_connections_timeout_total{job="user-service"}

# Temps d'acquisition de connexion
hikaricp_connections_acquire_seconds_sum / hikaricp_connections_acquire_seconds_count
```

### Métriques Système

```promql
# CPU système (%)
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Mémoire disponible (%)
(node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100

# Espace disque disponible (%)
(node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100

# I/O disque (lectures/sec)
rate(node_disk_reads_completed_total[5m])

# I/O disque (écritures/sec)
rate(node_disk_writes_completed_total[5m])
```

---

## 🚨 Alertes Configurées

### Alertes Critiques

| Alerte | Condition | Durée | Action |
|--------|-----------|-------|--------|
| **UserServiceDown** | Service inaccessible | 1 min | Email + Webhook |
| **MySQLDown** | Base de données inaccessible | 1 min | Email + Webhook |
| **DatabaseConnectionTimeout** | Timeout de connexion DB | 1 min | Email + Webhook |
| **DiskSpaceLow** | < 10% d'espace disque | 5 min | Email |

### Alertes Warning

| Alerte | Condition | Durée | Action |
|--------|-----------|-------|--------|
| **HighErrorRate** | Taux d'erreur 5xx > 5% | 5 min | Email |
| **HighResponseTime** | P95 > 1s | 5 min | Email |
| **HighCPUUsage** | CPU > 80% | 5 min | Email |
| **HighMemoryUsage** | Heap > 85% | 5 min | Email |
| **HighThreadCount** | Threads > 200 | 5 min | Email |
| **FrequentGarbageCollection** | GC > 10/sec | 5 min | Email |
| **HighDatabaseConnections** | Connexions > 8/10 | 5 min | Email |
| **SlowMySQLQueries** | Requêtes lentes > 10 | 5 min | Email |
| **HighSystemCPU** | CPU système > 80% | 5 min | Email |
| **LowSystemMemory** | Mémoire < 10% | 5 min | Email |

### Alertes Sécurité

| Alerte | Condition | Durée | Action |
|--------|-----------|-------|--------|
| **HighFailedLoginAttempts** | > 10 échecs en 5 min | 2 min | Email sécurité |
| **HighUnauthorizedRequests** | Requêtes 403 > 1/sec | 5 min | Email sécurité |

---

## 🔧 Configuration des Notifications

### Email (Gmail)

1. Éditez `monitoring/alertmanager/alertmanager.yml`
2. Configurez les paramètres SMTP :

```yaml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'votre-email@gmail.com'
  smtp_auth_username: 'votre-email@gmail.com'
  smtp_auth_password: 'votre-app-password'  # Générer un mot de passe d'application
  smtp_require_tls: true
```

3. Redémarrez Alertmanager :

```bash
docker-compose -f docker-compose-monitoring.yml restart alertmanager
```

### Slack (Optionnel)

1. Créez un Webhook Slack : https://api.slack.com/messaging/webhooks
2. Décommentez et configurez dans `alertmanager.yml` :

```yaml
slack_configs:
  - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    channel: '#alerts'
    title: '🚨 Alert: {{ .GroupLabels.alertname }}'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

---

## 📈 Dashboards Recommandés

### Dashboard JVM (ID: 4701)

1. Dans Grafana, cliquez sur **"+"** → **"Import"**
2. Entrez l'ID : **4701**
3. Sélectionnez **"Prometheus"** comme source de données
4. Cliquez sur **"Import"**

### Dashboard Spring Boot (ID: 12900)

1. Cliquez sur **"+"** → **"Import"**
2. Entrez l'ID : **12900**
3. Sélectionnez **"Prometheus"** comme source de données
4. Cliquez sur **"Import"**

### Dashboard MySQL (ID: 7362)

1. Cliquez sur **"+"** → **"Import"**
2. Entrez l'ID : **7362**
3. Sélectionnez **"Prometheus"** comme source de données
4. Cliquez sur **"Import"**

---

## 🛠️ Commandes Utiles

### Démarrer le monitoring

```bash
docker-compose -f docker-compose-monitoring.yml up -d
```

### Arrêter le monitoring

```bash
docker-compose -f docker-compose-monitoring.yml down
```

### Voir les logs

```bash
# Tous les services
docker-compose -f docker-compose-monitoring.yml logs -f

# Service spécifique
docker-compose -f docker-compose-monitoring.yml logs -f prometheus
docker-compose -f docker-compose-monitoring.yml logs -f grafana
docker-compose -f docker-compose-monitoring.yml logs -f alertmanager
```

### Redémarrer un service

```bash
docker-compose -f docker-compose-monitoring.yml restart prometheus
docker-compose -f docker-compose-monitoring.yml restart grafana
```

### Supprimer les volumes (réinitialisation complète)

```bash
docker-compose -f docker-compose-monitoring.yml down -v
```

---

## 🔍 Vérification de la Configuration

### 1. Vérifier que Prometheus collecte les métriques

1. Ouvrez http://localhost:9091
2. Allez dans **Status** → **Targets**
3. Vérifiez que tous les targets sont **UP** :
   - user-service
   - node-exporter
   - mysql-exporter
   - prometheus
   - alertmanager

### 2. Tester une requête Prometheus

1. Allez dans **Graph**
2. Entrez : `up{job="user-service"}`
3. Cliquez sur **Execute**
4. Vous devriez voir `1` (service UP)

### 3. Vérifier les alertes

1. Ouvrez http://localhost:9091/alerts
2. Vous devriez voir toutes les règles d'alerte configurées
3. Les alertes actives apparaissent en rouge

### 4. Tester Alertmanager

1. Ouvrez http://localhost:9093
2. Vous devriez voir les alertes actives (si il y en a)

---

## 📊 Métriques Exposées par le Service

Le microservice User expose les métriques via :
- **URL** : http://localhost:9090/actuator/prometheus
- **Format** : Prometheus text format

### Endpoints Actuator Disponibles

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | État de santé du service |
| `/actuator/info` | Informations sur l'application |
| `/actuator/metrics` | Liste des métriques disponibles |
| `/actuator/prometheus` | Métriques au format Prometheus |

---

## 🚨 Troubleshooting

### Prometheus ne collecte pas les métriques

**Problème** : Target "user-service" est DOWN

**Solutions** :
1. Vérifiez que le service User est démarré : http://localhost:9090/actuator/health
2. Vérifiez la configuration dans `prometheus.yml` :
   ```yaml
   - targets: ['host.docker.internal:9090']
   ```
3. Sur Linux, remplacez `host.docker.internal` par `172.17.0.1` ou l'IP de votre machine

### Grafana ne se connecte pas à Prometheus

**Problème** : "Bad Gateway" ou "Connection refused"

**Solutions** :
1. Vérifiez que Prometheus est démarré :
   ```bash
   docker-compose -f docker-compose-monitoring.yml ps prometheus
   ```
2. Vérifiez la configuration de la datasource :
   - URL : `http://prometheus:9090` (nom du service Docker)

### MySQL Exporter ne fonctionne pas

**Problème** : Target "mysql-exporter" est DOWN

**Solutions** :
1. Vérifiez que MySQL est accessible :
   ```bash
   mysql -h localhost -u root -p
   ```
2. Vérifiez la chaîne de connexion dans `docker-compose-monitoring.yml` :
   ```yaml
   DATA_SOURCE_NAME=root:@tcp(host.docker.internal:3306)/UserDB
   ```
3. Si vous avez un mot de passe MySQL, ajoutez-le :
   ```yaml
   DATA_SOURCE_NAME=root:votre_password@tcp(host.docker.internal:3306)/UserDB
   ```

### Les alertes ne sont pas envoyées

**Problème** : Pas d'email reçu

**Solutions** :
1. Vérifiez la configuration SMTP dans `alertmanager.yml`
2. Pour Gmail, générez un "App Password" : https://myaccount.google.com/apppasswords
3. Testez la configuration :
   ```bash
   docker-compose -f docker-compose-monitoring.yml logs alertmanager
   ```

---

## 📚 Ressources

### Documentation Officielle
- **Prometheus** : https://prometheus.io/docs/
- **Grafana** : https://grafana.com/docs/
- **Alertmanager** : https://prometheus.io/docs/alerting/latest/alertmanager/
- **Spring Boot Actuator** : https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Micrometer** : https://micrometer.io/docs

### Dashboards Grafana
- **Grafana Dashboards** : https://grafana.com/grafana/dashboards/
- **JVM Dashboard** : https://grafana.com/grafana/dashboards/4701
- **Spring Boot Dashboard** : https://grafana.com/grafana/dashboards/12900

### PromQL
- **PromQL Basics** : https://prometheus.io/docs/prometheus/latest/querying/basics/
- **PromQL Examples** : https://prometheus.io/docs/prometheus/latest/querying/examples/

---

## ✅ Checklist de Démarrage

- [ ] Docker et Docker Compose installés
- [ ] Microservice User démarré sur le port 9090
- [ ] MySQL démarré sur le port 3306
- [ ] Stack de monitoring démarré : `docker-compose -f docker-compose-monitoring.yml up -d`
- [ ] Tous les targets Prometheus sont UP : http://localhost:9091/targets
- [ ] Grafana accessible : http://localhost:3001 (admin/admin123)
- [ ] Dashboard "User Service - Overview" visible dans Grafana
- [ ] (Optionnel) Configuration email dans Alertmanager
- [ ] (Optionnel) Import de dashboards supplémentaires

---

**🎉 Votre monitoring est maintenant opérationnel !**

Pour toute question, consultez la documentation ou les logs des services.
