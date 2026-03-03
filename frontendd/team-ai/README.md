# Team AI – Service d’analyse de projets et construction d’équipe

Service Python (FastAPI) qui :
- **Analyse** la description d’un projet (NLP / option LLM) : rôles, compétences, budget, durée, complexité.
- **Construit une équipe** optimale : sélection des freelancers complémentaires, proposition d’un leader technique, génération des notifications à envoyer.

## Prérequis

- Python 3.10+
- Créer un environnement virtuel recommandé : `python -m venv venv` puis `venv\Scripts\activate` (Windows) ou `source venv/bin/activate` (Linux/Mac).

## Installation

```bash
cd team-ai
pip install -r requirements.txt
```

## Lancement

```bash
# Depuis team-ai/
python run.py
```

Ou avec uvicorn directement :

```bash
uvicorn app.main:app --host 0.0.0.0 --port 5000
```

Le service écoute sur **http://localhost:5000**.

## Variables d’environnement (optionnel)

| Variable | Description |
|----------|-------------|
| `TEAM_AI_HOST` | Host (défaut : 0.0.0.0) |
| `TEAM_AI_PORT` | Port (défaut : 5000) |
| `OPENAI_API_KEY` | Clé API OpenAI pour l’analyse LLM |
| `OPENAI_BASE_URL` | URL de l’API (OpenAI ou Ollama, ex. `http://localhost:11434/v1`) |
| `TEAM_AI_USE_LLM` | `true` pour activer l’analyse par LLM |

Sans LLM, l’analyse utilise des règles et mots-clés (NLP basique).

## Endpoints

### `POST /api/analyze-project`

Corps JSON :
```json
{
  "title": "Plateforme de gestion médicale",
  "description": "Projet complexe nécessitant backend Spring Boot, frontend Angular, équipe de 3 devs, budget 15 000$, durée 3 mois."
}
```

Réponse (camelCase pour le frontend) :
- `complexity` : simple | medium | complex
- `roles` : liste de rôles
- `requiredSkills` : compétences détectées
- `budgetEstimate` : { minAmount, maxAmount, currency }
- `durationEstimateDays` : nombre de jours
- `technicalLeaderRole` : rôle du leader
- `summary` : résumé

### `POST /api/build-team`

Corps JSON :
```json
{
  "projectId": 1,
  "projectTitle": "Mon projet",
  "projectAnalysis": {
    "complexity": "complex",
    "roles": ["Développeur Backend", "Développeur Frontend", "Tech Lead"],
    "requiredSkills": ["Java", "Spring", "Angular"],
    "technicalLeaderRole": "Tech Lead"
  },
  "freelancers": [
    {
      "freelancerId": 10,
      "fullName": "Alice",
      "email": "alice@example.com",
      "skills": ["Java", "Spring Boot", "REST API"],
      "yearsOfExperience": 5,
      "rating": 4.5,
      "completedProjects": 12
    }
  ],
  "maxTeamSize": 5
}
```

Réponse :
- `team` : liste de { freelancerId, role, isLeader, score, matchRationale }
- `technicalLeaderId` : id du leader proposé
- `notificationsToSend` : liste de { freelancerId, email, subject, message } pour notifier chaque membre
- `rationale` : explication de la composition

## Intégration à la plateforme

1. **Proxy Angular** : ajouter dans `proxy.conf.json` une règle pour `/api/ai` → `http://localhost:5000` (ou l’URL du service).
2. **Frontend** : appeler `POST /api/ai/analyze-project` à la création/saisie du projet (optionnel : pré-remplir compétences et budget), puis après création du projet appeler `POST /api/ai/build-team` avec l’analyse + la liste des freelancers (récupérée via l’API existante). Afficher l’équipe proposée et un bouton « Notifier l’équipe » qui enverra les notifications (côté backend à implémenter si besoin).
3. **Backend (Gateway)** : ajouter une route vers le service Team AI (ex. `/api/ai/**` → `http://team-ai:5000`) pour que le front appelle via la même origine.
4. **Notifications** : le backend peut consommer `notificationsToSend` pour envoyer des emails ou notifications in-app (à implémenter selon votre stack).

## Structure du code

```
team-ai/
├── app/
│   ├── __init__.py
│   ├── config.py          # Settings (port, LLM, etc.)
│   ├── main.py            # FastAPI app, CORS
│   ├── api/
│   │   ├── __init__.py
│   │   └── routes.py      # POST /api/analyze-project, /api/build-team
│   ├── models/
│   │   ├── __init__.py
│   │   └── schemas.py     # Pydantic (AnalyzeRequest/Response, BuildTeamRequest/Response)
│   └── services/
│       ├── __init__.py
│       ├── analyzer.py    # ProjectAnalyzer (NLP + option LLM)
│       └── team_builder.py # TeamBuilder (sélection équipe + leader + notifications)
├── requirements.txt
├── run.py
└── README.md
```
