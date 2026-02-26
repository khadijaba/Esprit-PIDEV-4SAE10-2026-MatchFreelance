# Microservice User — Plateforme Freelancers & Project Matching

Authentification et gestion des utilisateurs (Admin, Freelancer, Client).

## Démarrer

- Port : **8085**
- Eureka : s'enregistre sur `http://localhost:8761/eureka/`
- Base : MySQL, base `user` (création auto si absente)

## API (via Gateway : `http://localhost:8050/api/users`)

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/users/auth/register` | Inscription (body : email, password, fullName, role) |
| POST | `/api/users/auth/login` | Connexion (body : email, password) → token JWT |
| GET | `/api/users/me` | Profil courant (header `Authorization: Bearer <token>`) |
| GET | `/api/users` | Liste des utilisateurs (optionnel : `?role=FREELANCER`) |

## Rôles

- `ADMIN`
- `FREELANCER`
- `CLIENT`
