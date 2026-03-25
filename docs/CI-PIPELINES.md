# Pipelines CI/CD (Formation + Evaluation)

## Nombre de pipelines GitHub Actions

1. **Formation CI** — `.github/workflows/formation-ci.yml`  
   - MySQL (service Docker) + **Eureka** + **Config Server** démarrés par `scripts/ci/start-stack.sh`  
   - `mvn verify` avec variables d’environnement : JDBC MySQL, Eureka, `spring.config.import` vers Config

2. **Evaluation CI** — `.github/workflows/evaluation-ci.yml`  
   - Même stack (MySQL + Eureka + Config)  
   - Base utilisée : `EvaluationDB`

3. **Quality SonarQube** — `.github/workflows/quality-sonarqube.yml`  
   - Déclenchement **manuel** (`workflow_dispatch`) pour éviter l’échec du workflow si les secrets Sonar ne sont pas configurés  
   - 2 jobs : scan **Formation** et **Evaluation** après `clean verify` (même infra que la CI)

## Infra utilisée dans la CI (comme en dev)

- **MySQL 8** : utilisateur `root` / mot de passe `root` (uniquement dans GitHub Actions)  
- **Eureka** : JAR `backend/EurekaServer/EurekaServer/target/eurekaServer-0.0.1-SNAPSHOT.jar` (port **8761**)  
- **Config Server** : JAR `backend/ConfigServer/target/config-server-0.0.1-SNAPSHOT.jar` (port **8888**)  
- Les tests `@SpringBootTest` voient donc une vraie stack (pas de H2 imposé par le dépôt).

## Secrets GitHub requis (SonarQube uniquement)

- `SONAR_HOST_URL` : URL (ex. SonarCloud `https://sonarcloud.io` ou ton serveur)  
- `SONAR_TOKEN` : token d’analyse (SonarCloud : **User** > **My Account** > **Security**)

Pour **SonarCloud**, ajoute aussi les paramètres Maven du projet (`sonar.organization`, etc.) dans le workflow ou via `sonar-project.properties` si besoin.

## Tests locaux (avec MySQL + Eureka + Config sur ta machine)

Démarre MySQL, Eureka (8761), Config (8888), puis par exemple :

```bash
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/FormationDB?createDatabaseIfNotExist=true^&useSSL=false^&serverTimezone=UTC^&allowPublicKeyRetrieval=true
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=
set EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka
set SPRING_CONFIG_IMPORT=optional:configserver:http://localhost:8888
.\backend\microservices\Formation\mvnw.cmd -f backend\microservices\Formation\pom.xml clean test
```

Rapports JaCoCo : `target/site/jacoco/index.html` et `target/site/jacoco/jacoco.xml`

## Qualité SonarQube (métriques)

Après scan : couverture, duplications, issues, fiabilité, sécurité, maintenabilité, dette technique (selon le Quality Gate).
