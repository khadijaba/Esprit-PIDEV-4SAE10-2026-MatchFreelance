# Intégration : `Esprit-PIDEV-4SAE10-2026-MatchFreelance-projectskills`

Ce document organise la fusion du projet PIDEV (captures : création projet, scoring risque ONNX, **Top 5 freelancers** avec formule `0,7 × Compétences + 0,3 × Expérience`, supervision) avec le dépôt **validation** (`MatchFreelance` actuel).

## 1. Contenu du dossier PIDEV (vue d’ensemble)

| Dossier | Rôle |
|--------|------|
| `BackEnd/ApiGateway` | Gateway Spring Cloud (README PIDEV : **8086**) |
| `BackEnd/Eureka` | Annuaire (**8761**) |
| `BackEnd/ConfigServer` | Config centralisée (**8888**) |
| `BackEnd/Microservices/Skill` | Compétences, CV, portfolio (**8083** dans README PIDEV — **conflit avec Evaluation dans validation**) |
| `BackEnd/Microservices/Project` | Projets, compétences requises (**8084**) |
| `BackEnd/Microservices/Candidature`, `Contract`, `Interview` | Flux métier avancé (candidatures, contrats, entretiens) |
| `BackEnd/PythonAdvanced` | ML / ONNX (charge, risque — comme sur tes captures) |
| `BackEnd/TeamAi` | Doublon possible avec `team-ai` racine |
| `frontend/` | Angular **21** (projets, skills, project owner, wizard 2 étapes) |
| `team-ai/` | FastAPI (**5000**) : `POST /api/analyze-project`, `POST /api/build-team` |

**Attention :** dans `Skill` et `Project`, certains `application.properties` peuvent contenir des **marqueurs de conflit Git** (`<<<<<<<`). Les nettoyer avant `mvn spring-boot:run`.

## 2. État du dépôt `validation` (rappel)

- **Evaluation** occupe **`8083`** → ne pas y superposer Skill PIDEV sans changer de port.
- Les microservices **Skill** et **Project** avaient été **retirés** de `validation` (Gateway sans `/api/skills` ni `/api/projects`, frontend sans ces écrans).
- Gateway **validation** : **8050** ; Formation **8081**, etc.

## 3. Stratégies d’intégration (choisir une)

### Option A — **Deux dépôts côte à côte** (rapide pour démo)

1. Lancer PIDEV **tel quel** (Eureka 8761, Gateway PIDEV **8086**, Skill, Project, MySQL `skill` / `project`).
2. Lancer **validation** pour User / Formation / Evaluation (Gateway **8050**).
3. Côté front : soit deux apps (4200 PIDEV + 4200 validation en alternance), soit un **proxy** qui agrège les deux gateways (complexe).

Utile pour **ne pas toucher** au code `validation` tout de suite.

### Option B — **Réintégrer Skill + Project dans `validation`** (recommandé pour un seul produit)

1. **Copier** depuis PIDEV vers `validation/backend/microservices/` :
   - `Skill/` (complet)
   - `Project/` (complet)
   - Optionnel plus tard : `Candidature`, `Contract`, `Interview` si tu veux la supervision complète.
2. **Ports (exemple sans conflit)** :
   - Garder **Evaluation =8083**.
   - Passer **Skill →8087** (ou 8091), **Project → 8084** (inchangé si libre).
   - Vérifier `spring.application.name` Eureka : `SKILL`, `PROJECT` (comme attendu par `lb://` dans la Gateway).
3. **Gateway `validation`** (`GatewayApplication.java` + `application.properties`) :
   - Réactiver les routes `/api/skills/**` et `/api/projects/**` (réécriture `/api/projects` → `/projects` si le contrôleur Project est sous `/projects`).
4. **MySQL** : créer les bases attendues (`skill`, `project` ou noms dans les `application.properties` copiés).
5. **Evaluation** : si tu avais retiré `SkillClient` / `ProjectClient`, les **réintroduire** ou réaligner les DTO avec les nouveaux services (matching global, certificat, ranking).
6. **Frontend `validation`** :
   - Reprendre les écrans depuis `PIDEV/frontend/src` : wizard projet, détail projet, top freelancers, etc.
   - Adapter **Angular** (version / standalone) et `proxy.conf` vers **8050** ou le port Evaluation direct.
7. **team-ai** :
   - Copier `team-ai/` dans `validation/team-ai` (ou `python/team-ai`).
   - Ajouter une route Gateway vers `http://localhost:5000` pour ` /api/team-ai/**` ou préfixe choisi.
   - Variables : `TEAM_AI_BACKEND_URL` pointant vers ta Gateway (**8050** ou **8086** selon stack).

### Option C — **Remplacer le backend `validation` par celui de PIDEV**

Tu abandonnes la structure actuelle des microservices `validation` et tu travailles uniquement dans le dossier PIDEV, en y important manuellement les fichiers spécifiques déjà faits dans `validation` (Evaluation avancé, rapports Python, etc.). À utiliser seulement si PIDEV est devenu la **référence** officielle.

## 4. Ordre de démarrage typique (après fusion Option B)

1. MySQL  
2. Eureka  
3. Config Server (si utilisé)  
4. **User**, **Formation**, **Skill** (nouveau port), **Project**, **Evaluation**  
5. Gateway **validation** (8050)  
6. **team-ai** (5000)  
7. **PythonAdvanced** / ONNX si besoin des écrans charge & risque  
8. `ng serve` frontend

## 5. Alignement avec tes captures UI

| Capture | Source probable dans PIDEV |
|--------|----------------------------|
| Wizard projet + charge + risque ONNX | `frontend` + `BackEnd/PythonAdvanced` (ou service dédié dans Gateway) |
| Top 5 freelancers `0,7 comp + 0,3 exp` | `Project` ou `User` + `Skill` (endpoint de matching) + composant détail projet |
| Supervision phases + copilot | Microservice **Project** étendu ou service à part — à identifier dans `Project` / `Interview` |

Recherche utile dans PIDEV : `grep -r "0.7"`, `grep -r "Top 5"`, `grep -r "compatibles"` dans `frontend` et `Microservices/Project`.

## 6. Check-list rapide

- [ ] Résoudre conflits Git dans `application.properties` (Skill / Project PIDEV)  
- [ ] Table de ports documentée (aucun doublon avec Evaluation **8083**)  
- [ ] Eureka : tous les services enregistrés  
- [ ] Gateway : une seule URL front (proxy **8050** ou **8086**)  
- [ ] CORS si front4200 et API autre host  
- [ ] Tests : création projet → matching freelancers → une candidature

## 7. Emplacement du dossier source

Chemin local :  
`C:\Users\benay\Downloads\Esprit-PIDEV-4SAE10-2026-MatchFreelance-projectskills`

Ne pas déplacer aveuglément : préférer **copie** vers `validation` ou **workspace multi-root** dans Cursor jusqu’à stabilisation des ports et des bases.

---

*Document généré pour structurer l’intégration ; à ajuster selon les ports et noms de BDD réels de ta machine.*
