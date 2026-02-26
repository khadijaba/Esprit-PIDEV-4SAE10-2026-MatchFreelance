# Interview microservice — scope & fonctionnalités avancées

**Contexte** : Ce document concerne **uniquement le microservice Interview**. Les autres microservices (candidature, contract, project) sont gérés par d’autres membres du groupe ; les fonctionnalités ci‑dessous restent **dans le périmètre du microservice Interview** (pas de dépendance métier vers les autres services pour leur implémentation).

---

## 1. Fonctionnalités avancées (à ajouter au microservice Interview)

Toutes ces fonctionnalités sont **uniquement pour le microservice Interview** (votre responsabilité) :

- **Notifications in-app**  
  Notifications liées aux entretiens : entretien proposé, confirmé, annulé, etc. Stockage et liste des notifications dans le périmètre Interview (ex. entité Notification ou équivalent, liée à un userId et à un interviewId). Affichage côté front (Angular) à partir des APIs du microservice Interview.

- **Rappels in-app**  
  Rappels pour les entretiens à venir (ex. « Entretien dans 24h », « Dans 1h »). Gérés dans l’interview microservice (règles de calcul des rappels, marquage lu, etc.) et exposés via API ; affichage in-app dans le front.

- **Politique d’annulation**  
  Règles métier dans l’interview microservice : annulation autorisée jusqu’à X heures avant le début ; au-delà, refus ou traitement spécifique (ex. statut NO_SHOW, blocage de l’annulation). Logique entièrement dans le service Interview.

- **Visio in-app**  
  Pour les entretiens en mode ONLINE : si possible, proposer une visio intégrée (WebRTC ou intégration type Daily.co/Whereby) plutôt qu’un simple lien externe. Le microservice Interview peut gérer la création de « room » ou le token nécessaire, et le front affiche l’embed.

- **Reviews (avis)**  
  Avis après un entretien : note + commentaire (ex. client note le freelancer, ou réciproque). Données et CRUD dans le microservice Interview (entité Review liée à un interviewId, reviewer, reviewee, score, comment). Agrégat (moyenne, etc.) peut être exposé par l’interview service pour affichage profil (le profil reste géré ailleurs ; l’interview service fournit seulement les données d’avis liées aux entretiens).

---

## 2. Input control (validation) sur le CRUD Interview

Des validations d’entrée ont été ajoutées sur les APIs du microservice Interview et Availability, là où c’est pertinent :

- **Création / mise à jour d’entretien** : champs requis selon le mode (ONLINE → meetingUrl ; FACE_TO_FACE → addressLine, city), slotId ou (startAt + endAt), endAt > startAt, tailles max (notes, url, adresse, ville), ids positifs.
- **Création de créneaux (slot)** : startAt et endAt obligatoires, endAt > startAt ; batch limité en taille (ex. 200 créneaux par requête).
- **Path variables** : ids (interviewId, freelancerId, slotId) validés positifs ; en cas d’erreur, retour 400 avec message clair.

Les détails sont dans les DTOs (annotations Bean Validation), les validateurs personnalisés éventuels, et le `@ControllerAdvice` qui renvoie 400 pour les erreurs de validation.

---

## 3. Implémentation des fonctionnalités avancées (interview microservice)

### Notifications in-app
- **Entité** : `Notification` (userId, interviewId, type, message, readAt, createdAt).
- **Types** : INTERVIEW_PROPOSED, INTERVIEW_CONFIRMED, INTERVIEW_CANCELLED, INTERVIEW_COMPLETED, INTERVIEW_NO_SHOW, REMINDER_24H, REMINDER_1H.
- **Création** : automatique à la création / confirmation / annulation / complétion / no-show d’un entretien (notification envoyée au freelancer et au owner).
- **API** :
  - `GET /api/notifications/users/{userId}` : liste paginée des notifications.
  - `POST /api/notifications/{notificationId}/read?userId=`
  - `POST /api/notifications/users/{userId}/read-all`

### Rappels in-app
- **Types** : REMINDER_24H, REMINDER_1H (stockés comme notifications).
- **Job** : `ReminderScheduler` (cron toutes les 15 min) crée les rappels pour les entretiens CONFIRMED dont le début est dans ~24h ou ~1h.
- **Config** : `interview.reminders.cron=0 */15 * * * *`

### Politique d’annulation
- **Config** : `interview.cancellation.allowed-hours-before=24` (annulation autorisée jusqu’à 24h avant).
- **Comportement** : `POST .../cancel` refuse avec un message clair si moins de X heures avant le début.
- **NO_SHOW** : `POST /api/interviews/{interviewId}/no-show` permet de marquer une absence non excusée quand l’annulation n’est plus possible.

### Visio in-app
- **Champ** : `Interview.visioRoomId` (optionnel).
- **API** : `GET /api/interviews/{interviewId}/visio-room` (uniquement pour mode ONLINE) : crée ou retourne un roomId et un joinUrl (meetingUrl existant ou `/visio/{roomId}` pour embed côté front).

### Reviews (avis)
- **Entité** : `Review` (interviewId, reviewerId, revieweeId, score 1–5, comment). Contrainte unique (interview_id, reviewer_id).
- **API** :
  - `POST /api/interviews/{interviewId}/reviews?reviewerId=` + body (revieweeId, score, comment) : création après entretien COMPLETED.
  - `GET /api/interviews/{interviewId}/reviews` : avis d’un entretien.
  - `GET /api/reviews/reviewee/{revieweeId}` : avis reçus par un utilisateur (pour profil).
  - `GET /api/reviews/reviewee/{revieweeId}/aggregate` : moyenne et nombre d’avis.

**Gateway** : les routes `/api/notifications/**` et `/api/reviews/**` sont configurées vers l’interview-service.
