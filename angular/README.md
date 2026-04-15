# FreelanceHub — Angular Frontend

Angular frontend for the Freelancing Platform Back Office.

## Prerequisites

- Node.js 18+
- npm

## Setup

```bash
cd angular
npm install
```

## Run

**1. Start the Spring Boot backend** (from project root):

```bash
./mvnw spring-boot:run
```

Backend runs on **http://localhost:8081**

**2. Start the Angular dev server:**

```bash
npm start
```

Frontend runs on **http://localhost:4200**

API requests to `/api/*` are proxied to the backend automatically.

## Features

- **Dashboard** — Overview stats (total projects, open, in progress, total budget) and recent projects
- **Projects** — List with search, status filter, view, edit, delete
- **Create/Edit Project** — Form with validation (title, description, budget, duration, status)
- **Project Details** — Full project view with edit/delete actions
- **Toast Notifications** — Success and error feedback for all actions
- **Responsive Layout** — Sidebar collapses on mobile with overlay

## Tech Stack

- Angular 21
- Tailwind CSS 4
- Standalone components
- RxJS
