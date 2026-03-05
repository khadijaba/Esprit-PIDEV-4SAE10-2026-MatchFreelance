# Compatibilité : Top 5 Freelancers (Project Owner) et Skill Gap (Freelancer)

## 1. Top 5 Freelancers compatibles (côté Project Owner / Admin)

**Où c’est affiché**
- **Admin** : `/admin/projects/:id` (détail d’un projet) → bloc « Top 5 Freelancers compatibles ».
- **Project Owner** : depuis « Mes projets », clic sur un projet → `/projects/:id` (même page que la recherche publique) → même bloc Top 5.
- **Public** : `/projects/:id` (Find Projects → détail) → même bloc.

**Fonctionnement**
- Le projet doit avoir des **compétences requises** (saisies à la création/édition).
- Les compétences sont **normalisées** côté front (tableau, chaîne JSON ou « a, b, c » venant du backend).
- Le service appelle **Skills** (toutes les compétences) et **Users** (rôle FREELANCER), calcule un score par freelancer puis affiche les 5 premiers.

**Si rien ne s’affiche**
- Vérifier que le projet a bien des **compétences requises** (éditer le projet et en ajouter).
- Vérifier qu’il existe des utilisateurs **Freelancer** avec des **compétences** enregistrées (Espace Freelancer → Skills).
- Côté backend : `GET /api/skills` et `GET /api/users?role=FREELANCER` doivent renvoyer des données ; dans les Skills, `freelancerId` doit correspondre à l’`id` User du freelancer.

---

## 2. Compatibilité freelancer (Skill Gap)

**Où c’est affiché**
- **Freelancer connecté** uniquement, sur la page détail d’un projet : `/projects/:id`.
- Bloc « Votre compatibilité avec ce projet » avec **%** et **compétences manquantes**.

**Fonctionnement**
- Le projet doit avoir des **compétences requises**.
- On charge les **compétences du freelancer** via `GET /api/skills/freelancer/:userId` (userId = utilisateur connecté).
- On compare aux compétences requises (noms et catégories, normalisés) et on calcule le % + liste des manquantes.

**Si toujours 0 % ou rien**
- Vérifier que le **projet** a des compétences requises (affichées sur la page).
- Vérifier que le **freelancer** a des compétences dans son profil (Espace Freelancer).
- Côté backend : `GET /api/skills/freelancer/:id` doit utiliser le même **id** que l’utilisateur connecté (User.id = freelancerId des Skills).
- Les libellés (noms de compétences ou catégories) doivent pouvoir matcher : ex. « Java », « Web Development », `WEB_DEVELOPMENT` après normalisation.

---

## Fichiers modifiés / ajoutés

| Fichier | Rôle |
|--------|------|
| `utils/project-skill.util.ts` | Normalisation de `requiredSkills` (string[] / string / JSON) partagée. |
| `services/talent-matching.service.ts` | Utilise la normalisation ; `getTopMatchingByProjectId` normalise avant calcul. |
| `services/skill-gap.service.ts` | Utilise `normalizeRequiredSkills` du util. |
| `components/project-details/*` | Normalise le projet au chargement ; messages d’aide si Top 5 vide. |
| `components/front-project-detail/*` | Normalise le projet ; lien « Retour à mes projets » pour Project Owner ; messages Skill Gap. |
| `components/project-list/*` | Lien détail Project Owner → `/projects/:id` (page avec Top 5). |

---

## Checklist backend (pour que tout marche)

- [ ] **Projets** : création/édition envoie et le détail renvoie bien `requiredSkills` (tableau ou chaîne).
- [ ] **Skills** : `GET /api/skills` et `GET /api/skills/freelancer/:id` ; `freelancerId` = id de l’utilisateur Freelancer (User.id).
- [ ] **Users** : `GET /api/users?role=FREELANCER` renvoie les utilisateurs avec le rôle Freelancer.
- [ ] **Skills** : chaque skill a `name` (ou `skillName`) et `category` (string ou objet avec `name`) pour le matching.
