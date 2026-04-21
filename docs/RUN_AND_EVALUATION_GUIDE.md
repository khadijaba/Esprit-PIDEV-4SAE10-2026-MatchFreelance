# Run commands & evaluation guide (full marks + advanced features)

**Base path:** `PS D:\study\4SAE\Semestre 2\PIDEV\spring-task-planning>`

**To stop:** `Ctrl+C` in each terminal, or close the terminal.

---

## 1. Commands in order (one terminal per block)

Run each block in a **new terminal**, from the project root.

```powershell
# Terminal 1 – Eureka (8761)
cd eureka-server; mvn -q -DskipTests spring-boot:run
```

```powershell
# Terminal 2 – API Gateway (8081)
cd api-gateway; mvn -q -DskipTests spring-boot:run
```

```powershell
# Terminal 3 – Project service (8082)
cd project-service; mvn -q -DskipTests spring-boot:run
```

```powershell
# Terminal 4 – Candidature service (8083)
cd candidature-service; mvn -q -DskipTests spring-boot:run
```

```powershell
# Terminal 5 – Contract service (8084)
cd contract-service; mvn -q -DskipTests spring-boot:run
```

```powershell
# Terminal 6 – Interview service (8085)
cd interview-service; mvn -q -DskipTests spring-boot:run
```

```powershell
# Terminal 7 – User service (8086)
cd user-service; mvn -q -DskipTests spring-boot:run
```

```powershell
# Terminal 8 – Angular (4200)
cd angular; npm install; npx ng serve --open
```

Then open **http://localhost:4200**. API via gateway: **http://localhost:8081**.

---

## 2. What you need for full marks (grille)

| AA | Critère | To get full marks (brief) |
|----|---------|---------------------------|
| **AA1 [C1]** | Logique métier & analyse | **CRUD complet** interviews + availability; **lifecycle** (confirm/reject/cancel/complete/no-show); **Angular** list, detail, schedule, availability. **Advanced**: suggestions, fiabilité, workload, top N, politique annulation, visio. |
| **AA2 [C2]** | Architecture | **Eureka** (8761), **Gateway** (8081), **interview-service** (8085) and others registered; routes `/api/interviews/**`, `/api/availability/**` → interview-service. |
| **AA3 [C3]** | Synthèse & oral | Explain **Angular → proxy /api → Gateway 8081**; interview.service.ts calls `/api/interviews`, `/api/availability`, `/api/reviews`. One **clear demo scenario** (see §4). |
| **AA4 [C4]** | GitHub & standardisation | Git workflow; repo name type **Esprit-PIDEV-4SAE10-2026-MatchFreelance**; description with "Esprit School of Engineering – Tunisia", AU, tech; **topics**: esprit-school-of-engineering, academic-project, esprit-pidev, 2025-2026, angular, spring-boot; **README**: Overview, Features, Tech Stack, Architecture, Contributors, Academic Context, Getting Started. |
| **AA5 [C5]** | Excellence | Show **innovation**: e.g. slot suggestions algorithm, fiabilité + workload, visio in-app (Jitsi), ICS/export, notifications/reminders. |

---

## 3. Advanced features – brief + code placement

| Feature | Brief | Backend | Frontend |
|--------|------|---------|----------|
| **Alternative slot suggestions** | When create fails (conflict / no slot), API returns scored alternatives; user picks one and form is filled. | `InterviewController` `@PostMapping("/suggestions")`; `InterviewService.suggestAlternatives`, `computeSuggestionScore`. | `interview.service.ts` `suggestAlternatives`; `interview-schedule.component.ts` `loadSuggestions`, `useSuggestion` (on create error). |
| **Reliability (fiabilité)** | Score per freelancer (client view) or per owner (freelancer view) from COMPLETED/NO_SHOW/CANCELLED. | `InterviewController` `GET /reliability` (freelancerId or ownerId); `InterviewService.computeReliabilityForFreelancer`, `computeReliabilityForOwner`, `computeReliability`. | `interview.service.ts` `getReliabilityForFreelancer`, `getReliabilityForOwner`; `interview-list.component.ts` `loadReliability`, column "Fiabilité freelancer" / "Fiabilité client". |
| **Workload** | Classification (LIGHT/NORMAL/BUSY/OVERLOADED) from CONFIRMED interviews in next 7 days. | `InterviewController` `GET /workload?freelancerId=`; `InterviewService.computeWorkloadForFreelancer`. | `interview.service.ts` `getWorkloadForFreelancer`; `interview-list.component.ts` `loadReliability` (freelancer), `workloadLabel` above table. |
| **Top N freelancers** | Best freelancers by reliability + reviews (optional). | `InterviewController` `GET /top-freelancers`; `InterviewService.getTopFreelancersInInterviews`. | Service method present; can be used in admin or client UI. |
| **Cancellation policy** | Cancel allowed only if &gt; 24h before start. | `InterviewService.cancel` checks `cancellationAllowedHoursBefore`; throws if too late. | `interview-list.component.ts` `cancel`; toast on error. |
| **Visio (time window)** | In-app meeting (Jitsi); join only between startAt and endAt. | `InterviewController` `GET /{id}/visio-room`; `InterviewService.getOrCreateVisioRoom`, `assertWithinAccessWindow`. | `interview.service.ts` `getVisioRoom`; `visio-call.component.ts` getById → check window → getVisioRoom → embed; routes `client/interviews/:id/visio`, `freelancer/interviews/:id/visio`. "Rejoindre" only when in window. |
| **ICS export** | Download .ics for one interview. | `InterviewController` `GET /{id}/ics`; `InterviewService.generateIcs`. | `interview.service.ts` `downloadIcs`; interview-detail "Ajouter au calendrier". |
| **Availability (slots)** | Freelancer creates/list/deletes slots; used for scheduling and suggestions. | `AvailabilityController` slots CRUD + batch; `AvailabilityService`. | `interview.service.ts` createSlot, createSlotsBatch, getSlots, deleteSlot; `freelancer-availability.component.ts`. |

---

## 4. Brief demo scenario (what to show)

1. **Eureka + Gateway** – All services registered; Gateway routes `/api/interviews/**` to interview-service.
2. **Availability** – Freelancer: Mes disponibilités → create slots (batch). Base for scheduling and suggestions.
3. **Create interview** – Client: project → Entretiens → choose freelancer, date, mode → Create. Then trigger **suggestions**: pick conflicting time → error + suggested slots → click one → form filled → Create again.
4. **Reliability & workload** – Client: Mes entretiens → column "Fiabilité freelancer". Freelancer: Mes entretiens → "Charge: …" and "Fiabilité client" per row.
5. **Lifecycle** – Freelancer: Accepter (PROPOSED → CONFIRMED). Client/Freelancer: Annuler (only if &gt; 24h). After end time: Terminé (COMPLETED) or No show (NO_SHOW).
6. **Visio** – CONFIRMED ONLINE interview in time window → "Rejoindre" → visio page with Jitsi; outside window → disabled or error.
7. **Admin** – Entretiens: filters (status, mode, projectId, freelancerId, ownerId, dates), pagination.

Detailed scenario: `docs/INTERVIEW_CRUD_AND_ADVANCED_FUNCTIONS.txt` (Part 6).

---

## 5. References

- **Grille:** `docs/GRILLE_EVALUATION.md`
- **CRUD + advanced + UI tests:** `docs/INTERVIEW_CRUD_AND_ADVANCED_FUNCTIONS.txt`
- **Run & UI entry points:** `docs/RUN_PLATFORM_FOR_INTERVIEW_TEST.md`
