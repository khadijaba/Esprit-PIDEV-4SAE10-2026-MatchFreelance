# Évaluation des compétences — Comment ça travaille

Dans le projet, l’**évaluation des compétences** sert à :
- **Matching** : comparer les compétences requises d’un projet aux compétences des freelancers (Top 5, notifications).
- **Skill-gap** : mesurer la compatibilité d’un freelancer avec un projet (ce qu’il a vs ce qui manque).
- **Évolution** : score de profil et score par compétence (heatmap, répartition par niveau).

Voici **comment** chaque partie travaille.

---

## 1. Niveaux (Junior → Expert)

**Fichier :** `models/skill-evolution.model.ts`

Chaque compétence a un **niveau** stocké côté backend : `BEGINNER`, `JUNIOR`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`.

- **Ordre numérique** (pour les calculs) :  
  `BEGINNER/JUNIOR = 1`, `INTERMEDIATE = 2`, `ADVANCED = 3`, `EXPERT = 4`.
- **Affichage** : Junior, Intermediate, Advanced, Expert (via `getLevelLabel`).

Ces niveaux sont utilisés pour :
- le **score par compétence** (évolution) ;
- le **score de profil** (moyenne des niveaux + bonus expérience) ;
- l’**expérience** dans le matching (voir plus bas).

---

## 2. Matching projet ↔ freelancer (Top 5)

**Fichier :** `services/talent-matching.service.ts`

Pour chaque projet, on calcule un **score de compatibilité** par freelancer, puis on garde le **Top 5**.

### Formule du score

```
Score = (Compétences × 0,7) + (Expérience × 0,3)
```

- **Compétences (0–100 %)**  
  - On récupère les compétences **requises** du projet (normalisées).  
  - Pour chaque freelancer : on compare ces compétences aux **noms** et **catégories** de ses skills (normalisés : minuscules, `_` → espace).  
  - Une compétence requise « matche » si :  
    - égalité exacte, ou  
    - sous-chaîne (ex. « spring » dans « Spring Boot »), ou  
    - match par **mots** (ex. « Node.js » → mots « node », « js »).  
  - **skillMatch** = (nombre de compétences requises qui matchent / total) × 100.

- **Expérience (0–100 %)**  
  - Moyenne des **années d’expérience** (`yearsOfExperience`) sur toutes les compétences du freelancer.  
  - Cette moyenne est ramenée à 0–100 en plafonnant à **10 ans** = 100 %.

### Étapes concrètes

1. Normaliser le projet → `requiredSkills` (tableau de chaînes).
2. Charger toutes les skills et tous les users FREELANCER.
3. Grouper les skills par `freelancerId`.
4. Pour chaque freelancer :  
   - `computeSkillMatch(required, skills)` → % de compétences requises couvertes.  
   - `computeExperience(skills)` → % expérience (moyenne années, plafond 10 ans).  
   - Score final = 0,7 × skillMatch + 0,3 × experience, borné 0–100.
5. Trier par score décroissant, garder les 5 premiers.

**Utilisation :** fiche projet (détail), liste Top 5 sur une fiche projet.

---

## 3. Comparaison requise ↔ compétence (skillMatchesRequired)

**Fichier :** `utils/project-skill.util.ts`

Une compétence **requise** (une chaîne, ex. « Java », « Spring Boot ») est considérée comme **matchée** par le freelancer si au moins une de ses compétences (nom ou catégorie, normalisés) vérifie :

- **Match exact** : « java » === « java ».
- **Sous-chaîne** : « spring » contenu dans « spring boot » (ou l’inverse), avec longueur ≥ 3.
- **Match par mots** : on découpe la requise en mots (longueur ≥ 2) ; si un mot matche une compétence (exact ou sous-chaîne), c’est bon (ex. « Node.js » → « node », « js »).

Tout est comparé en **minuscules**, avec `_` remplacé par un espace. C’est la même logique côté **Python** (service Team AI) pour les notifications compatibles.

---

## 4. Skill-gap (compatibilité freelancer ↔ projet)

**Fichier :** `services/skill-gap.service.ts`

On ne calcule **pas** un score pondéré ici : on regarde **qui est couvert / qui manque**.

- **Entrées :** un **projet** (requiredSkills normalisées) et les **compétences du freelancer** (profil).
- Pour chaque compétence **requise** :  
  - si `skillMatchesRequired(norm, freelancerSet)` → ajout dans **matchedSkills** ;  
  - sinon → ajout dans **missingSkills**.
- **Compatibilité** = (nombre de matched / total requis) × 100.

**Utilisation :** afficher « Vous avez X % de compatibilité », « Compétences couvertes : … », « Compétences manquantes : … » (par ex. sur une fiche projet ou dans l’espace freelancer).

---

## 5. Évolution des compétences (score profil + score par skill)

**Fichier :** `components/skill-evolution-dashboard/skill-evolution-dashboard.component.ts`

### Score de profil (0–100)

- **Niveaux :** moyenne des ordres (1–4) des compétences, transformée en 0–100 :  
  `levelScore = ((moyenne - 1) / 3) * 100`.
- **Bonus expérience :** moyenne des années d’expérience par compétence, plafonnée (ex. +30 max), ajoutée au score.
- **overallScore** = levelScore + expBonus, plafonné à 100.  
  Affiché dans la **jauge circulaire** (Évolution des compétences).

### Score par compétence (0–100) — heatmap

Pour **chaque** compétence :

- **Niveau** → part en % : `(order - 1) / 3 * 100` (Junior ≈ 0, Expert = 100).
- **Années d’expérience** → bonus plafonné (ex. 10 ans = +15).
- **skillScore(skill)** = niveau% + bonus, plafonné à 100.  
  Utilisé pour la **barre de progression** et l’affichage « X/100 » dans la heatmap.

### Répartition par niveau

On compte combien de compétences sont en Junior, Intermediate, Advanced, Expert, et on affiche un **diagramme en barres** (répartition par niveau). BEGINNER est compté comme Junior.

---

## 6. Côté Python (notifications compatibles)

**Fichier (backend) :** `team-ai/app/services/compatibility_notifier.py`

Même idée que le matching frontend :

- **required_skills** : normalisées (découpage par virgule si une chaîne est envoyée).
- Pour chaque freelancer :  
  - **skill_match** : % de compétences requises qui matchent (noms + catégories), avec la même logique exact / sous-chaîne / mots.  
  - **experience** : moyenne des années, plafonnée (ex. 10 ans = 100 %).  
  - **score** = 0,7 × skill_match + 0,3 × experience, 0–100.
- On ne garde que les freelancers avec **score ≥ 70** pour envoyer les notifications.

---

## Résumé : qui fait quoi

| Besoin | Où | Comment |
|--------|-----|--------|
| Top 5 freelancers pour un projet | `TalentMatchingService` | Score = 70 % compétences (match noms/catégories) + 30 % expérience (moyenne années, max 10 ans). |
| « J’ai X % de compatibilité avec ce projet » | `SkillGapService` | Pour chaque compétence requise : match ou pas → matchedSkills / missingSkills ; compatibilité = % requis couverts. |
| Score de profil (jauge) | `SkillEvolutionDashboardComponent` | Moyenne des niveaux (1–4) → 0–100 + bonus expérience. |
| Score par compétence (heatmap) | `SkillEvolutionDashboardComponent.skillScore()` | Niveau → % + bonus années, plafonné 100. |
| Notifications « freelancers compatibles » | Python `compatibility_notifier` | Même formule 70 % / 30 %, seuil 70 % ; required_skills normalisées (split virgule). |

En résumé : **l’évaluation des compétences** repose sur les **niveaux** (Junior→Expert), les **années d’expérience**, et la **comparaison de chaînes normalisées** (noms + catégories) pour le matching et le skill-gap. Les poids 70 % / 30 % sont alignés entre le frontend (Top 5) et le service Python (notifications).
