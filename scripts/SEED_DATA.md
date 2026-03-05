# Seed data and reset

The app fills the database with **presentable demo data** when tables are empty.

## What gets seeded

| Service            | When it runs        | Content |
|--------------------|---------------------|--------|
| **user-service**   | On first start only | 1 client (Marie Martin), 4 freelancers (Jean, Sophie, Lucas, Emma), 1 admin. Logins: `client@freelancehub.demo` / `client123`, `freelancer1@freelancehub.demo` / `freelancer123`, `admin@freelancehub.demo` / `admin123`. |
| **project-service**| When no projects    | 8 projects (SaaS Dashboard, FinTech API, Logistics, Healthcare, E-Learning, Real Estate, API Docs, Microservices). One IN_PROGRESS (E-Learning), one COMPLETED (Microservices), rest OPEN. All for client ID 1. |
| **candidature-service** | When no candidatures | 4 applications per project (from freelancers 2, 4, 5, 6), 32 total. Professional pitch messages and budgets. |
| **contract-service**   | When no contracts   | 2 contracts: E-Learning (project 5) ACTIVE with freelancer 2, Microservices (project 8) COMPLETED with freelancer 4. |

## How to reset and refill the database

1. **Stop** all four backend services (user, project, candidature, contract).

2. **Run the reset script** (MySQL with user `root`; set password if needed):
   ```bash
   mysql -u root -p < scripts/reset-databases.sql
   ```
   Or in MySQL Workbench: open `scripts/reset-databases.sql` and execute.

3. **Start services again** (Eureka and API Gateway can stay running):
   - user-service  
   - project-service  
   - candidature-service  
   - contract-service  

4. Seeds run automatically on startup when the corresponding table is empty. Then open **http://localhost:4200** and log in as client or freelancer to see the demo data.

## Demo logins

| Role      | Email                        | Password     |
|-----------|------------------------------|--------------|
| Client    | client@freelancehub.demo     | client123    |
| Freelancer| freelancer1@freelancehub.demo| freelancer123|
| Admin     | admin@freelancehub.demo      | admin123     |

(Freelancers 2–4: freelancer2@…, freelancer3@…, freelancer4@… with the same password.)
