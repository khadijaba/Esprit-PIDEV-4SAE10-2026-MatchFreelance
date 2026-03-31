# Workflow Git – Équipe 5 personnes

## Dépôt partagé (recommandé)

- **Un seul repo** : Eureka + ApiGateway + tous les microservices (Skill, Project, User, …).
- Tout le monde clone le même repo ; chaque membre travaille dans son microservice.

## Branches

- **main** ou **develop** : branche d’intégration (tous les microservices + Eureka + Gateway).
- **feature/nom-service-description** : ex. `feature/user-auth`, `feature/skill-categories`.
- Pas de branche par microservice long‑terme : on merge dans `develop` après revue.

## Règles

1. **Eureka et ApiGateway** : modifications partagées (changer l’URL Eureka, ajouter une route). Toujours faire une **pull request** et faire valider par un autre avant merge.
2. **Son microservice** : chaque membre commit et pousse ses changements ; PR vers `develop` pour l’intégration.
3. **Avant de merger** : vérifier en local que le service démarre et apparaît dans Eureka (`http://<EUREKA_HOST>:8761`).
4. **Pas de conflit sur les ports** : chaque microservice a un port fixe (voir `MICROSERVICES-REGISTRY.md`).

## Intégration Eureka (toute l’équipe)

- Décider qui héberge Eureka (une machine ou un serveur).
- Mettre la même **EUREKA_URL** partout (dans les `application.properties` ou en variable d’environnement).
- Documenter l’URL dans le repo (ex. dans `ARCHITECTURE-EQUIPE.md` ou un fichier `config-equipe.properties.example`).

## Exemple de cycle

1. `git pull origin develop`
2. Créer `feature/user-login`
3. Développer dans `Microservices/User/`
4. Tester avec Eureka + Gateway (même `EUREKA_URL`)
5. Commit, push, ouvrir une PR vers `develop`
6. Après merge : les autres font `git pull` et ont le nouveau module User
