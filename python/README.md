# Python — Rappels & ETL (MatchFreelance)

Ce dossier contient deux modules Python qui s’appuient sur les microservices (via la **Gateway** `http://localhost:8050`) :

1. **Service de rappels** : détecte formations ouvertes, examens à passer, certificats et envoie des **emails (SMTP)** ou **webhooks**.
2. **Pipeline ETL** : agrège Skill, Formation, Evaluation (et utilisateurs si `/api/users` existe) dans une **base SQLite** pour tableaux de bord et rapports.

---

## Prérequis

- Python 3.10+
- Backend démarré (Eureka, Gateway, Formation, Evaluation, Skill, etc.) pour que les API répondent.

```bash
cd python
pip install -r requirements.txt
cp .env.example .env
# Éditer .env avec vos paramètres (API_BASE_URL, SMTP ou WEBHOOK_URL, ETL_DB_PATH)
```

---

## 1. Service de rappels (`rappels.py`)

Interroge les microservices pour construire des rappels par freelancer, puis envoie un **email digest** (SMTP) ou un **POST JSON** vers une URL (webhook).

### Détection

- **Formation ouverte** : formations avec statut OUVERTE (Formation).
- **Examen à passer** : inscriptions validées pour lesquelles le freelancer n’a pas encore passé l’examen de la formation (Evaluation).
- **Nouveau certificat** : certificats obtenus par le freelancer (Evaluation).

### Configuration (.env)

- `API_BASE_URL` : URL de la Gateway (ex. `http://localhost:8050`).
- `API_TOKEN` : optionnel, si les routes exigent un JWT.
- **SMTP** : `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM` pour l’envoi d’emails.
- **Webhook** : `WEBHOOK_URL` pour envoyer les rappels en POST JSON au lieu d’email.
- **Gmail** : utiliser `smtp.gmail.com`, port 587, et un **mot de passe d’application** Google (Compte Google → Sécurité → Validation en 2 étapes → Mots de passe des applications).

### Lancement

```bash
# Test sans envoi (affiche les rappels dans la console)
python rappels.py --dry-run

# Envoi réel (SMTP ou webhook selon la config)
python rappels.py
```

### Cron (exemple : tous les jours à 9h)

```cron
0 9 * * * cd /chemin/vers/validation/python && python rappels.py
```

---

## 2. Pipeline ETL (`etl.py`)

Charge les données des API dans une base **SQLite** (`data/analytics.db` par défaut). Utile pour des requêtes SQL, rapports ou tableaux de bord externes.

### Tables créées

| Table         | Source API                          |
|---------------|-------------------------------------|
| `formations`  | GET /api/formations                 |
| `inscriptions`| GET /api/inscriptions/formation/{id}|
| `examens`     | GET /api/examens/formation/{id}     |
| `passages`    | GET /api/examens/resultats/freelancer/{id} |
| `certificats` | GET /api/certificats/freelancer/{id} |
| `skills`      | GET /api/skills ou /api/skills/freelancer/{id} |

### Configuration (.env)

- `API_BASE_URL` : URL de la Gateway.
- `ETL_DB_PATH` : chemin du fichier SQLite (ex. `./data/analytics.db`).

### Lancement

```bash
python etl.py
```

### Exemples de requêtes (rapports / tableaux de bord)

```sql
-- Nombre d'inscriptions par formation
SELECT formation_titre, statut, COUNT(*) FROM inscriptions GROUP BY formation_id, statut;

-- Freelancers avec au moins un certificat
SELECT u.email, COUNT(c.id) AS nb_certificats
FROM users u
JOIN certificats c ON c.freelancer_id = u.id
WHERE u.role = 'FREELANCER'
GROUP BY u.id;

-- Formations les plus suivies
SELECT f.titre, COUNT(i.id) AS nb_inscriptions
FROM formations f
LEFT JOIN inscriptions i ON i.formation_id = f.id AND i.statut = 'VALIDEE'
GROUP BY f.id
ORDER BY nb_inscriptions DESC;
```

### Cron (exemple : tous les jours à 2h)

```cron
0 2 * * * cd /chemin/vers/validation/python && python etl.py
```

---

## Structure

```
python/
├── .env.example
├── config.py       # Chargement des variables d'environnement
├── rappels.py      # Service de rappels (email / webhook)
├── etl.py          # Pipeline ETL → SQLite
├── requirements.txt
├── README.md
└── data/           # Créé à la première exécution (ETL)
    └── analytics.db
```
