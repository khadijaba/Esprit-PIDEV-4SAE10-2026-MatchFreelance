# Backend — springBoot_pi

Backend (Gateway + microservices). Le frontend Angular est à la racine du projet : **`springBoot_pi/frontend`**.

## Structure

```
springBoot_pi/
├── frontend/         # Application Angular (dev: port 4200)
└── backend/
    ├── EurekaServer/     # Discovery (port 8761)
    ├── Gateway/          # API Gateway (port 8050)
    └── microservices/
        ├── Evaluation/   # Examens, passages (port 8083)
        └── Formation/    # Formations, inscriptions (Eureka)
```

## Démarrer l’ensemble

1. **Eureka** : lancer `EurekaServer` (8761).
2. **Gateway** : lancer `Gateway` (8050).
3. **Microservices** : lancer `Evaluation` (8083) et `Formation`.
4. **Frontend** : dans `springBoot_pi/frontend`, exécuter `npm install` puis `npm start` (4200).

Le front appelle `/api/*` ; le proxy Angular redirige vers la Gateway (8050), qui route vers les microservices (examens → 8083, formations → FORMATION).

## Microservice Examen (Evaluation)

- **Contexte** : `ExamenController` sous `/api/examens`.
- **Base de données** : MySQL `EvaluationDB` (voir `microservices/Evaluation/src/main/resources/application.properties`).
