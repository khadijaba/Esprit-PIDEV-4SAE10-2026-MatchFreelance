# Guide de test étape par étape — Python (MatchFreelance)

Suivez ces étapes dans l’ordre pour voir les résultats de l’ETL, des analytics, des rappels et des recommandations.

---

## Prérequis

- **Python 3** installé sur ta machine.
- **Backend démarré** : Eureka, Gateway (port 8050), et au moins les microservices **User**, **Formation**, **Evaluation** (et **Skill** pour les recommandations).
- Dans le dossier **`python`** : fichier **`.env`** configuré (voir `.env.example`), avec au minimum :
  - `API_BASE_URL=http://localhost:8050`
  - Pour les rappels par email : `SMTP_HOST`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`.

---

## Étape 1 — Vérifier que l’API répond

1. Ouvre un terminal.
2. Va dans le dossier du projet, puis dans `python` :
   ```bash
   cd chemin/vers/validation/python
   ```
3. Lance :
   ```bash
   python -c "import requests; r=requests.get('http://localhost:8050/api/formations'); print('OK' if r.status_code==200 else r.status_code)"
   ```
4. **Résultat attendu** : tu dois voir `OK` affiché.  
   Si tu as une erreur de connexion, démarre d’abord Eureka, la Gateway et les microservices.

---

## Étape 2 — ETL : charger les données dans SQLite

1. Dans le même terminal (toujours dans `python`) :
   ```bash
   python etl.py
   ```
2. **Résultat attendu** : un résumé du type :
   ```text
   [ETL] Terminé: {'users': ..., 'formations': ..., 'inscriptions': ..., ...}
   [ETL] Base: .../python/data/analytics.db
   ```
3. **Où voir le résultat** : la base SQLite est créée dans **`python/data/analytics.db`**. Tu peux l’ouvrir avec un outil SQLite (DB Browser, DBeaver, etc.) pour voir les tables `users`, `formations`, `inscriptions`, `examens`, `passages`, `certificats`, `skills`.

---

## Étape 3 — Analytics : indicateurs, CSV et graphiques

1. Toujours dans `python` :
   ```bash
   python analytics.py
   ```
2. **Résultat attendu** : des lignes du type :
   ```text
   [Analytics] CSV écrit : .../python/reports/indicateurs_formations.csv
   [Analytics] CSV écrit : .../python/reports/indicateurs_freelancers.csv
   [Analytics] Graphique écrit : .../python/reports/graph_inscriptions_par_formation.png
   [Analytics] Graphique écrit : .../python/reports/graph_certificats_par_mois.png
   [Analytics] Terminé : {...}
   [Analytics] Ouvrez le dossier 'reports' pour voir les CSV et graphiques.
   ```
3. **Où voir les résultats** :
   - Ouvre le dossier **`python/reports/`**.
   - Ouvre **`indicateurs_formations.csv`** (Excel ou éditeur de texte) : taux d’inscription, passage, réussite par formation.
   - Ouvre **`indicateurs_freelancers.csv`** : nombre de certificats, formations, score moyen par freelancer.
   - Ouvre les fichiers **`.png`** pour voir les graphiques (inscriptions par formation, certificats par mois).

**Note** : si tu n’as pas `matplotlib`, installe-le : `pip install matplotlib`, puis relance `python analytics.py`.

---

## Étape 4 — Rappels : test sans envoyer d’emails (dry-run)

1. Dans `python` :
   ```bash
   python rappels.py --dry-run
   ```
2. **Résultat attendu** : le script affiche pour chaque freelancer les rappels qui *seraient* envoyés (formations ouvertes, examens à passer, formation se termine, examen en retard, etc.), **sans envoyer d’email**.
3. **Pour vraiment envoyer les emails** : enlève `--dry-run` (et vérifie ton `.env` SMTP) :
   ```bash
   python rappels.py
   ```
4. **Où voir le résultat** : dans le terminal (dry-run) ou dans ta boîte mail (envoi réel). Les adresses dans `RAPPELS_ALWAYS_SEND_TO` reçoivent toujours un email si SMTP est configuré.

---

## Étape 5 — Recommandations : scores et parcours

1. Dans `python` :
   ```bash
   python recommendations.py
   ```
2. **Résultat attendu** : dans le terminal, tu vois par exemple :
   - Les **scores de complétion** (top 5) pour les freelancers.
   - Des exemples de **parcours prérequis** (formation → cert → formation…).
   - Un exemple de **recommandations de formations** pour un freelancer (selon compétences et certificats).
3. **Où voir le résultat** : tout s’affiche dans la **console**. Tu peux adapter le script plus tard pour écrire ces résultats dans un fichier JSON ou CSV si tu veux les réutiliser.

---

## Récapitulatif — ordre des commandes

| Étape | Commande              | Où voir le résultat                          |
|-------|------------------------|----------------------------------------------|
| 1     | Test API (voir ci-dessus) | Terminal : `OK`                             |
| 2     | `python etl.py`        | Terminal + fichier `data/analytics.db`      |
| 3     | `python analytics.py`  | Dossier **`reports/`** (CSV + PNG)          |
| 4     | `python rappels.py --dry-run` | Terminal (liste des rappels)           |
| 5     | `python recommendations.py` | Terminal (scores, parcours, recommandations) |

---

## Dépannage rapide

- **« Base ETL absente »** : exécute d’abord **Étape 2** (`python etl.py`).
- **Erreur de connexion / 404** : vérifie que la Gateway et les microservices tournent (port 8050, Eureka).
- **Aucun freelancer trouvé** : vérifie que des utilisateurs avec le rôle `FREELANCER` existent dans le microservice User.
- **Pas de graphiques** : `pip install matplotlib` puis relance `python analytics.py`.

Une fois ces étapes faites, tu as vu tous les résultats : ETL en base, analytics dans `reports/`, rappels en dry-run (ou par email), et recommandations dans la console.
