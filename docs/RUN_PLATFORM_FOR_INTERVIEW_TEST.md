# Run the platform to test the Interview microservice

To test **projects** (postuler, create interviews) and **interview** features through the Angular UI, start the following **in this order** (each in its own terminal).

## 1. Eureka (required first)
```powershell
cd "d:\study\4SAE\Semestre 2\PIDEV\spring-task-planning\eureka-server"
mvn -q -DskipTests spring-boot:run
```
Wait until you see `Started EurekaServerApplication`. Port: **8761**.

## 2. API Gateway
```powershell
cd "d:\study\4SAE\Semestre 2\PIDEV\spring-task-planning\api-gateway"
mvn -q -DskipTests spring-boot:run
```
Wait until you see `Started ApiGatewayApplication`. Port: **8081**.  
Angular proxies `/api` to this port.

## 3. Project service (required for projects list, postuler, and creating interviews)
```powershell
cd "d:\study\4SAE\Semestre 2\PIDEV\spring-task-planning\project-service"
mvn -q -DskipTests spring-boot:run
```
Wait until you see `Started ProjectServiceApplication`. Port: **8082**.  
**Without this, you get "Failed to load projects"** and cannot browse projects or create interviews from candidatures.

## 4. Candidature service (required for "Postuler" and scheduling interviews from a project)
```powershell
cd "d:\study\4SAE\Semestre 2\PIDEV\spring-task-planning\candidature-service"
mvn -q -DskipTests spring-boot:run
```
Wait until you see the candidature app started. Port: **8083**.  
Needed so freelancers can apply (postuler) and clients can see candidatures and schedule interviews.

## 5. Interview service
```powershell
cd "d:\study\4SAE\Semestre 2\PIDEV\spring-task-planning\interview-service"
mvn -q -DskipTests spring-boot:run
```
Wait until you see `Started InterviewServiceApplication`. Port: **8085**.

## 6. Angular (frontend)
```powershell
cd "d:\study\4SAE\Semestre 2\PIDEV\spring-task-planning\angular"
npx ng serve --open
```
Opens **http://localhost:4200** in the browser.  
API calls from the app go to `http://localhost:4200/api/...` and are proxied to the gateway at 8081.

---

## Where to test in the UI

| Area | URL | What you can do |
|------|-----|------------------|
| **Projects (admin)** | http://localhost:4200/admin/projects | List/create/edit projects (needs project-service) |
| **Projects (public – postuler)** | http://localhost:4200/projects | Browse projects, open detail, postuler (needs project + candidature) |
| **Client – project detail** | http://localhost:4200/client/projects/:id | Candidatures, schedule interview (needs project + candidature + interview) |
| **Freelancer – My schedule** | http://localhost:4200/freelancer/availability | Set weekly availability, generate slots |
| **Freelancer – My interviews** | http://localhost:4200/freelancer/interviews | List interviews, confirm/cancel/complete |
| **Client – My project interviews** | http://localhost:4200/client/interviews | List interviews (owner 1) |
| **Admin – All interviews** | http://localhost:4200/admin/interviews | List all interviews |

The app uses **hardcoded user IDs** (client = 1, freelancer = 1) for list filters.  
**If you see "Failed to load projects"**, start **project-service** (step 3) and ensure Eureka + API Gateway are running.
