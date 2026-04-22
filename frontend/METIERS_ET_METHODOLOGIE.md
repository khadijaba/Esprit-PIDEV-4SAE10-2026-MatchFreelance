# Métiers couverts dans le projet & Comment je travaille dessus

## Combien de « métiers » sont avancés dans ce projet ?

Le projet **MatchFreelance** (frontend + backend + service IA) couvre **plusieurs métiers et domaines** avancés. Voici une vue structurée.

---

### 1. Rôles utilisateur (3 métiers « plateforme »)

| Rôle | Rôle technique | Fonction principale |
|------|----------------|---------------------|
| **Freelancer** | `FREELANCER` | Gestion du profil (CV, portfolios, compétences), évolution des compétences, parcours carrière, notifications, candidatures aux projets |
| **Project Owner (Client)** | `CLIENT` / `PROJECT_OWNER` | Création et gestion de projets, publication, réception des candidatures, messagerie |
| **Admin** | `ADMIN` | Backoffice : dashboard, projets, formations, examens, utilisateurs, compétences, supervision CV/portfolios, tableau de bord intelligence |

Ces trois rôles ont chacun des **routes dédiées**, des **guards** (accès réservé) et des **écrans spécifiques**.

---

### 2. Catégories de compétences = métiers techniques (9 domaines)

Les compétences des freelancers sont classées dans **9 catégories** (alignées avec les formations) :

| Catégorie | Libellé |
|-----------|--------|
| `WEB_DEVELOPMENT` | Développement Web |
| `MOBILE_DEVELOPMENT` | Développement Mobile |
| `AI` | Intelligence Artificielle |
| `DATA_SCIENCE` | Data Science |
| `DEVOPS` | DevOps |
| `CYBERSECURITY` | Cybersécurité |
| `DESIGN` | Design |
| `OTHER` | Autre |

Chaque compétence a un **niveau** (Junior → Expert) et des **années d’expérience**, utilisés pour le matching et l’évolution.

---

### 3. Grands blocs fonctionnels (métiers « fonctionnels » avancés)

- **Projets**  
  Création, édition, détail, liste (côté public, project-owner, admin). Compétences requises, Top 5 freelancers (matching), notifications automatiques aux freelancers compatibles (score ≥ 70 %).

- **Formations**  
  Liste, détail, inscription, parcours carrière (recommandations selon les compétences).

- **Examens & certificats**  
  Passer un examen, liste/détail examens, vue certificat.

- **Matching & IA**  
  - **TalentMatchingService** (frontend) : calcul Top 5 par compétences + expérience.  
  - **Team AI** (Python) : calcul des freelancers compatibles pour notifications (formule compétences 70 % + expérience 30 %).  
  - **Skill-gap** : écarts entre compétences requises et compétences du freelancer.  
  - **Career-path** : recommandations de formations selon le profil.

- **Notifications**  
  Envoi bulk après publication de projet (compatibles ≥ 70 %), persistance (API + repli localStorage), affichage dans l’espace freelancer.

- **Messagerie / discussions**  
  Liste des discussions, conversation (chat).

- **Supervision (admin)**  
  CV, portfolios, liste des utilisateurs avec filtres par rôle, profil utilisateur détaillé.

- **Intelligence (admin)**  
  Tableau de bord avec statistiques par catégorie, heatmap des performances, prédictions.

- **Évolution des compétences (freelancer)**  
  Score de profil, heatmap des compétences avec score par skill, répartition par niveau (Junior → Expert), historique des évolutions.

En résumé : **3 rôles plateforme**, **9 domaines de compétences**, et une **dizaine de blocs fonctionnels** (projets, formations, examens, matching, IA, notifications, messagerie, supervision, intelligence, évolution) sont avancés dans ce projet.

---

## Comment je travaille sur ce projet (méthodologie)

Quand tu me demandes une évolution ou une correction, je procède en général comme suit.

### 1. Comprendre la demande

- Je lis ton message pour identifier l’objectif (nouvelle fonctionnalité, bug, refonte UI, doc, etc.).

### 2. Explorer le code existant

- **Recherche sémantique** : je cherche où sont gérés les concepts (ex. « où est calculé le score de compatibilité », « où sont envoyées les notifications »).  
- **Grep** : pour retrouver des noms précis (services, composants, routes, rôles, catégories).  
- **Lecture ciblée** : je lis les fichiers concernés (composants, services, modèles, routes) pour ne pas casser l’existant.

### 3. Identifier les bons endroits à modifier

- **Frontend** : composants Angular (`.component.ts` / `.html`), services, modèles, guards, routes.  
- **Backend** : si le projet inclut un backend (ex. Java/Spring sur le port 8086), j’adapte les appels API ou les contrats.  
- **Service Python (Team AI)** : pour tout ce qui touche au calcul des freelancers compatibles, aux compétences, au matching (ex. `compatibility_notifier.py`, routes, schémas).

Je m’appuie sur la structure du repo (dossiers `frontend/`, `team-ai/`, etc.) et sur les noms de fichiers pour savoir où intervenir.

### 4. Proposer des changements précis

- J’édite les fichiers avec des **modifications ciblées** (search/replace ou réécriture de blocs).  
- J’évite de tout réécrire : je touche au minimum nécessaire pour atteindre ton objectif.  
- Si une tâche est grosse, je peux la découper en étapes (todo list) et les traiter une par une.

### 5. Cohérence et bonnes pratiques

- **Types** : respect des interfaces et modèles existants (TypeScript, Pydantic côté Python).  
- **API** : respect des contrats (body, query params, réponses) entre frontend, backend et Python.  
- **UX / design** : si tu demandes un style « type Fiverr », j’utilise les variables CSS et les composants déjà en place (ex. `fiverr-card`, couleurs, espacements).

### 6. Vérifications

- **Linter** : je peux lancer le linter sur les fichiers modifiés pour corriger les erreurs.  
- **Logs / debug** : pour les bugs (ex. « aucun freelancer notifié »), j’ajoute des logs côté Python ou frontend pour qu’on puisse tracer le flux (required_skills, users, scores).

En pratique, **je ne lance pas les serveurs à ta place** : tu exécutes `ng serve`, le backend et le service Python. En revanche, je peux proposer des commandes ou des scénarios de test (ex. « crée un projet avec compétences X, Y et regarde les logs Python »).

### 7. Documentation

- Si utile, j’ajoute ou mets à jour des fichiers comme `NOTIFICATIONS_API.md`, `TEST_NOTIFICATIONS.md`, ou ce fichier `METIERS_ET_METHODOLOGIE.md` pour que l’équipe s’y retrouve.

---

## Résumé

- **Métiers avancés** : 3 rôles (Freelancer, Project Owner, Admin), 9 catégories de compétences (métiers techniques), et une dizaine de blocs fonctionnels (projets, formations, examens, matching, IA, notifications, messagerie, supervision, intelligence, évolution des compétences).  
- **Comment je travaille** : comprendre ta demande → explorer le code (recherche, grep, lecture) → modifier les bons fichiers de façon ciblée → garder la cohérence types/API/design → proposer vérifications ou doc si besoin.

Si tu veux, on peut détailler un bloc précis (ex. « seulement le métier matching / IA » ou « seulement l’espace Freelancer ») dans un autre fichier ou dans le chat.
