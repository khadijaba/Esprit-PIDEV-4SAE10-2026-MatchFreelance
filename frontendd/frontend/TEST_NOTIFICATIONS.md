# Comment tester les notifications

## Prérequis

1. **Backend principal** (port 8086) : démarré (projets, users, skills, auth).
2. **Service Python team-ai** (port 5000) : démarré pour le calcul des freelancers compatibles (score ≥ 70 %).
3. **Frontend Angular** : `ng serve` avec le proxy (`proxy.conf.json`).

```bash
# Terminal 1 – backend (ex. Spring)
# À la racine de ton backend
# ...

# Terminal 2 – Python team-ai
cd <dossier team-ai>
python app.py   # ou la commande pour lancer le service sur 5000

# Terminal 3 – frontend
cd frontend
npm start
# ou: npx ng serve --proxy-config proxy.conf.json
```

Ouvre **http://localhost:4200**.

---

## Scénario de test (même navigateur, sans API notifications)

Les notifications sont d’abord stockées en **localStorage**. Pour les voir en tant que freelancer, il faut utiliser **le même navigateur** où le projet a été créé.

### Étape 1 : Compte freelancer avec des compétences

- Inscris-toi ou connecte-toi avec un compte **FREELANCER**.
- Va dans **Mon espace** → **Compétences** (`/freelancer/skills`).
- Ajoute des compétences (ex. « React », « Node.js ») pour que le Python puisse calculer un score ≥ 70 % pour un projet qui demande ces compétences.
- Note l’**ID** du freelancer (si tu peux le voir dans l’URL ou le profil), ou retiens simplement l’email du compte.

### Étape 2 : Créer un projet (client / project owner)

- Déconnecte-toi, puis connecte-toi avec un compte **CLIENT / PROJECT OWNER**.
- Va sur **Mes projets** → **Créer un projet** :  
  **http://localhost:4200/project-owner/projects/new**
- Remplis le formulaire et ajoute des **compétences requises** qui correspondent au freelancer (ex. « React », « Node.js »).
- Enregistre le projet.

Résultat attendu :

- Un toast du type : « X freelancer(s) compatible(s) notifié(s). Ils verront la notification dans leur espace. »
- Si le message n’apparaît pas : le Python (port 5000) n’est peut‑être pas démarré, ou aucun freelancer n’a un score ≥ 70 % (vérifier les skills en base et le calcul côté team-ai).

### Étape 3 : Voir les notifications en tant que freelancer

- Reste dans le **même navigateur**.
- Déconnecte-toi du client, puis connecte-toi avec le compte **FREELANCER** qui a les compétences correspondantes.
- Va sur **Notifications** :
  - soit en cliquant sur l’**icône cloche** dans le header puis « Voir toutes les notifications »,
  - soit directement : **http://localhost:4200/freelancer/notifications**

Tu devrais voir la notification du projet créé à l’étape 2.

---

## Vérifications si rien n’apparaît

1. **Toast après création de projet**  
   - Si tu ne vois pas « X freelancer(s) compatible(s) notifié(s) » :  
     - Vérifier que le service Python tourne sur le port 5000.  
     - Vérifier que le proxy envoie bien `/api/ai` vers `http://localhost:5000`.  
     - Vérifier en base qu’il existe des freelancers avec des skills qui matchent les compétences requises du projet.

2. **Même navigateur**  
   - Les notifications sont dans le **localStorage de l’onglet où le projet a été créé**.  
   - Le freelancer doit se connecter dans ce même navigateur (après déconnexion du client) pour voir les notifications sans API backend.

3. **ID du freelancer**  
   - La liste affichée est filtrée par `freelancerId` = l’ID du user connecté.  
   - Vérifier que le compte freelancer utilisé est bien celui qui a été inclus dans la réponse du Python (score ≥ 70 %).

4. **Console navigateur (F12)**  
   - Regarder les appels réseau :  
     - `POST /api/ai/compute-compatible-freelancers` (création de projet).  
     - En fallback, pas d’appel GET `/api/notifications/freelancer/...` si l’API notifications n’existe pas encore ; le front utilise alors le localStorage.

---

## Tester avec l’API notifications (backend)

Quand les endpoints décrits dans **NOTIFICATIONS_API.md** sont en place sur le backend (port 8086) :

- **POST /api/notifications/bulk** est appelé après la création du projet.
- En tant que freelancer, **GET /api/notifications/freelancer/:id** est appelé quand tu ouvres la page ou le panneau des notifications.
- Dans ce cas, les notifications s’affichent **même sur un autre appareil ou navigateur**, car elles viennent de la base de données.

Pour tester : crée un projet comme ci‑dessus, puis ouvre **un autre navigateur (ou mode privé)**, connecte-toi en freelancer et va sur `/freelancer/notifications`. Si l’API répond correctement, les notifications doivent apparaître.
