# Erreur ECONNREFUSED sur /api/...

Si vous voyez dans le terminal :

```
[vite] http proxy error: /api/projects/...
AggregateError [ECONNREFUSED]
```

**Cause :** Le frontend envoie les appels API vers `http://localhost:8082`, mais l’**API Gateway** (et les autres services backend) ne tournent pas.

## Solution : démarrer le backend avant le frontend

### Option 1 – Script automatique (Windows)

Dans un PowerShell, exécuter :

```powershell
cd C:\Users\emnad\Backend\BackEnd\BackEnd
.\start-all.ps1
```

Cela ouvre 4 fenêtres (Eureka, Skill, Project, ApiGateway). Attendre environ **1 minute** que chaque service affiche un message du type `Started ...Application`.

Ensuite, dans un autre terminal :

```bash
cd D:\telechqrgement\frontendd\frontend
npm start
```

### Option 2 – Démarrer les services à la main (4 terminaux)

Dans l’ordre, exécuter dans 4 terminaux différents :

| # | Commande | Port |
|---|----------|------|
| 1 | `cd C:\Users\emnad\Backend\BackEnd\BackEnd\Eureka\eureka` puis `mvn spring-boot:run` | 8761 |
| 2 | `cd C:\Users\emnad\Backend\BackEnd\BackEnd\Microservices\Skill` puis `mvn spring-boot:run` | 8083 |
| 3 | `cd C:\Users\emnad\Backend\BackEnd\BackEnd\Microservices\Project` puis `mvn spring-boot:run` | 8084 |
| 4 | `cd C:\Users\emnad\Backend\BackEnd\BackEnd\ApiGateway` puis `mvn spring-boot:run` | **8082** |

Quand la 4ᵉ application affiche `Started ApiGatewayApplication`, vous pouvez lancer `npm start` dans le frontend.

### Vérification rapide

- Backend OK : http://localhost:8082/api/projects → réponse JSON (même vide `[]`).
- Frontend : http://localhost:4200 puis http://localhost:4200/admin pour le dashboard.
