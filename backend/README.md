# Backend — springBoot_pi

Backend (Gateway + microservices). Le frontend Angular est à la racine du projet : **`springBoot_pi/frontend`**.

## Prérequis : JDK 17

Les microservices utilisent **Spring Boot 4.x** et nécessitent **JDK 17**. Si `java -version` affiche Java 8, Maven échouera avec une erreur du type *"class file has wrong version 61.0, should be 52.0"*.

**Solution :** définir `JAVA_HOME` vers JDK 17 avant de lancer Maven.

- **Windows** : Paramètres → Variables d’environnement → ajouter ou modifier **JAVA_HOME** = `C:\Program Files\Java\jdk-17` (adapter si ton JDK 17 est ailleurs). Redémarrer le terminal / IntelliJ.
- **Depuis ce projet** : dans PowerShell, depuis `backend`, exécuter `.\run-with-jdk17.ps1` puis dans le même terminal lancer `mvnw.cmd` depuis le module (ex. `EurekaServer\EurekaServer`, `microservices\Formation`).
- **IntelliJ** : *File → Project Structure → Project SDK* = JDK 17 ; *Settings → Build Tools → Maven → JDK for importer* = JDK 17.

## Structure

```
springBoot_pi/
├── frontend/         # Application Angular (dev: port 4200)
└── backend/
    ├── EurekaServer/     # Discovery (port 8761)
    ├── Gateway/          # API Gateway (port 8082)
    └── microservices/
        ├── Evaluation/   # Examens, passages (port 8083)
        └── Formation/    # Formations, inscriptions (Eureka)
```

## Démarrer l’ensemble

1. **Eureka** : lancer `EurekaServer` (8761).
2. **Gateway** : lancer `Gateway` (8082).
3. **Microservices** : lancer `Evaluation` (8083) et `Formation`.
4. **Frontend** : dans `springBoot_pi/frontend`, exécuter `npm install` puis `npm start` (4200).

Le front appelle `/api/*` ; le proxy Angular redirige vers la Gateway (8082), qui route vers les microservices (examens → 8083, formations → FORMATION).

## Microservice Examen (Evaluation)

- **Contexte** : `ExamenController` sous `/api/examens`.
- **Base de données** : MySQL `EvaluationDB` (voir `microservices/Evaluation/src/main/resources/application.properties`).
