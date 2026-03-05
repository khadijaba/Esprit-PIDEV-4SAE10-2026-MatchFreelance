# Démarrage de la plateforme MatchFreelance

Document détaillé pour le développement et le dépannage. Pour une vue d’ensemble et l’intégration équipe, voir le **README.md** à la racine du projet.

## Prérequis

- **MySQL** : lancé sur le port 3306, utilisateur `root` (mot de passe vide par défaut dans les configs).
- **Node.js** : pour le frontend Angular.
- **Java 17** et **Maven** : pour les microservices.

## Ordre de démarrage (backend)

1. **Eureka** (port 8761)  
   `backend/EurekaServer/` → lancer en premier.

2. **Config Server** (port 8888) — *optionnel*  
   `backend/ConfigServer/`  
   Centralise la configuration des microservices. Si vous ne le lancez pas, chaque service utilise sa config locale (`optional:configserver`).

3. **Gateway** (port 8050)  
   `backend/Gateway/`

4. **Microservices** (ports 8081, 8083, 8084, 8085, 8086 selon les services) :
   - **User** (auth, utilisateurs)
   - **Formation** (formations, inscriptions, modules)
   - **Evaluation** (examens, certificats)
   - **Skill** (compétences, parcours intelligent)
   - **Project** (projets) : `backend/microservices/Project/` — **port 8084**

4. **Base MySQL** : les services créent la base si besoin (`createDatabaseIfNotExist=true`) :
   - FormationDB, project, etc.

## Développement frontend (sans Gateway)

Avec `ng serve`, le **proxy** (`proxy.conf.cjs`) envoie :

- `/api/projects` → **http://localhost:8084** (service Project directement)
- `/api/modules` → http://localhost:8081 (Formation)
- tout le reste `/api/*` → http://localhost:8050 (Gateway)

Donc **le microservice Project (8084) doit être démarré** pour que la liste et la création de projets fonctionnent.

## Démarrer le microservice Project

```bash
cd backend/microservices/Project
mvn spring-boot:run
```

Vérifier : http://localhost:8084/projects (doit retourner du JSON, éventuellement `[]`).

## Démarrer le frontend

```bash
cd frontend
npm install
ng serve
```

Ouvrir http://localhost:4200

## Problèmes fréquents

| Problème | Vérification |
|----------|----------------|
| Liste projets vide / erreur | Le service Project (8084) est-il lancé ? MySQL démarré ? |
| 404 sur /api/projects | Proxy : vérifier que `proxy.conf.cjs` contient bien la route `/api/projects` vers 8084. |
| Erreur à la création de projet | Le backend accepte désormais `projectOwnerId` null (défaut 0). Vérifier les logs du service Project. |
| Gateway ne route pas vers Project | Eureka : le service PROJECT doit être enregistré (nom = PROJECT, port 8084). |
