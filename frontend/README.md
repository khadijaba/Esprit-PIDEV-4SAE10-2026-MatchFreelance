# Frontend Angular — MatchFreelance

Frontend Angular du projet (Gateway + microservices), situé à la racine du projet : `springBoot_pi/frontend`.

## Prérequis

- Node.js 18+
- npm

## Installation

```bash
cd springBoot_pi/frontend
npm install
```

## Lancer le frontend

1. **Démarrer le backend** (depuis la racine du repo) :
   - Eureka Server (port 8761)
   - Gateway (port 8082)
   - Microservice Formation (inscrit dans Eureka)
   - Microservice Evaluation (port 8083)

2. **Démarrer Angular** :

```bash
npm start
```

L’app tourne sur **http://localhost:4200**. Les appels vers `/api/*` sont proxifiés vers la Gateway (8082).

## Structure

- **Proxy** : `proxy.conf.json` envoie `/api` → `http://localhost:8082`
- **Services** : `examen.service.ts`, `formation.service.ts`, etc. utilisent des URLs relatives `/api/...`

## Build production

```bash
npm run build
```

Les fichiers générés sont dans `dist/`.
