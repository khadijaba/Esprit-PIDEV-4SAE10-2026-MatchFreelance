# Microservice Skill — Parcours Intelligent

Parcours personnalisé, analyse des compétences actuelles, détection des gaps et proposition de formations ciblées (microservice Formation).

## Port : 8086

## Fonctionnalités

- **Skills** : CRUD compétences par freelancer (nom, catégorie, niveau).
- **Parcours Intelligent** : `GET /api/skills/parcours/intelligent?freelancerId=1`
  - Compétences actuelles du freelancer
  - Gaps (domaines sans compétence : WEB_DEVELOPMENT, MOBILE_DEVELOPMENT, DATA_SCIENCE, DEVOPS, etc.)
  - Formations proposées (ouvertes) du microservice Formation ciblant les gaps

## Dépendance

Appelle le microservice **FORMATION** (Eureka) pour récupérer les formations ouvertes.

## Base de données

MySQL, base `skill` (création auto).
