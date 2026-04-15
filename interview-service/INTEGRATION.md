# Interview Service Integration Guide

This document is a conflict-prevention and integration-clarity reference for `interview-service`.
It answers: what another engineer or Angular caller must know to avoid breaking integration.

Primary evidence sources:
- `interview-service/src/main/java/com/freelancing/interview/controller/*.java`
- `interview-service/src/main/java/com/freelancing/interview/service/*.java`
- `interview-service/src/main/java/com/freelancing/interview/dto/*.java`
- `angular/src/app/services/interview.service.ts`
- `angular/src/app/components/interview-*/**/*.ts`, `angular/src/app/components/visio-call/visio-call.component.ts`

## 1. SERVICE IDENTITY

- **Spring application name**: `interview-service` (`interview-service/src/main/resources/application.properties`)
- **Default port**: `8085`
- **API base paths**:
  - `/api/interviews`
  - `/api/availability`
  - `/api/reviews/*` and `/api/interviews/*/reviews` (via `ReviewController` with class base `/api`)
  - `/api/notifications`
- **Docker Compose service name**: `interview-service` (`docker-compose.yml`)
- **Docker Compose container name**: `mf-interview-service`
- **Active Spring profile**: `mysql` (`spring.profiles.active=mysql`)
- **Profile effect**:
  - base file has H2 defaults (`jdbc:h2:mem:interviews`)
  - mysql profile switches to `freelancing_interviews` (`application-mysql.properties`)
- **Runtime env vars impacting behavior**:
  - `SPRING_PROFILES_ACTIVE`
  - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `JITSI_BASE_URL`
  - optional scheduler/cancellation config from properties:
    - `interview.cancellation.allowed-hours-before`
    - `interview.reminders.cron`

### Port conflicts across compose roster

| Service | Host port |
|---|---:|
| mysql | 3306 |
| eureka-server | 8761 |
| api-gateway | 8081 |
| project-service | 8082 |
| candidature-service | 8083 |
| contract-service | 8084 |
| interview-service | 8085 |
| user-service | 8086 |
| productivity-service | 8087 |
| angular | 4200 |

No direct collisions in compose; host conflicts only if local processes already bind those ports.

## 2. FULL ENDPOINT CONTRACT

### 2.1 Endpoint inventory

| Method | Full path | Request | Response | Status codes | Notes |
|---|---|---|---|---|---|
| POST | `/api/interviews` | `InterviewCreateRequestDTO` | `InterviewResponseDTO` | 201, 400 | creates interview; conflict checks and slot booking logic in `InterviewService.create` |
| POST | `/api/interviews/suggestions` | `InterviewCreateRequestDTO` | `List<AlternativeSlotSuggestionDTO>` | 200, 400 | finds alternate slots |
| GET | `/api/interviews/reliability` | query: exactly one of `freelancerId` or `ownerId`; optional `from,to` | `ReliabilityResponseDTO` | 200, 400 | if both or neither IDs set -> 400 |
| GET | `/api/interviews/workload` | query: `freelancerId` | `WorkloadSummaryDTO` | 200, 400 | |
| GET | `/api/interviews/top-freelancers` | query: `limit>=1` default 5, `ownerId?`, `minReviews>=0` default 0, `from?`,`to?` | `List<TopFreelancerInInterviewsDTO>` | 200, 400 | combined score from reliability + reviews |
| GET | `/api/interviews` | query: `freelancerId,ownerId,projectId,candidatureId,status,mode,from,to,page,size,sort` | `Page<InterviewResponseDTO>` | 200, 400 | pageable search via JPA specs |
| GET | `/api/interviews/export/excel` | optional query: `ownerId,freelancerId` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | 200, 400 | exports PROPOSED+CONFIRMED |
| GET | `/api/interviews/{interviewId}` | path `@Min(1)` | `InterviewResponseDTO` | 200, 400 | |
| PUT | `/api/interviews/{interviewId}` | `InterviewUpdateRequestDTO` | `InterviewResponseDTO` | 200, 400 | reschedule blocked when slot-backed interview |
| POST | `/api/interviews/{interviewId}/confirm` | - | `InterviewResponseDTO` | 200, 400 | |
| POST | `/api/interviews/{interviewId}/cancel` | - | `InterviewResponseDTO` | 200, 400 | enforces hours-before-start policy |
| POST | `/api/interviews/{interviewId}/reject` | - | `InterviewResponseDTO` | 200, 400 | only PROPOSED and before start |
| POST | `/api/interviews/{interviewId}/no-show` | - | `InterviewResponseDTO` | 200, 400 | only after endAt |
| POST | `/api/interviews/{interviewId}/complete` | - | `InterviewResponseDTO` | 200, 400 | only CONFIRMED and after endAt |
| GET | `/api/interviews/{interviewId}/visio-room` | - | `VisioRoomResponseDTO` | 200, 400 | ONLINE only; only during time window |
| GET | `/api/interviews/{interviewId}/ics` | - | `text/calendar` | 200, 400 | ICS download |
| DELETE | `/api/interviews/{interviewId}` | - | none | 204, 400 | frees slot if linked |
| POST | `/api/availability/freelancers/{freelancerId}/slots` | `AvailabilitySlotCreateRequestDTO` | `AvailabilitySlotResponseDTO` | 201, 400 | |
| POST | `/api/availability/freelancers/{freelancerId}/slots/batch` | `AvailabilitySlotBatchCreateRequestDTO` | `List<AvailabilitySlotResponseDTO>` | 201, 400 | max 200 slots enforced by DTO |
| GET | `/api/availability/freelancers/{freelancerId}/slots` | query: `from?`,`to?`,`onlyFree` default true, page params | `Page<AvailabilitySlotResponseDTO>` | 200, 400 | range filtering only if both from/to present |
| DELETE | `/api/availability/slots/{slotId}` | - | none | 204, 400 | booked slots cannot be deleted |
| POST | `/api/interviews/{interviewId}/reviews` | query: `reviewerId`; body `ReviewCreateRequestDTO` | `ReviewResponseDTO` | 201, 400 | only for COMPLETED interview |
| GET | `/api/interviews/{interviewId}/reviews` | page params | `Page<ReviewResponseDTO>` | 200, 400 | |
| GET | `/api/reviews/reviewee/{revieweeId}` | page params | `Page<ReviewResponseDTO>` | 200, 400 | |
| GET | `/api/reviews/reviewee/{revieweeId}/aggregate` | - | `{ revieweeId, averageScore, reviewCount }` | 200, 400 | record DTO from service |
| GET | `/api/notifications/users/{userId}` | page params | `Page<NotificationResponseDTO>` | 200, 400 | |
| POST | `/api/notifications/{notificationId}/read` | query `userId` | `NotificationResponseDTO` | 200, 400 | validates notification ownership |
| POST | `/api/notifications/users/{userId}/read-all` | - | `{ message }` | 200, 400 | |

### 2.2 Request DTO contracts

| DTO | Field | Type | Required | Validation |
|---|---|---|---|---|
| InterviewCreateRequestDTO | candidatureId | Long | no | `@Positive` |
|  | projectId | Long | no | `@Positive` |
|  | freelancerId | Long | yes | `@NotNull @Positive` |
|  | ownerId | Long | yes | `@NotNull @Positive` |
|  | slotId | Long | conditional | `@Positive` |
|  | startAt/endAt | Instant | conditional | class-level validator requires slotId OR both times |
|  | durationMinutes | Integer | no | `@Positive` |
|  | mode | MeetingMode | no | default ONLINE in entity/service |
|  | meetingUrl | String | no | `@Size(max=2000)` |
|  | addressLine | String | no | `@Size(max=500)` |
|  | city | String | no | `@Size(max=200)` |
|  | lat/lng | Double | no | none |
|  | notes | String | no | `@Size(max=2000)` |
| InterviewUpdateRequestDTO | startAt/endAt | Instant | no | `@AssertTrue` ensures end > start when both provided |
|  | mode/status/meetingUrl/address/city/lat/lng/notes | mixed | no | size constraints on strings |
| AvailabilitySlotCreateRequestDTO | startAt,endAt | Instant | yes | `@NotNull`; class validator end>start |
| AvailabilitySlotBatchCreateRequestDTO | slots | List | yes | `@NotEmpty @Size(max=200) @Valid` |
| ReviewCreateRequestDTO | revieweeId | Long | yes | `@NotNull @Min(1)` |
|  | score | Integer | yes | `@NotNull @Min(1) @Max(5)` |
|  | comment | String | no | `@Size(max=2000)` |

### 2.3 Response DTO contracts

| DTO | Fields |
|---|---|
| InterviewResponseDTO | `id,candidatureId,projectId,freelancerId,ownerId,slotId,startAt,endAt,mode,meetingUrl,addressLine,city,lat,lng,status,notes,createdAt` |
| AlternativeSlotSuggestionDTO | `startAt,endAt,slotId,score` |
| ReliabilityResponseDTO | `userId,role,score,completedCount,noShowCount,cancelledCount,from,to` |
| WorkloadSummaryDTO | `freelancerId,from,to,totalMinutes7,totalMinutes1,interviewsNext24h,interviewsNext3d,interviewsNext7d,maxDailyMinutes,level` |
| TopFreelancerInInterviewsDTO | `freelancerId,combinedScore,reliabilityScore,averageReviewScore,reviewCount,completedCount,noShowCount,cancelledCount` |
| VisioRoomResponseDTO | `roomId,joinUrl` |
| AvailabilitySlotResponseDTO | `id,freelancerId,startAt,endAt,booked,bookedInterviewId` |
| ReviewResponseDTO | `id,interviewId,reviewerId,revieweeId,score,comment,createdAt` |
| NotificationResponseDTO | `id,userId,interviewId,type,message,readAt,createdAt` |

### 2.4 Status/error behavior

From `interview-service/.../exception/ValidationExceptionHandler.java`:
- `MethodArgumentNotValidException` -> 400 with `{ message, errors }`
- `ConstraintViolationException` -> 400 with `{ message }`
- `IllegalArgumentException` -> 400
- `IllegalStateException` -> 400 (note: not 409)

Known silent/default behaviors:
- `mode` defaults to ONLINE if omitted.
- for FACE_TO_FACE without lat/lng, service infers default coords from known city names.
- overlap checks include COMPLETED as blocking status in scheduling conflict query.

**OWNERSHIP_NOT_ENFORCED flags**
- No endpoint with `ownerId` in path exists in interview service.
- However access ownership/auth checks are generally caller-side and not token-enforced in these controllers.

## 3. ANGULAR SERVICE INTEGRATION MAP

Primary Angular caller: `angular/src/app/services/interview.service.ts`.

| Backend endpoint | Angular method | Components using it | Request shape actually sent | Response fields actually read |
|---|---|---|---|---|
| GET `/api/interviews` | `searchInterviews` | `InterviewListComponent`, `InterviewScheduleComponent` | params object serialized; includes page/size/sort and optional filters | `content,totalPages,number,size` |
| POST `/api/interviews` | `createInterview` | `InterviewScheduleComponent.scheduleInterview` | sends `startAt,endAt,durationMinutes` with IDs and mode details | success only; then reload list |
| POST `/api/interviews/suggestions` | `suggestAlternatives` | `InterviewScheduleComponent` | reuses create request object | `startAt,endAt,score` |
| GET `/api/interviews/{id}` | `getById` | `InterviewDetailComponent`, `VisioCallComponent` | path only | full interview object |
| GET `/api/interviews/{id}/ics` | `downloadIcs` | `InterviewDetailComponent` | none | blob |
| GET `/api/interviews/{id}/visio-room` | `getVisioRoom` | `VisioCallComponent` | none | `roomId,joinUrl` |
| POST status endpoints | `confirm/reject/cancel/complete/noShowInterview` | `InterviewListComponent`, `InterviewScheduleComponent` | empty object `{}` | interview object ignored; caller reloads |
| DELETE `/api/interviews/{id}` | `deleteInterview` | `InterviewScheduleComponent` | none | none |
| reliability/workload | `getReliabilityForFreelancer/getReliabilityForOwner/getWorkloadForFreelancer` | `InterviewListComponent` | query params | `score`, workload counters |
| top freelancers | `getTopFreelancers` | `ClientProjectDetailComponent` | optional `ownerId,limit,minReviews` | ranking fields |
| reviews | `getReviewsForInterview`, `createReview` | `InterviewDetailComponent` | review creation sends `reviewerId` query + body | page content and review fields |
| availability endpoints | create/list/delete slots and batch | `FreelancerAvailabilityComponent`, `InterviewScheduleComponent` | ISO date strings | slot fields |
| notifications endpoints | separate `NotificationService` (`/api/notifications/*`) | `NotificationMenuComponent` and layouts | `userId` path/query | page/content/read state |

Auto-triggered calls in components:
- `InterviewListComponent.ngOnInit`: `searchInterviews` -> then `loadDisplayNames` (`UserService`) + `loadReliability` (multiple interview calls in `forkJoin`) + workload call for freelancer role.
- `InterviewDetailComponent.ngOnInit`: `getById` -> `ProjectService.getById` + `UserService.getDisplayName` + `getReviewsForInterview`.
- `VisioCallComponent.ngOnInit`: `getById` -> time-window check -> `getVisioRoom` -> dynamic load of `external_api.js` from join URL origin.
- `FreelancerAvailabilityComponent.ngOnInit`: list availability slots.

Optimistic UI patterns:
- `NotificationMenuComponent.markAllAsRead` sets local `readAt` timestamps immediately after success without refetch.
- Most interview actions reload after success and are not optimistic before response.

## 4. KNOWN CONFLICTS AND BUG RISKS

> ⚠️ **CRITICAL** — `InterviewService.search` / `InterviewController.search`
- **Location**: `interview-service/src/main/java/com/freelancing/interview/controller/InterviewController.java`
- **Problem**: no backend auth binding between current user and `ownerId`/`freelancerId` query filters.
- **Trigger**: caller requests another user's interviews by changing query params.
- **Angular impact**: role UI assumes scoped data, but direct API calls can access broader data.
- **Recommended fix**: enforce user context server-side and reject unauthorized owner/freelancer filters.

> ⚠️ **HIGH** — `AvailabilityController` paths with `freelancerId`
- **Problem**: no server-side auth check that caller is that freelancer.
- **Trigger**: direct API call with another freelancer ID.
- **Angular impact**: freelancer availability can be modified across users.
- **Fix**: ownership assertion in `AvailabilityService` based on authenticated principal.

> ⚠️ **HIGH** — `VisioCallComponent` loads `external_api.js` from `joinUrl` origin
- **Location**: `angular/src/app/components/visio-call/visio-call.component.ts`
- **Problem**: script source is derived from backend-provided URL.
- **Trigger**: misconfigured/compromised `JITSI_BASE_URL`.
- **Impact**: frontend executes third-party script from provided origin.
- **Fix**: whitelist allowed Jitsi hosts.

- **MEDIUM** — cancellation/no-show/complete state rules are strict time-based; UI may show button while server rejects due to clock skew.
- **MEDIUM** — overlap check counts COMPLETED as blocking; can block reschedule scenarios unexpectedly.
- **LOW** — `@CrossOrigin(origins="*")` is broad and should be hardened for production.

## 5. DTO CONTRACT MISMATCHES

| Field | Backend contract | Angular sends/expects | Effect |
|---|---|---|---|
| `InterviewCreateRequestDTO.meetingUrl` | optional (`@Size`) and not required by service for ONLINE | Angular allows blank for in-app visio mode | works; no 400 |
| `InterviewCreateRequestValidator` class comment says ONLINE URL required | implementation does not enforce it | Angular often sends empty URL for ONLINE | comment-code drift; no runtime error |
| Review aggregate endpoint | backend returns `{revieweeId,averageScore,reviewCount}` | Angular model `ReviewAggregate` expects same | no mismatch |
| Notification page type | backend uses Spring `Page` JSON with `number` field | Angular `NotificationPage` reuses interview page model with `number` | consistent currently |

## 6. SHARED DATA DEPENDENCIES

Cross-service/shared IDs used by interview integration:
- `ownerId` and `freelancerId` originate from `user-service` identity (`AuthService.currentUser()` in Angular).
- `projectId` originates from `project-service` and is attached when scheduling from project detail.
- `candidatureId` originates from `candidature-service`; interview creation is typically initiated from candidature context.

Prerequisite sequencing in Angular:
- Client project detail loads project + candidatures first, then exposes interview scheduling UI with those IDs.
- Review creation depends on interview status transition to COMPLETED.

Drift risk:
- same user/entity IDs are duplicated across services and not FK-enforced across databases.

## 7. AUTO-TRIGGERED FLOWS

### 7.1 Interview list load chain
Trigger: route init (`InterviewListComponent.ngOnInit`).
Flow:
1. `searchInterviews`
2. `UserService.getDisplayNamesMap` (if client/freelancer role)
3. reliability calls in `forkJoin`
4. freelancer workload call
Failure impact: list still renders, but labels/reliability badges can be empty.

### 7.2 Interview detail load chain
Trigger: route `/client/interviews/:id` or `/freelancer/interviews/:id` init.
Flow:
1. `getById`
2. `ProjectService.getById`
3. `UserService.getDisplayName` for other participant
4. `getReviewsForInterview`
Failure impact: partial screen with missing secondary data.

### 7.3 Visio open chain
Trigger: route `/.../interviews/:id/visio` init.
Flow:
1. `getById`
2. local time-window check in component
3. `getVisioRoom`
4. load external Jitsi API script
5. instantiate Jitsi iframe API
Failure impact: cannot join call; toast displayed.

### 7.4 Reminder automation (backend)
Trigger: scheduler (`ReminderScheduler.createReminders`) every 15 minutes by default.
Flow:
1. query CONFIRMED interviews in 24h window
2. query CONFIRMED interviews in 1h window
3. create notification if absent for freelancer and owner
Failure impact: reminders missing; interview core flow unaffected.

## 8. ENVIRONMENT AND INFRASTRUCTURE

### Required env vars and defaults

```env
SPRING_PROFILES_ACTIVE=mysql
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/freelancing_interviews?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=<root by compose default>
JITSI_BASE_URL=https://meet.jit.si
```

Config keys in properties:
- `interview.cancellation.allowed-hours-before=24`
- `interview.reminders.cron=0 */15 * * * *`

### Compose dependency links
- interview depends on: `eureka-server`, `mysql`.
- consumed by: `api-gateway` route `/api/interviews/**,/api/availability/**,/api/notifications/**,/api/reviews/**`.

### DB and migration strategy
- schema: `freelancing_interviews`
- JPA strategy: `ddl-auto=update`
- manual migration scripts exist under `interview-service/src/main/resources/db/migration/`
- seed behavior: `InterviewDataInitializer` inserts rich sample data when interview/slot repositories are empty.

### External dependencies
- Jitsi URL generation in `InterviewService.getOrCreateVisioRoom` from `jitsi.base-url`.
- reminder notifications rely on scheduler enabled by `@EnableScheduling` in `InterviewServiceApplication`.

## 9. ANGULAR ENVIRONMENT CONFIG

- No `angular/src/environments/*.ts` files found.
- Angular interview service uses relative URLs:
  - `/api/interviews`
  - `/api/availability`
  - `/api/reviews`
  - `/api/notifications` via separate notification service
- Proxy config (`angular/proxy.conf.json`) routes all `/api` to `http://localhost:8081`.
- `angular/angular.json` uses `proxy.conf.json` for dev serve.
- Backend CORS: `@CrossOrigin(origins="*")` on all interview controllers; compatible with dev proxy and direct browser calls.

> INSPECT_REQUIRED: if deployment requires strict origins, introduce explicit allowed-origin config and align Angular runtime host(s).

## 10. INTEGRATION TEST CHECKLIST

- [ ] Unauthorized query scope — call `/api/interviews?ownerId=<other>` as another user — verify backend rejects (currently likely allows).
- [ ] Unauthorized availability mutation — create/delete slot for another freelancer ID — verify rejection.
- [ ] Slot booking lifecycle — create interview from slot then cancel/delete interview — verify slot returns to `booked=false`.
- [ ] Overlap prevention — create overlapping interviews for same owner/freelancer — verify 400 with conflict message.
- [ ] Time-window transitions — attempt COMPLETE/NO_SHOW before `endAt` and after `endAt` — verify rule enforcement.
- [ ] Cancellation policy boundary — cancel at `<24h` vs `>=24h` before start (or configured threshold).
- [ ] Visio room access boundary — call `/visio-room` before start and after end — verify blocked.
- [ ] Jitsi fallback behavior — run with default and custom `JITSI_BASE_URL`; verify `joinUrl` and script loading in `VisioCallComponent`.
- [ ] Review constraints — duplicate review by same reviewer and self-review attempt — verify 400.
- [ ] Notification ownership — mark notification as read with wrong `userId` — verify rejection.
- [ ] Auto-trigger chains — verify list/detail/visio pages still render partial state when secondary calls fail.

