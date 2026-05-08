# Idées de métier avancées — MatchFreelance (Python)

Document de référence des fonctionnalités métier avancées : **déjà en place** et **à développer**, avec les commandes et où voir les résultats.

---

## Vue d’ensemble

| Domaine | Statut | Où voir / Commande |
|--------|--------|--------------------|
| **Rappels & notifications** | ✅ En place | `python rappels.py` ou `--dry-run` |
| **ETL & Analytics** | ✅ En place | `python etl.py` puis `python analytics.py` → **Admin → Rapports** |
| **Scoring & Recommandations** | ✅ En place | `python recommendations.py` → **Admin → Recommandations** |
| Alertes & supervision | À faire | § 4 |
| Certificats & conformité | À faire | § 5 |
| Formations & planification | À faire | § 6 |
| Reporting avancé | À faire | § 7 |
| Matching Projets–Freelancers | À faire | § 9 |
| Badges & gamification | ✅ En place | `python badges.py` → **Profil** (niveau + badges) |
| Parcours de carrière & roadmap | À faire | § 11 |
| Marketplace de compétences | À faire | § 12 |
| Prérequis dynamiques | À faire | § 13 |
| Audit trail & traçabilité | À faire | § 14 |
| Prévisionnel & ML | ✅ En place | `python ml_predictions.py` → **Admin → Prévisionnel & ML** |
| RGPD & consentements | À faire | § 16 |
| Multi-tenant / organisations | À faire | § 17 |
| API publique & webhooks | À faire | § 18 |
| Disponibilité & planning | À faire | § 19 |
| Notations & avis | À faire | § 20 |

---

## 1. Rappels & notifications ✅

**Script :** `rappels.py`

**Déjà en place :**
- Rappels par email : formations ouvertes, examens à passer, certificats
- **Examen bien mis dans l’email** : libellé « Examen : {titre} » + lien direct vers la page passer l’examen
- **Formation se termine dans X jours** (config : `RAPPEL_FORMATION_JOURS_AVANT_FIN=7`)
- **Examen non passé après X jours** (config : `RAPPEL_EXAMEN_JOURS_RETARD=14`)
- Webhook Slack/Teams (`WEBHOOK_SLACK=true`)
- Envoi garanti à certaines adresses (`RAPPELS_ALWAYS_SEND_TO`)

**Commandes :**
```bash
cd python
python rappels.py --dry-run   # Voir les rappels sans envoyer
python rappels.py             # Envoyer les emails
```

**Configuration (.env) :** `SMTP_*`, `RAPPEL_FORMATION_JOURS_AVANT_FIN`, `RAPPEL_EXAMEN_JOURS_RETARD`, `RAPPELS_ALWAYS_SEND_TO`, `WEBHOOK_SLACK`, `WEBHOOK_URL`

---

## 2. ETL & Analytics ✅

**Scripts :** `etl.py` → `analytics.py`

**Déjà en place :**
- ETL : User, Formations, Inscriptions, Examens, Passages, Certificats, Skills → SQLite (`data/analytics.db`)
- Indicateurs par formation : taux d’inscription, taux de passage, taux de réussite
- Indicateurs par freelancer : nb certificats, formations suivies, score moyen
- Export CSV : `reports/indicateurs_formations.csv`, `reports/indicateurs_freelancers.csv`
- Graphiques PNG : inscriptions par formation, certificats par mois
- Copie automatique vers la plateforme → **Admin → Rapports**

**Commandes :**
```bash
cd python
python etl.py
python analytics.py
```

**Où voir :** Dossier `python/reports/` (CSV + PNG) et **http://localhost:4200/admin/rapports**

---

## 3. Scoring & Recommandations ✅

**Script :** `recommendations.py`

**Déjà en place :**
- Score de complétion 0–100 % par freelancer (certificats + formations + score examens)
- Recommandations de formations selon compétences et certificats
- Parcours avec prérequis en chaîne (formation → cert → formation…)
- Export JSON vers la plateforme → **Admin → Recommandations**

**Commande :**
```bash
cd python
python recommendations.py
```

**Où voir :** **http://localhost:4200/admin/recommandations**

---

## 4. Alertes & supervision (à faire)

- Script de santé : GET Gateway / Formation / Evaluation / User → alerte si down ou lent
- Quotas : alerte si formation surchargée ou taux d’échec élevé
- Détection d’anomalies (doublons, incohérences)

**Suggestion :** Nouveau script `sante_api.py` appelant les URLs et écrivant un rapport ou envoyant un email.

---

## 5. Certificats & conformité (à faire)

- Expiration des certificats + rappel 30 jours avant
- Renouvellement : rappel pour repasser l’examen
- Export PDF batch (zip des certificats d’un freelancer)

---

## 6. Formations & planification (à faire)

- Calendrier ICS/HTML des formations ouvertes
- Conflits de dates (même freelancer, deux formations qui se chevauchent)
- Liste d’attente quand formation complète + notification

---

## 7. Reporting & tableaux de bord (à faire)

- Rapport hebdo/mensuel (résumé texte ou HTML)
- KPI : conversion inscription → certificat, délai moyen
- Export pour Power BI / Metabase

---

## 8. Automatisation (à faire)

- Cron : `rappels.py` + `etl.py` + `analytics.py` (quotidien ou horaire)
- Intégration Google Calendar / Outlook
- Import CSV → API (formations, utilisateurs)

---

## Guide de test rapide

1. **Vérifier l’API :** Gateway (8050) + microservices démarrés
2. **ETL + Analytics :** `python etl.py` puis `python analytics.py` → ouvrir `reports/` ou Admin → Rapports
3. **Rappels (test) :** `python rappels.py --dry-run`
4. **Recommandations :** `python recommendations.py` → Admin → Recommandations

Voir aussi **`GUIDE_TEST_ETAPES.md`** pour le détail étape par étape.

---

## 9. Matching Projets–Freelancers (à faire)

- **Score de fit** : pour chaque projet ouvert, calculer un score d’adéquation par freelancer (compétences, certificats, disponibilité).
- **Top N recommandés** : afficher côté client les 5–10 freelancers les plus adaptés pour un projet.
- **Côté freelancer** : « Projets qui matchent avec mon profil » (tri par score).
- **Données** : skills, certificats, historique formations + mots-clés / compétences requises du projet.

---

## 10. Badges & gamification (à faire)

- **Badges** : « Première formation validée », « 5 certificats », « Expert [skill] », « Parcours 100 % complété ».
- **Niveaux** : Débutant → Intermédiaire → Avancé → Expert (selon score complétion + nb certificats).
- **Affichage** : badge sur le profil et dans les listes (côté client pour rassurer).
- **Notifications** : déblocage d’un badge → toast ou email.

---

## 11. Parcours de carrière & roadmap (à faire)

- **Roadmap par rôle** : ex. « Devenir Data Scientist » = chaîne formations (Python → ML → MLOps → …).
- **Progression visuelle** : barre ou étapes « 3/7 formations du parcours Data Scientist ».
- **Recommandation** : « Pour viser le rôle X, il vous manque la formation Y (prérequis : certificat Z). »
- **Données** : parcours prérequis déjà en place + métadonnées « rôle cible » sur les formations.

---

## 12. Marketplace de compétences (à faire)

- **Tags / taxonomie** : compétences normalisées (ex. « Java », « Spring Boot ») avec lien formations + projets.
- **Tendances** : compétences les plus demandées (projets ouverts) vs les plus détenues (freelancers).
- **Gaps** : « Ces compétences sont demandées mais peu de freelancers les ont » → idée de nouvelles formations.
- **Export** : CSV/JSON pour analyse RH ou achats de formations.

---

## 13. Prérequis dynamiques & parcours personnalisés (à faire)

- **Prérequis multiples** : formation exigeant « Certificat A **ou** Certificat B » (au lieu d’un seul examen requis).
- **Équivalences** : « Formation X compte comme équivalent à l’examen Y » pour éviter doublons.
- **Parcours personnalisé** : à partir du profil (skills + certificats), générer un plan d’étapes ordonné (déjà partiellement fait avec Parcours Intelligent ; étendre avec dates suggérées, priorités).

---

## 14. Audit trail & traçabilité (à faire)

- **Log des actions sensibles** : qui a validé quelle inscription, qui a créé/modifié une formation, passage d’examen (date, IP optionnel).
- **Export** : pour conformité ou litiges (ex. « Historique des validations d’inscriptions »).
- **Implémentation** : table `audit_log` (user_id, action, entity_type, entity_id, payload, created_at) + API ou script de lecture.

---

## 15. Prévisionnel & ML (à faire)

- **Prédiction de réussite** : probabilité de réussite à un examen selon profil (formations déjà suivies, scores passés).
- **Recommandation de projets** : modèle « freelancers similaires ont postulé / ont été recrutés sur ce type de projet ».
- **Détection de décrochage** : inscription validée mais pas de passage d’examen après X jours → score de risque + ciblage rappels.
- **Stack** : Python (pandas, scikit-learn) ou API externe ; résultats exposés en API ou dans un rapport admin.

---

## 16. Conformité RGPD & consentements (à faire)

- **Consentement** : stocker date/version du consentement (newsletter, rappels, traitement des données).
- **Droit à l’oubli** : anonymisation ou suppression des données personnelles + suppression des inscriptions/certificats liés.
- **Export des données** : « Télécharger mes données » (profil, inscriptions, certificats, passages) en JSON/PDF.
- **Durée de rétention** : politique claire (ex. logs 1 an, certificats tant que compte actif).

---

## 17. Multi-tenant / organisations (à faire)

- **Espaces par entreprise** : un client = une organisation avec ses projets, ses formations privées, ses freelancers invités.
- **Rôles** : Admin org, Manager projet, Freelancer interne.
- **Isolation** : les données (projets, formations) sont scopées par `organisation_id`.
- **Facturation** : abonnement par org ou par nombre d’utilisateurs actifs.

---

## 18. API publique & intégrations (à faire)

- **API publique documentée** (OpenAPI) : création de projets, liste formations, inscription, récupération résultats (avec auth).
- **Webhooks** : événements « inscription validée », « certificat délivré », « projet publié » → URL de callback configurée par le client.
- **SSO** : connexion via Google / Microsoft / OAuth2 pour réduire friction à l’inscription.
- **Intégration ATS** : export candidats/certificats vers un logiciel de recrutement.

---

## 19. Disponibilité & planning freelancer (à faire)

- **Disponibilités** : le freelancer indique ses créneaux (semaines/mois) ou « disponible à partir du ».
- **Conflits** : alerte si une formation ouverte chevauche une autre formation déjà suivie ou un projet en cours.
- **Proposition de créneaux** : pour une formation, afficher « X freelancers disponibles sur cette période ».

---

## 20. Notations & avis (à faire)

- **Client → Freelancer** : note et commentaire après un projet (étoiles + texte).
- **Freelancer → Formation** : satisfaction et « recommanderiez-vous ? » (NPS).
- **Agrégation** : note moyenne et nombre d’avis sur le profil freelancer ; taux de recommandation par formation.
- **Modération** : signalement, masquage ou suppression d’avis abusifs. A+wqw