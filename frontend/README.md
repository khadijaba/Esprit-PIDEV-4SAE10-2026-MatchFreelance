# Frontend - Angular Application

Application Angular pour la plateforme FreelanceHub avec deux interfaces :
- **Frontoffice** : Interface publique pour freelancers et project owners
- **Backoffice** : Dashboard admin pour supervision

## Structure

```
frontend/
├── src/
│   └── app/
│       ├── components/          # Composants Angular
│       │   ├── front-*         # Composants frontoffice
│       │   ├── freelancer-*    # Composants freelancer
│       │   ├── layout/         # Layout admin
│       │   └── ...             # Autres composants
│       ├── services/           # Services Angular
│       │   ├── skill.service.ts
│       │   └── project.service.ts
│       ├── models/             # Modèles TypeScript
│       │   ├── skill.model.ts
│       │   └── project.model.ts
│       └── app.routes.ts       # Routes de l'application
├── proxy.conf.json            # Configuration proxy pour API Gateway
└── package.json
```

## Installation

```bash
cd frontend
npm install
```

## Démarrage

```bash
npm start
```

L'application sera accessible sur **http://localhost:4200**

**Note:** Assurez-vous que l'API Gateway (port 8082) est démarré avant de lancer le frontend.

## Routes

### Frontoffice (Public)
- `/` - Page d'accueil
- `/projects` - Liste des projets
- `/projects/:id` - Détails d'un projet
- `/freelancer/skills` - Gestion des compétences (freelancer)

### Backoffice (Admin)
- `/admin/dashboard` - Tableau de bord
- `/admin/projects` - Liste des projets
- `/admin/projects/new` - Créer un projet
- `/admin/projects/:id` - Détails d'un projet
- `/admin/projects/:id/edit` - Éditer un projet
- `/admin/skills` - Supervision des compétences

## Configuration Proxy

Le fichier `proxy.conf.json` configure le proxy pour rediriger toutes les requêtes `/api/*` vers l'API Gateway sur le port 8082.

## Technologies

- Angular 21
- Tailwind CSS 4
- RxJS
- Standalone Components