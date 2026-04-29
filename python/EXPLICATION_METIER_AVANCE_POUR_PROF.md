# Explication du métier avancé — Projet MatchFreelance (Python)

Document pour présenter à la professeure comment le **métier avancé** a été travaillé dans le projet.

---

## 1. Contexte du projet

- **Plateforme** : MatchFreelance (mise en relation freelancers / formations / certificats).
- **Backend** : architecture **microservices** (Spring Boot) : User, Formation, Evaluation, Skill, avec une **Gateway** et **Eureka**.
- **Frontend** : Angular (fiches formations, inscriptions, examens, dashboards).
- **Partie Python** : scripts qui s’appuient sur les **APIs** exposées par la Gateway pour ajouter des fonctionnalités **métier avancées** sans modifier le cœur Java : **ETL, analytics, rappels, scoring et recommandations**.

L’idée du métier avancé : exploiter les données des microservices (User, Formation, Evaluation, Skill) avec des **scripts Python** pour fournir **indicateurs**, **rappels**, **recommandations** et **parcours**, de façon automatisée et reproductible.

---

## 2. Ce qu’on appelle « métier avancé » ici

Le **métier avancé** correspond à trois blocs de fonctionnalités, tous implémentés en Python et documentés :

| Bloc | Objectif métier | Fichiers Python |
|------|------------------|-----------------|
| **1. ETL & Analytics** | Avoir une vue agrégée des données (formations, inscriptions, examens, certificats) pour calculer des **indicateurs** et les **voir** (CSV, graphiques). | `etl.py`, `analytics.py` |
| **2. Rappels & Notifications** | Relancer les freelancers au bon moment : formations ouvertes, **examen à passer**, **formation qui se termine**, **examen non passé depuis X jours**, avec **examen bien mis en avant dans l’email** et possibilité d’envoyer vers **Slack/Teams**. | `rappels.py`, `config.py` |
| **3. Scoring & Recommandations** | Donner un **score de complétion** (0–100 %) par freelancer, **recommandations de formations** selon compétences/certificats, et **parcours avec prérequis en chaîne** (formation C → cert B → formation B → …). | `recommendations.py` |

Chaque bloc répond à un **besoin métier** précis (pilotage, engagement, personnalisation) et utilise les **même APIs** que le frontend, mais côté Python pour l’automatisation et le traitement par lots.

---

## 3. Comment chaque bloc a été travaillé

### 3.1 ETL & Analytics

**Objectif** : centraliser les données des microservices dans une base SQLite pour calculer des indicateurs et les visualiser.

**Fonctionnement** :

1. **ETL (`etl.py`)**  
   - Appels HTTP GET à la Gateway (`/api/users`, `/api/formations`, `/api/inscriptions/formation/{id}`, `/api/examens/formation/{id}`, `/api/examens/resultats/freelancer/{id}`, `/api/certificats/freelancer/{id}`, `/api/skills/...`).  
   - Les réponses sont chargées dans des **tables SQLite** (users, formations, inscriptions, examens, passages, certificats, skills).  
   - C’est un **pipeline Extract – Transform – Load** : extraction depuis les APIs, transformation minimale (mapping champs), chargement en base.

2. **Analytics (`analytics.py`)**  
   - Lit la base SQLite produite par l’ETL.  
   - Calcule des **indicateurs par formation** : taux d’inscription (inscriptions / capacité), taux de passage à l’examen, taux de réussite (certificats délivrés).  
   - Calcule des **indicateurs par freelancer** : nombre de certificats, nombre de formations suivies, score moyen aux examens.  
   - **Exporte** ces indicateurs en **CSV** dans le dossier `reports/`.  
   - Génère des **graphiques** (matplotlib) : inscriptions par formation, certificats par mois, sauvegardés en PNG dans `reports/`.

**Résultat visible** : après `python etl.py` puis `python analytics.py`, la professeure peut **ouvrir le dossier `python/reports/`** pour voir les CSV et les graphiques (preuve du métier avancé « indicateurs et visualisation »).

---

### 3.2 Rappels & Notifications

**Objectif** : envoyer au bon moment des rappels utiles aux freelancers (formations, examens) et mettre clairement **l’examen** dans l’email ; permettre aussi l’envoi vers Slack/Teams.

**Fonctionnement** :

1. **Données utilisées**  
   - Liste des freelancers (User), formations ouvertes (Formation), inscriptions et statuts (Formation), résultats d’examens et certificats (Evaluation).  
   - Tout est récupéré via des **GET** sur la Gateway (comme pour l’ETL).

2. **Types de rappels implémentés**  
   - **Formations ouvertes** : rappel qu’il existe des formations disponibles.  
   - **Examen à passer** : inscription validée mais examen pas encore passé ; dans l’email on met bien **« Examen : [titre] »** et un **lien direct** vers la page « passer l’examen » (formationId + examenId).  
   - **Formation se termine dans X jours** : pour les formations dont la date de fin est dans les X prochains jours (ex. 7), rappel aux freelancers inscrits.  
   - **Examen non passé après X jours** : inscription validée depuis plus de X jours (ex. 14) sans avoir passé l’examen → rappel avec **examen** et lien.  
   - **Certificats** : rappel sur les certificats obtenus.

3. **Email**  
   - Contenu **texte + HTML**, avec **examen** bien identifié (libellé + lien).  
   - Envoi via **SMTP** (config dans `.env`).  
   - Possibilité d’envoyer aussi vers **Slack/Teams** : si `WEBHOOK_SLACK=true`, le payload est formaté pour être lisible dans un canal (webhook).

**Résultat visible** :  
- En **dry-run** (`python rappels.py --dry-run`) : dans le terminal, liste des rappels qui seraient envoyés (sans envoi réel).  
- En **envoi réel** : les freelancers (et les adresses dans `RAPPELS_ALWAYS_SEND_TO`) reçoivent les emails avec l’examen bien mis en avant.

---

### 3.3 Scoring & Recommandations

**Objectif** : donner un **score de complétion** par freelancer, **recommandations de formations** personnalisées, et les **parcours avec prérequis en chaîne**.

**Fonctionnement** :

1. **Score de complétion (0–100 %)**  
   - Pour chaque freelancer :  
     - Partie « certificats » (ex. 40 % : nombre de certificats, plafonné).  
     - Partie « formations validées » (ex. 30 % : nombre d’inscriptions validées).  
     - Partie « examens » (ex. 30 % : score moyen aux examens, normalisé).  
   - Formule implémentée dans `recommendations.py` ; les données viennent des APIs Formation et Evaluation (inscriptions, résultats, certificats).

2. **Recommandation de formations**  
   - On prend les **formations ouvertes** et les **certificats** déjà obtenus par le freelancer.  
   - On écarte les formations pour lesquelles il a déjà le certificat.  
   - On priorise les formations dont le **type** correspond aux **compétences** du freelancer (API Skill).  
   - Résultat : une liste ordonnée de formations recommandées (ex. top 10).

3. **Parcours avec prérequis en chaîne**  
   - Certaines formations ont un **examen requis** (`examenRequisId`) : il faut avoir le certificat d’un examen pour s’inscrire.  
   - Chaque examen est lié à une formation.  
   - On construit donc des **chaînes** du type : Formation C exige certificat B → certificat B obtenu après formation B → formation B exige éventuellement certificat A → …  
   - Le script Python parcourt ces dépendances (Formation ↔ Examen) et expose, pour chaque formation concernée, la **liste ordonnée des prérequis** (parcours à suivre).

**Résultat visible** : en lançant `python recommendations.py`, la console affiche les scores, des exemples de recommandations et les parcours prérequis (preuve du métier avancé « scoring et recommandations »).

---

## 4. Architecture simplifiée (schéma de principe)

```
  [Gateway : port 8050]
         │
         │  GET /api/users, /api/formations, /api/inscriptions, ...
         ▼
  ┌──────────────────────────────────────────────────────────────┐
  │  Scripts Python (dossier python/)                             │
  │                                                               │
  │  etl.py          →  SQLite (data/analytics.db)                 │
  │       analytics.py →  reports/*.csv, reports/*.png              │
  │                                                               │
  │  rappels.py      →  Emails (SMTP) ou Webhook (Slack/Teams)     │
  │                                                               │
  │  recommendations.py →  Scores, recommandations, parcours      │
  │                       (affichage console / réutilisable)       │
  └──────────────────────────────────────────────────────────────┘
```

- Les **microservices** (User, Formation, Evaluation, Skill) ne sont pas modifiés pour le métier avancé : ils exposent déjà les APIs.  
- Le **métier avancé** est entièrement dans les **scripts Python** qui consomment ces APIs et produisent : base SQLite, rapports, emails, scores et recommandations.

---

## 5. Récapitulatif pour la professeure

- **Métier avancé** = trois blocs : **ETL & Analytics**, **Rappels & Notifications**, **Scoring & Recommandations**.  
- **Technologie** : Python (requests, sqlite3, matplotlib, smtplib), configuration via `.env`.  
- **Données** : uniquement via les **APIs** de la Gateway (aucune base métier côté Python, sauf la base ETL SQLite pour les indicateurs).  
- **Preuves** :  
  - **ETL & Analytics** : dossier `python/reports/` (CSV + graphiques) après `etl.py` + `analytics.py`.  
  - **Rappels** : sortie de `rappels.py --dry-run` ou emails reçus ; examen bien présent dans l’email.  
  - **Scoring & Recommandations** : sortie console de `recommendations.py`.  

Ce document peut servir de base pour un oral ou un rapport expliquant **comment le métier avancé a été travaillé** dans le projet MatchFreelance.
