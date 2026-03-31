# Libérer le port 8082 (Windows)

Si l'API Gateway ne démarre pas avec « Port 8082 was already in use » :

## 1. Trouver le processus qui utilise le port 8082

**PowerShell (admin ou non) :**

```powershell
netstat -ano | findstr :8082
```

Tu verras une ligne du type : `TCP    0.0.0.0:8082    ...    LISTENING    12345`  
Le dernier nombre est le **PID** (ex. 12345).

**Ou avec PowerShell :**

```powershell
Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue | Select-Object OwningProcess
```

## 2. Arrêter le processus

Remplace `12345` par le PID affiché :

```powershell
taskkill /PID 12345 /F
```

## 3. Relancer l'API Gateway

Redémarre l’application Gateway ; le port 8082 sera libre.

---

## Alternative : utiliser un autre port pour la Gateway

Sans libérer 8082, tu peux lancer la Gateway sur un autre port, par exemple **8086** :

- **En ligne de commande :**  
  `java -DPORT=8086 -jar ...`  
  ou dans ton IDE : variable d’environnement **PORT=8086**.

- **Dans application.properties** le port est déjà configuré en `server.port=${PORT:8082}`, donc **PORT=8086** suffit.

Pense à mettre à jour le **frontend** (proxy) si tu utilises la Gateway : dans `proxy.conf.json`, mets `"target": "http://localhost:8086"` au lieu de 8082.
