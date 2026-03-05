# Contrat API Auth (aligné avec le microservice User)

## Gateway

- **Route** : `/api/users/**` → `stripPrefix(1)` → service **USER** reçoit `/users/**`.
- **Proxy frontend** : `ng serve` envoie `/api` vers `http://localhost:8082`.

## 1. POST `/api/users/auth/register` (backend: `/users/auth/register`)

**Body (JSON)** :
```json
{
  "email": "string",
  "password": "string",
  "fullName": "string",
  "role": "FREELANCER" | "PROJECT_OWNER" | "ADMIN"
}
```

Le formulaire frontend envoie `firstName` + `lastName` (ou `username`) ; le service construit `fullName` avant l’envoi.

## 2. POST `/api/users/auth/login` (backend: `/users/auth/login`)

**Body (JSON)** :
```json
{
  "email": "string",
  "password": "string"
}
```

**Réponse (200/201)** : `{ "token": "...", "user": { ... }, "role": "FREELANCER" }`

---

## Erreur « 0 Unknown Error » ou « Http failure response ... 0 »

Cela signifie que **le frontend n’arrive pas à contacter le backend** (connexion refusée ou bloquée).

À faire :

1. **Démarrer la Gateway** sur le port **8086** (voir `proxy.conf.json` : toutes les requêtes `/api` sont envoyées vers `http://localhost:8086`).
2. **Démarrer le microservice User** sur le port configuré dans la Gateway (ex. 8085).
3. **Lancer le front avec le proxy** : depuis le dossier `frontend`, exécuter `ng serve` (et non un serveur qui ne charge pas `proxy.conf.json`). Le proxy n’est actif qu’avec `ng serve`.

Si la Gateway tourne sur un **autre port**, adapter la cible dans `frontend/proxy.conf.json` (ex. `"target": "http://localhost:9090"`).

---

## En cas d’erreur 500 à la connexion

1. **Vérifier que le backend tourne**  
   - API Gateway (ex. port 8086, voir `proxy.conf.json`)  
   - Microservice **User** (ex. 8085)  
   - Base de données (MySQL, etc.)

2. **Voir le message exact du backend**  
   - F12 → onglet **Réseau** → refaire une connexion → cliquer sur la requête `auth/login` → onglet **Réponse** : le corps contient souvent le message d’erreur (ex. exception Java).  
   - Dans la **console** du navigateur (F12), en cas de 500 le frontend affiche aussi `[Login 500] Backend error: ...`.

3. **Causes fréquentes côté backend**  
   - Base de données injoignable ou schéma/tables manquants  
   - Champ attendu différent (ex. `username` au lieu de `email`) : le front envoie `{ "email", "password" }`  
   - Exception non gérée dans le contrôleur ou le service (vérifier les logs du service User)
