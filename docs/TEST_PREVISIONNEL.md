# Tester le prévisionnel (décrochage + prédiction réussite)

La page **Prévisionnel** affiche :
1. **Détection de décrochage** : inscriptions validées sans passage d’examen après X jours (défaut 14).
2. **Prédiction de réussite à l’examen** : probabilité de réussite (score ≥ 60 %) pour les examens pas encore passés.

Les données viennent du script Python `python/ml_predictions.py` qui appelle l’API (Gateway 8050) et écrit `frontend/public/reports/ml_predictions.json`. Le frontend charge ce JSON.

---

## 1. Tester la détection de décrochage

### Données nécessaires

- Au moins **1 freelancer** (User avec rôle FREELANCER).
- **1 formation** avec au moins **1 examen** associé (microservice Evaluation).
- **1 inscription** de ce freelancer à cette formation, avec le statut **VALIDEE**.
- Le freelancer **n’a pas passé** cet examen.
- Par défaut : l’inscription doit dater d’**au moins 14 jours** pour être considérée en décrochage. Pour tester tout de suite, on utilise **0 jour** (voir ci‑dessous).

### Étapes CRUD (ordre conseillé)

1. **Créer un freelancer** (s’il n’existe pas)  
   - Frontend : **S’inscrire** avec le rôle Freelancer, ou Admin → utilisateurs.  
   - Noter l’**ID** du freelancer (ex. `6`).

2. **Créer une formation**  
   - Admin → Formations → **Nouvelle formation**.  
   - Renseigner titre, type, dates, etc. → **Créer**.  
   - Noter l’**ID** de la formation (ex. `1`).

3. **Créer un examen pour cette formation**  
   - Admin → Examens (ou le chemin utilisé dans votre app) → **Créer un examen** lié à la formation créée.  
   - Noter l’**ID** de l’examen.

4. **Inscrire le freelancer à la formation**  
   - Soit depuis le frontend (page formation → S’inscrire).  
   - Soit en API :
     ```http
     POST http://localhost:8050/api/inscriptions/formation/1/freelancer/6
     ```
   - Remplacer `1` et `6` par l’ID formation et l’ID freelancer.

5. **Valider l’inscription**  
   - Admin : liste des inscriptions en attente → **Valider** pour cette inscription.  
   - Ou en API (avec l’**ID de l’inscription**, pas du freelancer) :
     ```http
     PATCH http://localhost:8050/api/inscriptions/{id_inscription}/valider
     ```

6. **Ne pas passer l’examen**  
   - Ne pas aller sur « Passer l’examen » pour ce freelancer / cet examen. Ainsi, l’inscription reste « validée sans passage ».

7. **Utiliser 0 jour pour le test (décrochage immédiat)**  
   - Par défaut, le script considère le décrochage après **14 jours**. Pour voir un décrochage tout de suite :
     - Dans le dossier `python`, créer ou éditer un fichier **`.env`** et ajouter :
       ```env
       JOURS_DECROCHAGE=0
       ```
     - Ou en ligne de commande (Windows PowerShell) :
       ```powershell
       $env:JOURS_DECROCHAGE="0"; python ml_predictions.py
       ```
     - Sous Linux/Mac :
       ```bash
       JOURS_DECROCHAGE=0 python ml_predictions.py
       ```

8. **Lancer le script et rafraîchir la page**  
   - Backend (Gateway, Formation, Evaluation, User, etc.) doit tourner.  
   - Dans le dossier `python` :
     ```bash
     pip install -r requirements.txt
     python ml_predictions.py
     ```
   - Le script écrit `frontend/public/reports/ml_predictions.json`.  
   - Ouvrir la page **Prévisionnel** (ou la rafraîchir) : la section « Détection de décrochage » doit afficher au moins une ligne (freelancer, formation, jours depuis inscription, risque).

---

## 2. Tester la prédiction de réussite à l’examen

### Données nécessaires

- Le modèle utilise une **régression logistique** sur les historiques de passage.  
- Il faut **au moins 10 résultats d’examens** (passages avec score) dans l’API pour que le script entraîne le modèle et produise des prédictions.
- En plus : des freelancers avec **inscription validée** à une formation qui a un **examen qu’ils n’ont pas encore passé**.

### Étapes

1. **Créer beaucoup de résultats d’examens**  
   - Plusieurs freelancers passent plusieurs examens (avec des scores variés, certains ≥ 60 %, d’autres &lt; 60 %).  
   - Au moins **10 passages** au total (ex. 2 freelancers × 5 examens, ou 10 freelancers × 1 examen).

2. **Avoir au moins un cas « inscription validée + examen non passé »**  
   - Comme en section 1 : un freelancer inscrit et validé à une formation qui a un examen, sans avoir passé cet examen.

3. **Lancer le script**  
   - Même commande que ci‑dessus : `python ml_predictions.py`.  
   - Si les données sont suffisantes, `exam_success_predictions` dans le JSON sera rempli et la section « Prédiction de réussite à l’examen » affichera des lignes (freelancer, examen, formation, probabilité en %).

---

## 3. Vérifier sans lancer le script (exemple de JSON)

Pour vérifier **uniquement l’affichage** de la page (décrochage + prédiction), vous pouvez utiliser le fichier d’exemple :

1. Copier **`frontend/public/reports/ml_predictions_example.json`** vers **`frontend/public/reports/ml_predictions.json`** (ou coller le contenu ci‑dessous dans `ml_predictions.json`).
2. Démarrer ou rafraîchir l’app Angular et ouvrir la page **Prévisionnel**.

Vous devriez voir **1 décrochage** et **1 prédiction de réussite**.

```json
{
  "dropouts": [
    {
      "freelancer_id": 6,
      "email": "freelancer@test.com",
      "fullName": "Jean Dupont",
      "inscription_id": 1,
      "formation_id": 1,
      "formation_titre": "Spring Boot avancé",
      "jours_depuis_inscription": 18,
      "risk": "medium"
    }
  ],
  "project_recommendations_by_freelancer": {},
  "exam_success_predictions": [
    {
      "freelancer_id": 6,
      "email": "freelancer@test.com",
      "fullName": "Jean Dupont",
      "examen_id": 1,
      "examen_titre": "Examen Spring Boot",
      "formation_id": 1,
      "formation_titre": "Spring Boot avancé",
      "proba_reussite": 72.5
    }
  ],
  "meta": {
    "jours_decrochage": 14,
    "seuil_reussite_examen": 60,
    "project_recommendation_note": null,
    "projects_count": 0
  }
}
```

---

## Résumé des URLs utiles (avec Gateway sur 8050)

| Action | Méthode | URL |
|--------|--------|-----|
| Liste des utilisateurs | GET | http://localhost:8050/api/users |
| Liste des formations | GET | http://localhost:8050/api/formations |
| Examens d’une formation | GET | http://localhost:8050/api/examens/formation/{formationId} |
| Inscrire un freelancer | POST | http://localhost:8050/api/inscriptions/formation/{formationId}/freelancer/{freelancerId} |
| Valider une inscription | PATCH | http://localhost:8050/api/inscriptions/{id}/valider |
| Inscriptions d’un freelancer | GET | http://localhost:8050/api/inscriptions/freelancer/{freelancerId} |

Avant de lancer `ml_predictions.py`, vérifier que **API_BASE_URL** pointe bien vers la Gateway (ex. `http://localhost:8050` dans `python/.env` ou dans `config.py`).
