# Spring Cloud Config Server

Serveur de configuration centralisée pour les microservices (architecture type *MicroServices - Spring Cloud Config*).

## Rôle

- Récupère la configuration depuis un **File Store** (dossier `config/`) ou **Git**.
- Expose les propriétés aux microservices (Config Clients) via HTTP sur le port **8888**.

## Démarrer le Config Server

```bash
cd ConfigServer
mvn spring-boot:run
```

Le serveur écoute sur **http://localhost:8888**.

## Ordre de démarrage recommandé

1. **Eureka** (8761)
2. **Config Server** (8888)
3. **ApiGateway** (8086)
4. **Microservices** (User, Project, Skill, Formation)

Les clients chargent leur config au démarrage via `spring.config.import=optional:configserver:http://localhost:8888`. Si le Config Server est indisponible, ils utilisent leur `application.properties` local (`optional:`).

## File Store (profil `native`)

Les fichiers dans `src/main/resources/config/` sont servis par le Config Server :

- `application.properties` : config partagée (ex. Eureka).
- `USER.properties`, `PROJECT.properties`, `SKILL.properties`, `FORMATION.properties`, `ApiGateway.properties` : config par application (nom = `spring.application.name` du client).

## Utiliser Git

Dans `application.properties` du Config Server :

1. Désactiver le profil `native` et activer `git`.
2. Définir `spring.cloud.config.server.git.uri` (URL du dépôt Git).
3. Mettre les mêmes fichiers (application.properties, USER.properties, etc.) dans le dépôt Git.

Exemple :

```properties
spring.profiles.active=git
spring.cloud.config.server.git.uri=https://github.com/votre-org/config-repo
spring.cloud.config.server.git.default-label=main
```

## Vérifier la config servie

- Tous les services : http://localhost:8888/application/default
- Service USER : http://localhost:8888/USER/default
- Service PROJECT : http://localhost:8888/PROJECT/default
