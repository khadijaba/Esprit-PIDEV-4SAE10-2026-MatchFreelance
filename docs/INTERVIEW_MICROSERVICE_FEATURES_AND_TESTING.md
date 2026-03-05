# Interview microservice — features & how to test

All features below belong to the **Interview microservice** (port 8085). Use the API Gateway at **http://localhost:8081** or the Angular app at **http://localhost:4200** (proxy forwards `/api` to the gateway).

---

## Services started (order)

| # | Service            | Port | Command (from project root) |
|---|--------------------|------|-----------------------------|
| 1 | Eureka             | 8761 | `cd eureka-server && mvn -q -DskipTests spring-boot:run` |
| 2 | API Gateway        | 8081 | `cd api-gateway && mvn -q -DskipTests spring-boot:run` |
| 3 | project-service    | 8082 | `cd project-service && mvn -q -DskipTests spring-boot:run` |
| 4 | candidature-service| 8083 | `cd candidature-service && mvn -q -DskipTests spring-boot:run` |
| 5 | contract-service    | 8084 | `cd contract-service && mvn -q -DskipTests spring-boot:run` |
| 6 | **interview-service** | 8085 | `cd interview-service && mvn -q -DskipTests spring-boot:run` |
| 7 | Angular            | 4200 | `cd angular && npx ng serve --open` |

Base URL for API tests: **http://localhost:8081** (gateway) or **http://localhost:8085** (direct).

---

## 1. Availability (créneaux)

**What it does:** Freelancers define time slots; owners can book one when creating an interview.

| Feature | API | How to test (API) | How to test (UI) |
|--------|-----|-------------------|------------------|
| Create one slot | `POST /api/availability/freelancers/{freelancerId}/slots`  
Body: `{ "startAt": "2026-04-01T10:00:00Z", "endAt": "2026-04-01T10:30:00Z" }` | `curl -X POST http://localhost:8081/api/availability/freelancers/1/slots -H "Content-Type: application/json" -d "{\"startAt\":\"2026-04-01T10:00:00Z\",\"endAt\":\"2026-04-01T10:30:00Z\"}"` | **Freelancer → My schedule** → “Add a one-off slot” → set Start/End → Add slot |
| Create slots in batch | `POST /api/availability/freelancers/{freelancerId}/slots/batch`  
Body: `{ "slots": [ { "startAt": "...", "endAt": "..." }, ... ] }` (max 200) | Same shape as single slot, array in `slots` | **Freelancer → My schedule** → set weekly ranges (Range 1 / Range 2) → “Generate for the next N weeks” → **Generate slots** |
| List slots | `GET /api/availability/freelancers/{freelancerId}/slots?onlyFree=true&page=0&size=10` | `curl "http://localhost:8081/api/availability/freelancers/1/slots"` | **Freelancer → My schedule** → table “Your slots” |
| Delete slot (free only) | `DELETE /api/availability/slots/{slotId}` | `curl -X DELETE http://localhost:8081/api/availability/slots/1` | **Freelancer → My schedule** → Remove on a **Free** slot |

---

## 2. Interviews (CRUD & lifecycle)

**What it does:** Create, search, update, confirm, cancel, complete, delete interviews. Supports ONLINE (meetingUrl) and FACE_TO_FACE (addressLine, city).

| Feature | API | How to test (API) | How to test (UI) |
|--------|-----|-------------------|------------------|
| Create (with slot) | `POST /api/interviews`  
Body: `{ "freelancerId": 1, "ownerId": 1, "slotId": 1, "mode": "ONLINE", "meetingUrl": "https://meet.example.com/abc" }` | Create a slot first, then POST with `slotId` | **Client → project detail** → candidature → Schedule interview (pick slot). Or call API then see in list. |
| Create (manual times) | Same, but `startAt`, `endAt` instead of `slotId`; for FACE_TO_FACE use `addressLine`, `city` | `POST` without `slotId`, with `startAt`/`endAt` and `meetingUrl` or `addressLine`+`city` | Schedule flow with manual date/time if no slot picker |
| Search (paginated, filters) | `GET /api/interviews?freelancerId=1&ownerId=1&status=CONFIRMED&page=0&size=10&sort=startAt,desc` | `curl "http://localhost:8081/api/interviews?freelancerId=1"` | **Admin → Interviews** (all), **Client → Interviews** (owner 1), **Freelancer → My interviews** (freelancer 1); use filters |
| Update | `PUT /api/interviews/{id}`  
Body: partial (notes, meetingUrl, startAt/endAt, etc.) | `PUT` with JSON body | (If UI exposes edit interview) or API only |
| Confirm | `POST /api/interviews/{id}/confirm` | `curl -X POST http://localhost:8081/api/interviews/1/confirm` | **Freelancer/Client/Admin → Interviews** → Confirm on a PROPOSED row |
| Cancel | `POST /api/interviews/{id}/cancel`  
(Blocked if &lt; 24h before start — see Politique d’annulation) | `curl -X POST http://localhost:8081/api/interviews/1/cancel` | Same list → Cancel on a CONFIRMED/PROPOSED row |
| No-show | `POST /api/interviews/{id}/no-show` | When cancel is refused (too late), call this to mark NO_SHOW | Same list → (if you add a “No-show” button) or API |
| Complete | `POST /api/interviews/{id}/complete` | `curl -X POST http://localhost:8081/api/interviews/1/complete` | Same list → Complete on a CONFIRMED row |
| Delete | `DELETE /api/interviews/{id}` | `curl -X DELETE http://localhost:8081/api/interviews/1` | (If UI exposes delete) or API only |

**Status flow:** PROPOSED → CONFIRMED → COMPLETED (or CANCELLED / NO_SHOW).

---

## 3. Notifications in-app

**What it does:** Notifications created automatically for interview events; list and mark as read by user.

| Feature | API | How to test (API) | How to test (UI) |
|--------|-----|-------------------|------------------|
| List by user | `GET /api/notifications/users/{userId}?page=0&size=20` | `curl "http://localhost:8081/api/notifications/users/1"` | (Integrate in layout: bell icon → list for current user) |
| Mark one as read | `POST /api/notifications/{notificationId}/read?userId=1` | `curl -X POST "http://localhost:8081/api/notifications/1/read?userId=1"` | Click on a notification → mark read |
| Mark all as read | `POST /api/notifications/users/{userId}/read-all` | `curl -X POST "http://localhost:8081/api/notifications/users/1/read-all"` | “Mark all as read” in notification panel |

**When they are created:** On interview create (PROPOSED), confirm (CONFIRMED), cancel (CANCELLED), complete (COMPLETED), no-show (NO_SHOW). Types: INTERVIEW_PROPOSED, INTERVIEW_CONFIRMED, INTERVIEW_CANCELLED, INTERVIEW_COMPLETED, INTERVIEW_NO_SHOW, REMINDER_24H, REMINDER_1H.

---

## 4. Rappels (reminders)

**What it does:** A scheduled job (every 15 min) creates REMINDER_24H and REMINDER_1H notifications for CONFIRMED interviews starting in ~24h or ~1h.

| Feature | How to test |
|--------|-------------|
| Automatic reminders | Create a CONFIRMED interview with `startAt` in ~24h or ~1h; wait up to 15 min (or trigger job); then `GET /api/notifications/users/{userId}` and look for REMINDER_24H / REMINDER_1H. |
| Read reminders | Same as notifications: list and mark as read (same APIs). |

Config: `interview.reminders.cron=0 */15 * * * *` in `application.properties`.

---

## 5. Politique d’annulation (cancellation policy)

**What it does:** Cancel is allowed only if the interview starts in **more than 24 hours**. Otherwise the API returns an error and suggests NO_SHOW.

| Feature | How to test |
|--------|-------------|
| Cancel allowed | Create an interview with `startAt` in 2 days. `POST .../cancel` → 200, status CANCELLED. |
| Cancel refused | Create an interview with `startAt` in 12 hours. `POST .../cancel` → 400 with message like “Annulation non autorisée : il reste moins de 24 h…”. |
| NO_SHOW when too late | For that same interview, `POST /api/interviews/{id}/no-show` → 200, status NO_SHOW; slot freed if it was linked. |

Config: `interview.cancellation.allowed-hours-before=24`.

---

## 6. Visio in-app

**What it does:** For ONLINE interviews, returns a room id and join URL (existing meetingUrl or generated).

| Feature | API | How to test (API) | How to test (UI) |
|--------|-----|-------------------|------------------|
| Get or create visio room | `GET /api/interviews/{interviewId}/visio-room`  
(Only for mode ONLINE) | `curl "http://localhost:8081/api/interviews/1/visio-room"`  
Response: `{ "roomId": "room-...", "joinUrl": "https://..." }` | Add a “Join call” / “Open visio” button on interview detail that calls this and opens `joinUrl` in iframe or new tab |

---

## 7. Reviews (avis)

**What it does:** After an interview is COMPLETED, owner or freelancer can submit one review per interview (reviewer → reviewee, score 1–5, optional comment). Aggregate by reviewee for profile.

| Feature | API | How to test (API) | How to test (UI) |
|--------|-----|-------------------|------------------|
| Create review | `POST /api/interviews/{interviewId}/reviews?reviewerId=1`  
Body: `{ "revieweeId": 2, "score": 5, "comment": "Great!" }`  
(Interview must be COMPLETED; reviewer/reviewee must be owner/freelancer of that interview) | Complete an interview, then POST with owner as reviewer and freelancer as reviewee (or vice versa) | Add “Rate this interview” form on interview detail after complete (reviewerId = current user) |
| List by interview | `GET /api/interviews/{interviewId}/reviews` | `curl "http://localhost:8081/api/interviews/1/reviews"` | Interview detail → “Reviews” section |
| List by reviewee | `GET /api/reviews/reviewee/{revieweeId}?page=0&size=10` | `curl "http://localhost:8081/api/reviews/reviewee/1"` | Profile or “My reviews” page |
| Aggregate (avg + count) | `GET /api/reviews/reviewee/{revieweeId}/aggregate`  
Response: `{ "revieweeId": 1, "averageScore": 4.5, "reviewCount": 10 }` | `curl "http://localhost:8081/api/reviews/reviewee/1/aggregate"` | Profile page “Average rating” / “X reviews” |

---

## 8. Input validation (Interview & Availability)

**What it does:** DTO and path validation; 400 with clear message on error.

| Where | How to test |
|-------|-------------|
| Interview create | Send invalid body: e.g. ONLINE without `meetingUrl`, or FACE_TO_FACE without `addressLine`/`city`, or neither `slotId` nor both `startAt`/`endAt`, or `endAt` ≤ `startAt`. Expect 400 and message in response body. |
| Slot create | Send `endAt` ≤ `startAt` or missing times. Expect 400. |
| Batch slots | Send `slots` with &gt; 200 elements. Expect 400. |
| Path variables | Call e.g. `GET /api/interviews/0` or `GET /api/availability/freelancers/0/slots`. Expect 400. |

---

## 9. Schedule conflict detection (no double-booking)

**What it does:** prevents creating or rescheduling an interview that overlaps (by time range) with an existing **active** interview for the same freelancer or owner. Active = `PROPOSED`, `CONFIRMED`, `COMPLETED`. `CANCELLED` and `NO_SHOW` are ignored.

- **Core logic**:
  - `InterviewRepository.existsOverlappingForFreelancer(...)`
  - `InterviewRepository.existsOverlappingForOwner(...)`
  - `InterviewService.assertNoScheduleConflicts(...)` (called from `create` and `update` when times are set).
- **Business rule**:
  - If a conflicting interview exists for the freelancer → `IllegalStateException("Freelancer already has an interview overlapping this time range")`.
  - If a conflicting interview exists for the owner → `IllegalStateException("Owner already has an interview overlapping this time range")`.

**How to test (UI-only with seed data):**

- On startup, the data initializer creates sample interviews for freelancer `1` and owners `1`/`2`. To see conflicts purely from the UI:
  1. Go to **Client → project detail** and schedule an interview for a time range, e.g. `2026-04-01 10:00–10:30`.
  2. Try to schedule another interview in an overlapping time for:
     - the same freelancer (any project), or
     - the same owner (even with another freelancer).
  3. You should see a toast error with the conflict message and the second interview will not be created.

You can also hit `POST /api/interviews` directly: create one interview, then POST another with overlapping `[startAt, endAt)` and the same `freelancerId` or `ownerId`.

---

## 10. iCalendar (.ics) export

**What it does:** generates a simple `.ics` file for any interview so users can add it directly to Google Calendar / Outlook / Apple Calendar.

- **Core logic**:
  - `InterviewService.generateIcs(Long interviewId)` builds an iCalendar (`VCALENDAR` / `VEVENT`) string with:
    - `UID`, `DTSTAMP`, `DTSTART`, `DTEND` (UTC).
    - `SUMMARY:Interview`.
    - For ONLINE: `DESCRIPTION` + `URL` with `meetingUrl`.
    - For FACE_TO_FACE: `LOCATION` from `addressLine` + `city`.
  - Endpoint: `GET /api/interviews/{interviewId}/ics` in `InterviewController`, returns:
    - `Content-Type: text/calendar; charset=utf-8`
    - `Content-Disposition: attachment; filename=interview-{id}.ics`

**How to test (UI recommended):**

- Add a “Download calendar invite” / “Add to calendar” button on interview detail that calls `/api/interviews/{id}/ics` and triggers a file download.
- Or manual:
  - `curl -v "http://localhost:8081/api/interviews/1/ics" -o interview-1.ics`
  - Open `interview-1.ics` with your calendar application and verify the time, URL/location.

---

## 11. Smart alternative slot suggestions (advanced)

**What it does:** when an interview cannot be created at the requested time (no availability or schedule conflict), the backend computes **better alternative times** for the same freelancer and owner instead of just returning an error.

- **Core logic (backend):**
  - `AvailabilitySlotRepository.findAllByFreelancerIdAndBookedFalseAndStartAtGreaterThanEqualAndEndAtLessThanEqual(...)`
  - `InterviewService.suggestAlternatives(InterviewCreateRequestDTO req)`:
    - Uses desired `[startAt, endAt]` to derive a desired duration.
    - Looks at free availability slots for that freelancer over the next 14 days.
    - For each slot that can contain that duration, picks a candidate start time as close as possible to the requested time but clamped into the slot.
    - Skips candidates that would conflict with existing PROPOSED/CONFIRMED/COMPLETED interviews for the **freelancer or owner** (reuses the overlap queries).
    - Computes a **score** per candidate:
      - Base = absolute difference in minutes between preferred and candidate start.
      - Extra penalty for very early/late local hours.
    - Sorts by score and returns the best 3–5 candidates as `AlternativeSlotSuggestionDTO { startAt, endAt, slotId, score }`.
  - Endpoint: `POST /api/interviews/suggestions` with the same body as create.

- **How to test (UI – recommended):**
  1. Go to **Client → project detail** for a candidature where you can schedule.
  2. In the **Interviews** card, click **Schedule**.
  3. Pick a date/time + duration that you know is **invalid**, for example:
     - Outside any availability slot for the freelancer, or
     - Overlapping another interview for the same freelancer/owner (triggering conflict).
  4. Click **Create**:
     - You should get an error toast (“Freelancer isn’t available…” or conflict message).
     - Below the form, a **“Suggested alternative times”** section appears with small buttons like:  
       `2026-04-01 11:00 → 11:30`, `2026-04-01 14:00 → 14:30`, …
  5. Click one of the suggestions:
     - The date, time and duration fields are pre-filled with that suggestion.
     - Toast: “Using suggested time. Click Create to confirm.”
  6. Click **Create** again:
     - The interview is successfully scheduled at the suggested time.

- **How to test (API-only):**
  1. Build a request body that you know will fail for `/api/interviews` (e.g. overlaps a known interview).
  2. POST it to `/api/interviews/suggestions` directly:
     - `curl -X POST http://localhost:8081/api/interviews/suggestions -H "Content-Type: application/json" -d '{...}'`
  3. Verify the JSON response contains a non-empty array of `{ startAt, endAt, slotId, score }` with times inside available slots that do **not** overlap.

---

## 12. Reliability index (advanced)

**What it does:** computes a **reliability score** for each freelancer and project owner based on their past interviews. Reliability reflects how often interviews are completed vs. cancelled or no-show.

- **Core logic (backend):**
  - Repository helpers:
    - `findByFreelancerIdAndStartAtBetween(freelancerId, from, to)`
    - `findByOwnerIdAndStartAtBetween(ownerId, from, to)`
  - Service methods:
    - `computeReliabilityForFreelancer(Long freelancerId, Instant from, Instant to)`
    - `computeReliabilityForOwner(Long ownerId, Instant from, Instant to)`
  - For a given user (freelancer or owner), in a window (default last **180 days**):
    - Let:
      - `c` = # COMPLETED interviews
      - `n` = # NO_SHOW interviews
      - `x` = # CANCELLED interviews (we treat late cancellations as less bad than no-shows)
      - `T = c + n + x`
    - If `T == 0` → neutral reliability = **0.5**.
    - Else:
      1. Raw score:
         \[
         R_0 = \frac{c - n - 0.5 x}{T}
         \]
         Clipped to \([0,1]\).
      2. Smoothed score (Laplace-style, with prior 0.7 and weight 3):
         \[
         \text{score} = \frac{R_0 \cdot T + 0.7 \cdot 3}{T + 3}
         \]
    - Returned as `ReliabilityResponseDTO { userId, role, score, completedCount, noShowCount, cancelledCount, from, to }`.
  - Endpoint:
    - `GET /api/interviews/reliability?freelancerId=...`
    - `GET /api/interviews/reliability?ownerId=...`

- **How to test (API-only):**
  1. Make sure you have a mix of interviews for a user:
     - Some COMPLETED, some NO_SHOW, some CANCELLED.
  2. Call, for example:  
     `curl "http://localhost:8081/api/interviews/reliability?freelancerId=1"`
  3. Check the response:
     - `score` is between 0 and 1.
     - `completedCount / noShowCount / cancelledCount` match your test data.

- **How to test (UI – client view):**
  1. Start the platform and log in as **Client** (demo assumes client id 1, freelancer id 1).
  2. Go to **Client → Interviews**.
  3. At the top of the table, you should see something like:  
     `Freelancer reliability: 86%`
  4. Manipulate test data:
     - Mark more interviews as NO_SHOW for that freelancer → score should decrease.
     - Mark more as COMPLETED → score should increase.

- **How to test (UI – freelancer view):**
  1. Log in as **Freelancer**.
  2. Go to **Freelancer → Interviews**.
  3. At the top, you should see:  
     `Owner reliability: 90%`  
     (for the owner associated with most of the listed interviews).

---

## 13. Workload classification & time-window access control (advanced)

**What it does:**

1. Computes a **workload summary** for each freelancer over the next 7 days, and classifies them as **LIGHT / NORMAL / BUSY / OVERLOADED**.
2. Enforces a strict **time window** for joining ONLINE interviews, both in backend and UI.

### 13.1 Workload classification

- **Core logic (backend):**
  - `InterviewService.computeWorkloadForFreelancer(Long freelancerId)`:
    - Window: `[now, now + 7 days]`.
    - Considers only **CONFIRMED** interviews.
    - For each such interview:
      - `minutes = (endAt - startAt)` in minutes; ignore non-positive.
      - Add to `totalMinutes7`.
      - If `startAt ≤ now + 24h`:
        - Add to `totalMinutes1`.
        - Increment `interviewsNext24h`.
      - If `startAt ≤ now + 3d`: increment `interviewsNext3d`.
      - Always increments `interviewsNext7d`.
      - Track per-day totals via local date to compute `maxDailyMinutes`.
    - Classification thresholds:
      - **OVERLOADED** if:
        - `totalMinutes7 ≥ 600` (≥10 hours in next week) **or**
        - `maxDailyMinutes ≥ 240` (≥4 hours of interviews on one day).
      - **BUSY** if:
        - `totalMinutes7 ≥ 360` (≥6 hours) **or**
        - `interviewsNext24h ≥ 3`.
      - **NORMAL** if:
        - `totalMinutes7 ≥ 120` (≥2 hours), but not BUSY/OVERLOADED.
      - **LIGHT** otherwise.
    - Returns `WorkloadSummaryDTO { freelancerId, from, to, totalMinutes7, totalMinutes1, interviewsNext24h, interviewsNext3d, interviewsNext7d, maxDailyMinutes, level }`.
  - Endpoint:  
    `GET /api/interviews/workload?freelancerId=1`

- **How to test (API-only):**
  1. Create several CONFIRMED interviews for freelancer `1` over the next 7 days with different durations and dates.
  2. Call:  
     `curl "http://localhost:8081/api/interviews/workload?freelancerId=1"`
  3. Check that:
     - `totalMinutes7` is roughly the sum of durations.
     - `totalMinutes1`, `interviewsNext24h`, `interviewsNext3d`, `interviewsNext7d` match your data.
     - `level` changes appropriately when you cross thresholds (add/remove interviews).

- **How to test (UI – freelancer view):**
  1. Log in as **Freelancer**.
  2. Go to **Freelancer → Interviews**.
  3. At the top of the list, you should see:  
     `My workload: busy (3 interviews, 7.5h next 7 days)`  
     (values vary depending on seeded/test data).
  4. Add or remove CONFIRMED interviews and reload; the text should update.

### 13.2 Time-window aware access control

This refines and formalizes the join rules for ONLINE interviews:

- **Backend (InterviewService):**
  - `getOrCreateVisioRoom(Long interviewId)` calls:
    - `assertWithinAccessWindow(Interview interview)`:
      - If `now < startAt`: throws  
        `"Online meeting is not available yet (before the scheduled start time)"`.
      - If `now > endAt`: throws  
        `"Online meeting is no longer available (after the scheduled end time)"`.
  - This ensures the visio room is **only usable between** `startAt` and `endAt`.

- **Frontend (Angular):**
  - `InterviewListComponent`:
    - `canJoinOnline(i: Interview)` mirrors the same window check (`startAt ≤ now ≤ endAt`).
    - The “**Join online**” link in the Link/Place column is shown only when `canJoinOnline(i)` is true; otherwise it shows “Not available”.
    - `timeLeftLabel(i)` shows:
      - `Starts in X` (before)
      - `In progress (X left)` (during)
      - `Ended` (after)
  - `VisioCallComponent`:
    - Before calling `getVisioRoom`, it fetches the interview via `GET /api/interviews/{id}` and performs the same time check on the client.
    - If too early/late, it shows a clear message and does **not** open the iframe.

- **How to test (UI):**
  1. Create an ONLINE CONFIRMED interview that starts a few minutes in the future.
  2. As client or freelancer, go to the relevant **Interviews** list:
     - Before start: “Join online” should be disabled / show “Not available”.
  3. When the clock reaches the start time (within the scheduled window):
     - “Join online” appears and routes to `/client/interviews/{id}/visio` or `/freelancer/interviews/{id}/visio`.
     - The meeting loads inside the in-app iframe (Jitsi room).
  4. After `endAt`:
     - “Join online” disappears / shows “Not available”.
     - Hitting the visio route directly will now show an error message from backend/frontend.

---

## Quick checklist (Interview microservice only)

- [ ] **Availability:** Create slot (single + batch), list, delete free slot.  
- [ ] **Interviews:** Create (slot + manual), search with filters, confirm, cancel, complete, no-show.  
- [ ] **Notifications:** List by user, mark one read, mark all read; see INTERVIEW_* and REMINDER_* types.  
- [ ] **Rappels:** Have CONFIRMED interview in ~24h/~1h, wait for job, check notifications.  
- [ ] **Politique d’annulation:** Cancel &gt;24h (OK), cancel &lt;24h (400), then no-show.  
- [ ] **Visio:** GET visio-room for an ONLINE interview, use joinUrl.  
- [ ] **Reviews:** Complete interview → create review → list by interview / by reviewee → aggregate.  
- [ ] **Validation:** Trigger 400 for invalid create/update and path params.
- [ ] **Advanced – Suggestions:** Force a failed schedule → see smart alternative slot suggestions and use one to create.  
- [ ] **Advanced – Reliability:** Check freelancer/owner reliability API + badges in interview lists.  
- [ ] **Advanced – Workload & access:** Check freelancer workload label and verify join availability window for ONLINE interviews.

All microservices above have been started in the background. Use **http://localhost:8081** for API calls via the gateway and **http://localhost:4200** for the Angular UI (after running `ng serve --open` in the `angular` folder).
