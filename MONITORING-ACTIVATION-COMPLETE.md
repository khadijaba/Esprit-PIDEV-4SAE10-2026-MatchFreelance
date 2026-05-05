# 📊 Monitoring Grafana & Prometheus - Activation Complète

**Status**: ✅ **ACTIVÉ À CHAQUE PUSH**  
**Date**: 5 mai 2026  
**Commit**: `ed4442c`

---

## 🎯 Objectif Atteint

Le monitoring Grafana et Prometheus est maintenant **activé automatiquement à chaque push** sur les branches suivantes :
- ✅ `User`
- ✅ `main`
- ✅ `master`
- ✅ `develop`

**Aucun filtre de fichiers** : Le workflow se déclenche pour **tous les changements**, pas seulement les modifications dans `monitoring/` ou `src/`.

---

## 🚀 Comment Ça Fonctionne

### 1. Déclenchement Automatique

Chaque fois que vous faites un `git push` vers l'une des branches configurées, le workflow GitHub Actions `.github/workflows/deploy-monitoring.yml` se déclenche automatiquement.

```bash
# Exemple
git add .
git commit -m "Mon changement"
git push origin User

# ✅ Le workflow de monitoring se déclenche automatiquement !
```

### 2. Processus de Déploiement

Le workflow exécute 3 jobs en séquence :

#### **Job 1: Build Image** 🐳
1. Checkout du code
2. Configuration JDK 17
3. Build Maven (`mvn clean package -DskipTests`)
4. Build de l'image Docker
5. Push vers GitHub Container Registry

#### **Job 2: Deploy Monitoring** 📊
1. **Validation** des fichiers de configuration
   - `docker-compose-monitoring.yml`
   - `monitoring/prometheus/prometheus.yml`
   - `monitoring/prometheus/alerts.yml`
   - `monitoring/alertmanager/alertmanager.yml`

2. **Déploiement** du stack Docker Compose
   ```bash
   docker-compose -f docker-compose-monitoring.yml up -d
   ```

3. **Health Checks** de tous les services
   - Prometheus (http://localhost:9091/-/healthy)
   - Grafana (http://localhost:3001/api/health)
   - Alertmanager (http://localhost:9093/-/healthy)
   - Node Exporter (http://localhost:9100/metrics)
   - MySQL Exporter (http://localhost:9104/metrics)

4. **Vérification** des targets Prometheus
   - Vérifie que Prometheus scrape correctement les métriques

5. **Configuration** de Grafana
   - Datasource Prometheus
   - Import du dashboard "User Service - Overview"

6. **Génération** du rapport de déploiement
   - Statut de chaque service
   - URLs d'accès
   - Targets Prometheus

#### **Job 3: Notification** 📢
- Notification de succès ou échec
- Création d'une issue GitHub en cas d'échec

---

## 📊 Services Déployés

### Stack Complet

| Service | Description | Port | URL |
|---------|-------------|------|-----|
| **Prometheus** | Collecte des métriques | 9091 | http://localhost:9091 |
| **Grafana** | Visualisation | 3001 | http://localhost:3001 |
| **Alertmanager** | Gestion des alertes | 9093 | http://localhost:9093 |
| **Node Exporter** | Métriques système | 9100 | http://localhost:9100 |
| **MySQL Exporter** | Métriques base de données | 9104 | http://localhost:9104 |

### Credentials Grafana
- **Username**: `admin`
- **Password**: `admin123`

---

## 📈 Dashboards et Métriques

### Dashboard "User Service - Overview"

Le dashboard pré-configuré affiche 7 panels :

1. **JVM Heap Memory**
   - Mémoire heap utilisée vs max
   - Détection des fuites mémoire

2. **JVM Threads**
   - Nombre de threads actifs
   - Détection des deadlocks

3. **HTTP Requests Rate**
   - Taux de requêtes par seconde
   - Par endpoint et statut

4. **HTTP Request Duration**
   - Latence moyenne, p95, p99
   - Par endpoint

5. **HTTP Errors Rate**
   - Taux d'erreurs 4xx et 5xx
   - Détection des problèmes

6. **System CPU Usage**
   - Utilisation CPU du système
   - Détection de la surcharge

7. **Database Connections**
   - Connexions actives vs max
   - Détection des fuites de connexions

### Métriques Collectées

#### Métriques JVM (Spring Boot Actuator)
- `jvm_memory_used_bytes`
- `jvm_memory_max_bytes`
- `jvm_threads_live_threads`
- `jvm_gc_pause_seconds`

#### Métriques HTTP
- `http_server_requests_seconds_count`
- `http_server_requests_seconds_sum`
- `http_server_requests_seconds_max`

#### Métriques Système (Node Exporter)
- `node_cpu_seconds_total`
- `node_memory_MemAvailable_bytes`
- `node_filesystem_avail_bytes`
- `node_network_receive_bytes_total`

#### Métriques Base de Données (MySQL Exporter)
- `mysql_global_status_threads_connected`
- `mysql_global_status_max_used_connections`
- `mysql_global_status_queries`
- `mysql_global_status_slow_queries`

---

## 🚨 Alertes Configurées

### 16 Alertes Actives

#### Alertes Critiques (4)
1. **ServiceDown** - Service indisponible pendant 2 minutes
2. **HighErrorRate** - Taux d'erreurs 5xx > 5% pendant 5 minutes
3. **MemoryCritical** - Mémoire heap > 95% pendant 5 minutes
4. **DatabaseConnectionsCritical** - Connexions DB > 95% pendant 5 minutes

#### Alertes Warning (10)
1. **HighLatency** - Latence p95 > 1s pendant 5 minutes
2. **HighCPUUsage** - CPU > 80% pendant 5 minutes
3. **HighMemoryUsage** - Mémoire heap > 85% pendant 5 minutes
4. **HighThreadCount** - Threads > 200 pendant 5 minutes
5. **HighGCTime** - Temps GC > 10% pendant 5 minutes
6. **DiskSpaceLow** - Espace disque < 20%
7. **HighNetworkTraffic** - Trafic réseau > 100MB/s
8. **DatabaseSlowQueries** - Requêtes lentes > 10/min
9. **DatabaseConnectionsHigh** - Connexions DB > 80%
10. **HighRequestRate** - Taux de requêtes > 1000/s

#### Alertes Sécurité (2)
1. **HighAuthFailureRate** - Échecs d'authentification > 10/min
2. **UnauthorizedAccessAttempts** - Tentatives d'accès non autorisé > 5/min

### Configuration Alertmanager

Les alertes sont envoyées par email (à configurer) :
```yaml
receivers:
  - name: 'email-notifications'
    email_configs:
      - to: 'your-email@example.com'
        from: 'alertmanager@example.com'
        smarthost: 'smtp.gmail.com:587'
```

---

## 🔧 Configuration

### Prometheus (`monitoring/prometheus/prometheus.yml`)

```yaml
scrape_configs:
  # User Service (Spring Boot Actuator)
  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8082']
    scrape_interval: 15s

  # Node Exporter (Métriques système)
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  # MySQL Exporter (Métriques base de données)
  - job_name: 'mysql-exporter'
    static_configs:
      - targets: ['mysql-exporter:9104']
```

### Grafana Datasource

```yaml
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

---

## 📝 Logs et Debugging

### Voir les Logs des Services

```bash
# Tous les services
docker-compose -f docker-compose-monitoring.yml logs

# Service spécifique
docker-compose -f docker-compose-monitoring.yml logs prometheus
docker-compose -f docker-compose-monitoring.yml logs grafana
docker-compose -f docker-compose-monitoring.yml logs alertmanager

# Suivre les logs en temps réel
docker-compose -f docker-compose-monitoring.yml logs -f
```

### Vérifier l'État des Services

```bash
# Statut des conteneurs
docker-compose -f docker-compose-monitoring.yml ps

# Health check manuel
curl http://localhost:9091/-/healthy  # Prometheus
curl http://localhost:3001/api/health # Grafana
curl http://localhost:9093/-/healthy  # Alertmanager
```

### Vérifier les Targets Prometheus

```bash
# Via API
curl http://localhost:9091/api/v1/targets | jq

# Via UI
# Ouvrir http://localhost:9091/targets
```

---

## 🎯 Utilisation

### 1. Accéder à Grafana

1. Ouvrir http://localhost:3001
2. Se connecter avec `admin` / `admin123`
3. Aller dans **Dashboards** → **User Service - Overview**
4. Visualiser les métriques en temps réel

### 2. Accéder à Prometheus

1. Ouvrir http://localhost:9091
2. Aller dans **Status** → **Targets** pour voir les services scrapés
3. Aller dans **Alerts** pour voir les alertes actives
4. Utiliser l'onglet **Graph** pour requêter les métriques

### 3. Accéder à Alertmanager

1. Ouvrir http://localhost:9093
2. Voir les alertes actives
3. Configurer les silences si nécessaire

---

## 🔄 Workflow GitHub Actions

### Voir l'Exécution du Workflow

1. Aller sur GitHub : https://github.com/khadijaba/Esprit-PIDEV-4SAE10-2026-MatchFreelance
2. Cliquer sur **Actions**
3. Sélectionner le workflow **📊 Deploy Monitoring Stack**
4. Voir les logs de chaque job

### Télécharger le Rapport de Déploiement

1. Aller dans l'exécution du workflow
2. Descendre jusqu'à **Artifacts**
3. Télécharger **monitoring-deployment-report**
4. Ouvrir le fichier `deployment-report.md`

### Déclencher Manuellement le Workflow

```bash
# Via GitHub UI
# Actions → 📊 Deploy Monitoring Stack → Run workflow

# Ou via GitHub CLI
gh workflow run deploy-monitoring.yml -f environment=development
```

---

## 🛠️ Maintenance

### Arrêter le Monitoring

```bash
cd User
docker-compose -f docker-compose-monitoring.yml down
```

### Redémarrer le Monitoring

```bash
cd User
docker-compose -f docker-compose-monitoring.yml restart
```

### Mettre à Jour la Configuration

1. Modifier les fichiers dans `monitoring/`
2. Commit et push
3. Le workflow redéploie automatiquement

```bash
git add monitoring/
git commit -m "Update monitoring config"
git push origin User
# ✅ Redéploiement automatique !
```

### Nettoyer les Volumes

```bash
# Arrêter et supprimer les volumes
docker-compose -f docker-compose-monitoring.yml down -v

# Redémarrer
docker-compose -f docker-compose-monitoring.yml up -d
```

---

## 📚 Documentation Complète

- **[MONITORING-GUIDE.md](MONITORING-GUIDE.md)** - Guide complet du monitoring
- **[MONITORING-AUTO-DEPLOY.md](MONITORING-AUTO-DEPLOY.md)** - Guide de déploiement automatique
- **[README-MONITORING.md](README-MONITORING.md)** - README du monitoring
- **[CORRECTIONS-COMPILATION-MONITORING.md](CORRECTIONS-COMPILATION-MONITORING.md)** - Corrections appliquées

---

## ✅ Checklist de Vérification

Après chaque push, vérifier :

- [ ] Le workflow GitHub Actions se termine avec succès
- [ ] Prometheus est accessible (http://localhost:9091)
- [ ] Grafana est accessible (http://localhost:3001)
- [ ] Alertmanager est accessible (http://localhost:9093)
- [ ] Les targets Prometheus sont UP
- [ ] Le dashboard Grafana affiche des données
- [ ] Les alertes sont configurées dans Prometheus

---

## 🎉 Résultat Final

✅ **Monitoring activé à chaque push**  
✅ **5 services déployés automatiquement**  
✅ **1 dashboard pré-configuré**  
✅ **16 alertes actives**  
✅ **Déploiement automatique via GitHub Actions**  
✅ **Health checks automatiques**  
✅ **Rapport de déploiement généré**

**Le monitoring Grafana et Prometheus est maintenant pleinement opérationnel ! 🚀**
