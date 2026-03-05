# Grille d'évaluation – SAE Sprint 1 (Spring – Angular)

Référence : [SAE-Sprint1-Spring-Angular] Grille d'évaluation-2526-V2

---

## AA1 – Logique Métier & Analyse

**Critère [C1] :** Consistance des tâches : mise en place d'un microservice (CRUD complet, fonctions avancées) et intégration côté front-end avec Angular.

| Niveau | Barème | Description |
|--------|--------|-------------|
| **A** | 4–3 | Toutes les tâches sont maîtrisées. |
| **B** | 2 | La majorité des tâches sont des (CRUD simple) et maîtrise globale. |
| **C** | 1 | CRUD uniquement, sans maîtrise réelle du code. |
| **D** | 0 | Échec de l'implémentation Back ou Front. |

| Niveau | Barème | Description – Fonctionnalités avancées |
|--------|--------|----------------------------------------|
| **A** | 4–3 | Toutes les fonctionnalités avancées sont maîtrisées et fonctionnelles. |
| **B** | 2 | La majorité des fonctionnalités sont présentes avec une maîtrise globale de la logique. |
| **C** | 1 | Fonctionnalités partiellement implémentées, sans maîtrise réelle de la logique métier. |
| **D** | 0 | Échec de l'implémentation des fonctionnalités avancées (Back ou Front). |

| | A | B | C | D | Note attribuée | Remarques |
|---|---|---|---|---|----------------|-----------|
| **AA1 [C1]** | | | | | | |

**Preuves projet (Interview microservice) :** CRUD interviews + availability, lifecycle (confirm/reject/cancel/complete/no-show), intégration Angular (interview-list, interview-detail, interview-schedule, availability). Fonctionnalités avancées : suggestions de créneaux, indice de fiabilité, workload, top N freelancers, politique d’annulation, fenêtre d’accès visio. Voir docs/INTERVIEW_CRUD_AND_ADVANCED_FUNCTIONS.txt.

---

## AA2 – Architecture Logicielle

**Critère [C2] :** Infrastructure Microservices : configuration et enregistrement correct dans Eureka Server et API Gateway.

| Niveau | Barème | Description |
|--------|--------|-------------|
| **A** | 4 | Architecture fluide, routage Gateway parfait et choix justifiés. |
| **B** | 3 | Infrastructure fonctionnelle mais modularité perfectible. |
| **C** | 2–1 | Microservice fonctionnel mais mal intégré à la Gateway. |
| **D** | 0 | Absence de Gateway ou d'Eureka. |

| | A | B | C | D | Note attribuée | Remarques |
|---|---|---|---|---|----------------|-----------|
| **AA2 [C2]** | | | | | | |

**Preuves projet :** eureka-server (8761), api-gateway (8081), interview-service (8085) et autres microservices enregistrés ; routes /api/interviews/** et /api/availability/** vers interview-service. Voir README et docs/RUN_PLATFORM_FOR_INTERVIEW_TEST.md.

---

## AA3 – Synthèse & Présentation

**Critère [C3] :** Scénarios de tests & Oral : expliquer le lien Angular–API ; scénario cohérent.

| Niveau | Barème | Description |
|--------|--------|-------------|
| **A** | 4 | Présentation claire, convaincante et réponses pertinentes. |
| **B** | 3 | Communication efficace mais manque de précision technique. |
| **C** | 2–1 | Communication peu structurée ou réponses partielles. |
| **D** | 0 | Incapable de présenter ou d'expliquer son travail. |

| | A | B | C | D | Note attribuée | Remarques |
|---|---|---|---|---|----------------|-----------|
| **AA3 [C3]** | | | | | | |

**Aide :** Lien Angular–API : proxy /api → Gateway 8081 ; interview.service.ts appelle /api/interviews, /api/availability, /api/reviews. Scénario démo : docs/INTERVIEW_MICROSERVICE_FEATURES_AND_TESTING.md et fin de docs/INTERVIEW_CRUD_AND_ADVANCED_FUNCTIONS.txt.

---

## AA4 – Pratiques Pro & Standardisation

**Critère [C4] :** Gestion & GitHub : utilisation de Git ; respect du naming, topics et README Esprit.

| Niveau | Barème | Description |
|--------|--------|-------------|
| **A** | 2 | Workflow Git régulier et standardisation GitHub Esprit parfaite et respectée. |
| **B** | 1.5 | Code intégré mais standardisation GitHub incomplète. |
| **C** | 1 | Intégration manuelle et/ou non-respect du naming conventionnel. |
| **D** | 0 | Ni gestion de projet, ni respect des standards Esprit. |

| | A | B | C | D | Note attribuée | Remarques |
|---|---|---|---|---|----------------|-----------|
| **AA4 [C4]** | | | | | | |

**Rappel standardisation Esprit (dépôt public) :**
- Nom : Esprit-[PI]-[Classe]-[AU]-[NomDuProjet] (ex. Esprit-PIDEV-4SAE10-2026-MatchFreelance).
- Description : inclure "Developed at Esprit School of Engineering – Tunisia", année universitaire, technologies principales.
- Topics minimum : esprit-school-of-engineering, academic-project, esprit-pidev, année (ex. 2025-2026), technologie principale (ex. angular, spring-boot).
- README : structure avec Overview, Features, Tech Stack, Architecture, Contributors, Academic Context, Getting Started, Acknowledgments.

---

## AA5 – Excellence

**Critère [C5] :** Innovation : fonctionnalités originales ou technologies hors cursus.

| Niveau | Barème | Description |
|--------|--------|-------------|
| **A** | 2 | Innovation réelle ou complexité technique élevée. |
| **B** | 1 | Effort d'optimisation ou qualité de code supérieure. |
| **C** | 0.5 | Quelques tentatives d'amélioration. |
| **D** | 0 | Travail strictement minimaliste. |

| | A | B | C | D | Note attribuée | Remarques |
|---|---|---|---|---|----------------|-----------|
| **AA5 [C5]** | | | | | | |

**Exemples projet :** Algorithmes avancés (suggestions de créneaux scorées, fiabilité avec lissage, workload, top N combiné fiabilité+reviews), visio in-app (Jitsi), export iCal, notifications et rappels automatiques.

---

## Récapitulatif des notes

| Acquis d'apprentissage | Critère | Note max (indicative) | Note attribuée |
|------------------------|---------|------------------------|----------------|
| AA1 – Logique Métier & Analyse | [C1] | 8 (4+4) | |
| AA2 – Architecture Logicielle | [C2] | 4 | |
| AA3 – Synthèse & Présentation | [C3] | 4 | |
| AA4 – Pratiques Pro & Standardisation | [C4] | 2 | |
| AA5 – Excellence | [C5] | 2 | |
| **Total** | | **20** | |

---

## Évaluation académique GitHub (Esprit)

| Critère | Points | Conformité projet |
|---------|--------|-------------------|
| Convention de nommage | 0,5 | Nom dépôt type Esprit-PIDEV-4SAE10-2026-MatchFreelance |
| Description du projet | 0,5 | "Developed at Esprit School of Engineering – Tunisia", AU, technologies |
| Utilisation des "topics" | 0,5 | esprit-school-of-engineering, academic-project, esprit-pidev, année, technologies |
| Mots-clés dans le README | 0,25 ou 0,5 | "Esprit School of Engineering" présent |
| Hébergement (facultatif) | 0,25 | Si déploiement (Vercel, Render, etc.) |
