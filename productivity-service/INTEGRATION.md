# Productivity Service Integration Guide

This document is an integration-safety reference for `productivity-service`.
All statements are based on current code in:
- `productivity-service/src/main/java/com/freelancing/productivity/controller/ProductivityController.java`
- `productivity-service/src/main/java/com/freelancing/productivity/service/*`
- `angular/src/app/services/productivity.service.ts`
- `angular/src/app/components/productivity-board/productivity-board.component.ts`

## 1. SERVICE IDENTITY

- **Spring application name**: `productivity-service` (`src/main/resources/application.yml`)
- **Default port**: `8087` (`application.yml`)
- **API base path**: `/api/productivity` (`ProductivityController` class-level `@RequestMapping`)
- **Docker Compose service name**: `productivity-service` (`docker-compose.yml`)
- **Compose container name**: `mf-productivity-service` (`docker-compose.yml`)
- **Active Spring profile**: `mysql` by default (`spring.profiles.active=mysql` in `application.yml`)
- **Profile effect**:
  - base `application.yml` points datasource to H2 (`jdbc:h2:mem:productivity`)
  - `application-mysql.yml` switches datasource to MySQL schema `freelancing_productivity`
- **Runtime env vars affecting behavior** (`docker-compose.yml` + `application.yml`):
  - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
  - `PRODUCTIVITY_AI_OLLAMA_ENABLED`
  - `PRODUCTIVITY_AI_OLLAMA_BASE_URL`
  - `PRODUCTIVITY_AI_OLLAMA_MODEL`
  - `PRODUCTIVITY_AI_OLLAMA_TEMPERATURE`
  - `PRODUCTIVITY_AI_OLLAMA_FALLBACK_ENABLED`

### Port conflict awareness across compose roster

| Service | Container port | Host port |
|---|---:|---:|
| mysql | 3306 | 3306 |
| eureka-server | 8761 | 8761 |
| api-gateway | 8081 | 8081 |
| project-service | 8082 | 8082 |
| candidature-service | 8083 | 8083 |
| contract-service | 8084 | 8084 |
| interview-service | 8085 | 8085 |
| user-service | 8086 | 8086 |
| productivity-service | 8087 | 8087 |
| angular (nginx) | 80 | 4200 |

- No direct collision inside compose.
- Host conflict risk exists if a local Angular dev server is already bound to `4200`.

## 2. FULL ENDPOINT CONTRACT

Source: `ProductivityController` + DTO classes under `productivity-service/src/main/java/com/freelancing/productivity/dto/`.

### 2.1 Endpoint inventory

| Method | Path | Params | Request DTO | Response DTO | Status codes | Notes |
|---|---|---|---|---|---|---|
| GET | `/api/productivity/owners/{ownerId}/tasks` | `ownerId: @Min(1)` | - | `List<TaskResponseDTO>` | 200, 400 | owner-scoped list (`TaskService.getTasksByOwner`) |
| GET | `/api/productivity/owners/{ownerId}/tasks/page` | `ownerId`, `q,status,priority,dueFrom,dueTo,page,size` | - | `PageResponseDTO<TaskResponseDTO>` | 200, 400 | default `page=0,size=10`; service caps size to 50 |
| POST | `/api/productivity/owners/{ownerId}/tasks` | `ownerId` | `TaskCreateRequestDTO` | `TaskResponseDTO` | 201, 400 | defaults applied if omitted (priority/plannedMinutes) |
| PUT | `/api/productivity/tasks/{taskId}` | `taskId` | `TaskUpdateRequestDTO` | `TaskResponseDTO` | 200, 400, 409 | partial update; nullable clear caveat for `goalId` |
| POST | `/api/productivity/tasks/{taskId}/start` | `taskId` | - | `TaskResponseDTO` | 200, 400, 409 | DONE cannot be restarted |
| POST | `/api/productivity/tasks/{taskId}/clear-goal` | `taskId` | - | `TaskResponseDTO` | 200, 400 | explicit goal-unassign path |
| POST | `/api/productivity/tasks/{taskId}/complete` | `taskId` | - | `TaskResponseDTO` | 200, 400 | sets `completedAt=now` |
| DELETE | `/api/productivity/tasks/{taskId}` | `taskId` | - | - | 204, 400 | no owner verification in service |
| GET | `/api/productivity/owners/{ownerId}/calendar.ics` | `ownerId` | - | `text/calendar` | 200, 400 | attachment filename `productivity-{ownerId}.ics` |
| GET | `/api/productivity/tasks/{taskId}/calendar.ics` | `taskId` | - | `text/calendar` | 200, 400 | attachment filename `task-{taskId}.ics` |
| GET | `/api/productivity/owners/{ownerId}/todo-lists` | `ownerId` | - | `List<TodoListResponseDTO>` | 200, 400 | owner-scoped |
| GET | `/api/productivity/owners/{ownerId}/todo-lists/page` | `ownerId`, `q,page,size` | - | `PageResponseDTO<TodoListResponseDTO>` | 200, 400 | default `page=0,size=10`; cap 50 |
| POST | `/api/productivity/owners/{ownerId}/todo-lists` | `ownerId` | `TodoListCreateRequestDTO` | `TodoListResponseDTO` | 201, 400 | |
| PUT | `/api/productivity/todo-lists/{listId}` | `listId` | `TodoListCreateRequestDTO` | `TodoListResponseDTO` | 200, 400 | no owner assertion |
| DELETE | `/api/productivity/todo-lists/{listId}` | `listId` | - | - | 204, 400 | no owner assertion |
| GET | `/api/productivity/todo-lists/{listId}/items` | `listId` | - | `List<TodoItemResponseDTO>` | 200, 400 | |
| GET | `/api/productivity/todo-lists/{listId}/items/page` | `listId`, `q,done,dueFrom,dueTo,page,size` | - | `PageResponseDTO<TodoItemResponseDTO>` | 200, 400 | |
| POST | `/api/productivity/todo-lists/{listId}/items` | `listId` | `TodoItemCreateRequestDTO` | `TodoItemResponseDTO` | 201, 400 | owner copied from parent list |
| PUT | `/api/productivity/todo-items/{itemId}` | `itemId` | `TodoItemUpdateRequestDTO` | `TodoItemResponseDTO` | 200, 400 | |
| POST | `/api/productivity/todo-items/{itemId}/toggle` | `itemId` | - | `TodoItemResponseDTO` | 200, 400 | |
| DELETE | `/api/productivity/todo-items/{itemId}` | `itemId` | - | - | 204, 400 | |
| GET | `/api/productivity/owners/{ownerId}/progress` | `ownerId` | - | `ProgressSummaryDTO` | 200, 400 | |
| GET | `/api/productivity/owners/{ownerId}/goals` | `ownerId` | - | `List<GoalResponseDTO>` | 200, 400 | |
| POST | `/api/productivity/owners/{ownerId}/goals` | `ownerId` | `GoalCreateRequestDTO` | `GoalResponseDTO` | 201, 400 | |
| PUT | `/api/productivity/goals/{goalId}` | `goalId` | `GoalCreateRequestDTO` | `GoalResponseDTO` | 200, 400 | no owner assertion |
| DELETE | `/api/productivity/goals/{goalId}` | `goalId` | - | - | 204, 400 | no owner assertion |
| GET | `/api/productivity/owners/{ownerId}/dependencies` | `ownerId` | - | `List<DependencyResponseDTO>` | 200, 400 | |
| POST | `/api/productivity/owners/{ownerId}/dependencies` | `ownerId` | `DependencyCreateRequestDTO` | `DependencyResponseDTO` | 201, 400, 409 | cycle => `IllegalStateException` -> 409 |
| DELETE | `/api/productivity/dependencies/{dependencyId}` | `dependencyId` | - | - | 204, 400 | no owner assertion |
| GET | `/api/productivity/owners/{ownerId}/dependencies/order` | `ownerId` | - | `TaskOrderResponseDTO` | 200, 400, 409 | cycle => 409 |
| POST | `/api/productivity/owners/{ownerId}/adaptive-reschedule` | `ownerId` | `AdaptiveRescheduleRequestDTO` (body optional) | `AdaptiveRescheduleResponseDTO` | 200, 400 | request null accepted |
| POST | `/api/productivity/owners/{ownerId}/conflicts/resolve` | `ownerId` | `ConflictResolutionRequestDTO` | `ConflictResolutionResponseDTO` | 200, 400 | > `⚠️ OWNERSHIP_NOT_ENFORCED` ownerId path ignored in service |
| GET | `/api/productivity/owners/{ownerId}/decisions` | `ownerId` | - | `List<DecisionLogResponseDTO>` | 200, 400 | |
| POST | `/api/productivity/owners/{ownerId}/decisions` | `ownerId` | `DecisionLogCreateRequestDTO` | `DecisionLogResponseDTO` | 201, 400 | |
| POST | `/api/productivity/owners/{ownerId}/ai/decompose` | `ownerId` | `AiDecomposeRequestDTO` | `AiDecomposeResponseDTO` | 200, 400, 409 | > `⚠️ OWNERSHIP_NOT_ENFORCED` ownerId path ignored in service |
| GET | `/api/productivity/owners/{ownerId}/ai/context-suggestions` | `ownerId` | - | `List<ContextSuggestionDTO>` | 200, 400, 409 | |
| GET | `/api/productivity/owners/{ownerId}/insights` | `ownerId` | - | `ProductivityInsightsDTO` | 200, 400 | |
| GET | `/api/productivity/owners/{ownerId}/weekly-review` | `ownerId` | - | `WeeklyReviewDTO` | 200, 400 | currently static prompts |

### 2.2 Request DTO field contracts

| DTO | Field | Type | Required | Validation |
|---|---|---|---|---|
| `TaskCreateRequestDTO` | `title` | `String` | yes | `@NotBlank @Size(max=180)` |
|  | `description` | `String` | no | `@Size(max=4000)` |
|  | `priority` | `ProductivityPriority` | no | enum |
|  | `plannedMinutes` | `Integer` | no | `@Min(5) @Max(720)` |
|  | `actualMinutes` | `Integer` | no | `@Min(0) @Max(1440)` |
|  | `goalId` | `Long` | no | none |
|  | `dueAt` | `Instant` | no | none |
| `TaskUpdateRequestDTO` | `title` | `String` | no | `@Size(max=180)` |
|  | `description` | `String` | no | `@Size(max=4000)` |
|  | `priority` | `ProductivityPriority` | no | enum |
|  | `plannedMinutes` | `Integer` | no | `@Min(5) @Max(720)` |
|  | `actualMinutes` | `Integer` | no | `@Min(0) @Max(1440)` |
|  | `goalId` | `Long` | no | none |
|  | `dueAt` | `Instant` | no | none |
|  | `status` | `ProductivityTaskStatus` | no | enum |
| `GoalCreateRequestDTO` | `title` | `String` | yes | `@NotBlank @Size(max=180)` |
|  | `description` | `String` | no | `@Size(max=2000)` |
|  | `targetDate` | `Instant` | no | none |
| `DependencyCreateRequestDTO` | `predecessorTaskId` | `Long` | yes | `@Min(1)` |
|  | `successorTaskId` | `Long` | yes | `@Min(1)` |
| `DecisionLogCreateRequestDTO` | `taskId` | `Long` | no | none |
|  | `decisionType` | `String` | yes | `@NotBlank @Size(max=80)` |
|  | `reason` | `String` | yes | `@NotBlank @Size(max=4000)` |
| `AiDecomposeRequestDTO` | `goalText` | `String` | yes | `@NotBlank @Size(max=2000)` |
|  | `maxSteps` | `Integer` | no | `@Min(2) @Max(12)` |
| `AdaptiveRescheduleRequestDTO` | `weekStart` | `LocalDate` | no | none |
|  | `dailyCapacityMinutes` | `Integer` | no | none |
| `TodoListCreateRequestDTO` | `name` | `String` | yes | `@NotBlank @Size(max=120)` |
| `TodoItemCreateRequestDTO` | `title` | `String` | yes | `@NotBlank @Size(max=250)` |
|  | `dueAt` | `Instant` | no | none |
| `TodoItemUpdateRequestDTO` | `title` | `String` | no | `@Size(max=250)` |
|  | `done` | `Boolean` | no | none |
|  | `dueAt` | `Instant` | no | none |
|  | `positionIndex` | `Integer` | no | `@Min(0)` |

### 2.3 Response DTO field contracts

| DTO | Fields |
|---|---|
| `TaskResponseDTO` | `id, ownerId, goalId, title, description, status, priority, plannedMinutes, actualMinutes, dueAt, completedAt, createdAt, updatedAt` |
| `GoalResponseDTO` | `id, ownerId, title, description, targetDate, totalTasks, doneTasks, completionRate, weeklyVelocity` |
| `DependencyResponseDTO` | `id, ownerId, predecessorTaskId, successorTaskId, createdAt` |
| `TaskOrderResponseDTO` | `ownerId, orderedTaskIds` |
| `ConflictResolutionResponseDTO` | `recommendedTaskId, deferredTaskId, recommendedScore, deferredScore, rationale, aiSource` |
| `AdaptiveRescheduleResponseDTO` | `ownerId, weekStart, dailyCapacityMinutes, allocations, aiSource` |
| `DailyTaskAllocationDTO` | `taskId, title, scheduledDate, allocatedMinutes` |
| `DecisionLogResponseDTO` | `id, ownerId, taskId, decisionType, reason, createdAt` |
| `AiDecomposeResponseDTO` | `inputGoal, suggestedSteps, rationale, aiSource` |
| `ContextSuggestionDTO` | `category, message, confidence` |
| `ProductivityInsightsDTO` | `ownerId, estimationAccuracyScore, completionRate, currentCompletionStreakDays, bestPerformanceHour, worstPerformanceHour` |
| `WeeklyReviewDTO` | `ownerId, prompts` |
| `TodoListResponseDTO` | `id, ownerId, name, totalItems, completedItems, createdAt` |
| `TodoItemResponseDTO` | `id, ownerId, listId, title, done, positionIndex, dueAt, createdAt, updatedAt` |
| `ProgressSummaryDTO` | `ownerId, totalTasks, doneTasks, inProgressTasks, blockedTasks, overdueTasks, completionRate, totalPlannedMinutesOpen` |
| `PageResponseDTO<T>` | `content, page, size, totalElements, totalPages, last` |

### 2.4 Error mapping

From `ValidationExceptionHandler`:
- 400: `MethodArgumentNotValidException`, `ConstraintViolationException`, `IllegalArgumentException`
- 409: `IllegalStateException`
- 500: any other exception

## 3. ANGULAR SERVICE INTEGRATION MAP

Primary caller files:
- `angular/src/app/services/productivity.service.ts`
- `angular/src/app/components/productivity-board/productivity-board.component.ts`
- route exposure in `angular/src/app/app.routes.ts` (`/freelancer/productivity` only)

| Backend endpoint | Angular service method | Consuming component(s) | Actual request shape sent | Response fields actually read | Auto-triggered / optimistic behavior |
|---|---|---|---|---|---|
| `GET /owners/{ownerId}/tasks/page` | `listTasksPage` | `ProductivityBoardComponent.loadTasks` | `q,status,priority,dueFrom,dueTo,page,size` as query | `content,totalPages` | Auto-called in `reloadAll()` and again after many mutations |
| `GET /owners/{ownerId}/tasks` | `listTasks` | `ProductivityBoardComponent.loadAllTasks` | none | full task list | Auto-called in `reloadAll()` |
| `POST /owners/{ownerId}/tasks` | `createTask` | `createTask`, `addBoardTask`, `applyDecompositionAsTasks` | task payload with optional `goalId/dueAt` | created `id` + fields | follow-up auto calls create dependencies then reload |
| `PUT /tasks/{taskId}` | `updateTask` | task status updates, goal assign, board transitions | partial body | none beyond success/reload | used in auto conflict flow before confirmation UI update |
| `POST /tasks/{taskId}/start` | `startTask` | status change, conflict auto-apply | empty body `{}` | none beyond success/reload | invoked automatically by conflict automation |
| `POST /tasks/{taskId}/complete` | `completeTask` | status change | empty body | none beyond success/reload | guarded client-side by predecessor check |
| `POST /tasks/{taskId}/clear-goal` | `clearTaskGoal` | `assignTaskGoal` | `{}` | updated task | explicit unassign path |
| `DELETE /tasks/{taskId}` | `deleteTask` | `deleteTask` | none | none | manual confirmation first |
| `GET /owners/{ownerId}/calendar.ics` | `downloadOwnerIcs` | `downloadCalendar` | none | blob only | triggers browser download |
| goals endpoints | `listGoals/createGoal/updateGoal/deleteGoal` | multiple goal actions | JSON body for create/update | goal fields used in UI cards | goals auto-loaded on init + reload |
| dependencies endpoints | `listDependencies/addDependency/removeDependency/getDependencyOrder` | nesting/ordering operations | link body `{ predecessorTaskId, successorTaskId }` | dependency IDs used heavily | dependency changes chain into reloads |
| `POST /owners/{ownerId}/adaptive-reschedule` | `adaptiveReschedule` | auto + manual planning | body includes `weekStart,dailyCapacityMinutes` (may be undefined) | `allocations,aiSource` | auto-triggered on task load with no filters |
| `POST /owners/{ownerId}/conflicts/resolve` | `resolveConflict` | auto conflict assistant | `{ firstTaskId, secondTaskId }` | recommended/deferred IDs + rationale | auto-triggered and then auto-applied via `forkJoin` |
| decisions endpoints | `listDecisions/createDecision` | decision log UI | create body includes `AUTO_CONFLICT_RESOLUTION` type | entries with `decisionType,createdAt` | auto write after conflict apply |
| AI endpoints | `decomposeGoal/getContextSuggestions` | AI decomposition + suggestions | decompose body `{goalText,maxSteps}` | `suggestedSteps,rationale,aiSource` | context suggestions auto-loaded on init |
| insights/review/progress | `getInsights/getWeeklyReview/getProgress` | dashboard cards | none | summary metrics + prompt list | auto-loaded on init |
| todo endpoints | list/create/update/toggle/delete list/items | same component | simple JSON bodies | todo counters and item fields | auto reload on mutations |

## 4. KNOWN CONFLICTS AND BUG RISKS

> ⚠️ **CRITICAL** — `POST /api/productivity/owners/{ownerId}/conflicts/resolve` (`ProductivityController.resolveConflict` -> `DecisionIntelligenceService.resolve(request)`)
- **Problem**: path `ownerId` is ignored in service.
- **Trigger**: caller can send foreign task IDs.
- **Angular impact**: auto conflict flow may act on tasks outside owner scope if backend data is exposed.
- **Fix**: change signature to `resolve(ownerId, request)` and verify both tasks belong to owner.

> ⚠️ **HIGH** — `POST /api/productivity/owners/{ownerId}/ai/decompose` (`ProductivityController.decomposeGoal` -> `CognitiveAssistService.decomposeGoal(request)`)
- **Problem**: ownerId path is unused.
- **Trigger**: any caller can invoke decomposition without owner context.
- **Angular impact**: no personalization and potential cross-owner future leakage if context is added later.
- **Fix**: propagate ownerId into service method.

> ⚠️ **HIGH** — `AdaptivePlanningService.buildAdaptiveWeekPlan`
- **Problem**: comparator chain uses `.reversed()` over due+priority chain.
- **Trigger**: mixed due dates and priorities.
- **Angular impact**: adaptive plan ordering may be counterintuitive/wrong.
- **Fix**: explicit comparator `dueAt asc nullsLast`, then priority desc.

> ⚠️ **HIGH** — task/goal/dependency/todo mutating methods by raw ID (`TaskService`, `GoalService`, `DependencyService`, `TodoService`)
- **Problem**: owner assertions missing in many mutators.
- **Trigger**: direct ID-based calls.
- **Angular impact**: potential cross-owner mutation if APIs are called directly.
- **Fix**: enforce owner ownership in service layer.

- **MEDIUM** — `TaskService.updateTask`: `goalId` only updated when non-null.
  - Trigger: Angular sends `{goalId: null}` via generic update.
  - Impact: silent no-op unless dedicated `clear-goal` endpoint is used.
  - Fix: keep explicit unassign API (already used) and document.

- **MEDIUM** — `IcsUtils.tasksToIcs`: fixed 30-minute duration.
  - Trigger: tasks with `plannedMinutes != 30`.
  - Impact: calendar mismatch for users.
  - Fix: derive `DTEND` from planned minutes with bounds.

- **LOW** — README drift (`productivity-service/README.md`) references outdated planning endpoints.

## 5. DTO CONTRACT MISMATCHES

| Field | Backend contract | Angular send/expect | Current behavior |
|---|---|---|---|
| `TaskUpdateRequestDTO.goalId` | nullable field, but service only applies when non-null (`TaskService.updateTask`) | Angular model allows `goalId?: number | null` | `null` is silently ignored unless `clearTaskGoal` endpoint is used |
| `AdaptiveRescheduleRequestDTO` body | body optional in controller; fields optional | Angular always sends object with `weekStart`/`dailyCapacityMinutes` keys | works; undefined fields are ignored |
| `ConflictResolutionRequestDTO` | strictly `firstTaskId`,`secondTaskId` | Angular sends exact shape | no mismatch, but owner not enforced |
| `WeeklyReviewDTO` | `ownerId,prompts` only | Angular expects same | no mismatch |
| `ProductivityInsightsDTO` | fixed small metric set | Angular expects exactly these fields | no mismatch now; additive backend fields are safe |

## 6. SHARED DATA DEPENDENCIES

- Shared identity key is **user ID** from `AuthService.currentUser().id` used as productivity `ownerId` (`ProductivityBoardComponent.ownerId`).
- Backend does not call interview service directly; integration is Angular-orchestrated.
- Potential cross-service drift:
  - `ownerId` semantics are shared with interview/project/user services but not centrally enforced by token-based auth in productivity APIs.
- Service dependencies via gateway:
  - Angular -> `/api/productivity/**` -> `api-gateway` route id `productivity-service` (`api-gateway/application.properties`).

## 7. AUTO-TRIGGERED FLOWS

### 7.1 Productivity board initial load
Trigger: `ProductivityBoardComponent.ngOnInit -> reloadAll()`.
Ordered calls:
1. tasks page
2. full task list
3. progress summary
4. goals
5. dependencies
6. decision logs
7. insights
8. context suggestions
9. weekly review

Breakage pattern:
- Any failed call shows toast, but page continues rendering partial data.

### 7.2 Auto conflict resolution flow
Trigger: `loadTasks` success with no active filters.
Sequence:
1. `resolveConflict(ownerId,{firstTaskId,secondTaskId})`
2. On suggestion success, `applyConflictResolution` auto-calls:
   - `startTask` or `updateTask` on recommended task
   - `updateTask` on deferred task
   - `createDecision` with `decisionType=AUTO_CONFLICT_RESOLUTION`
3. Reload tasks and decision logs.

### 7.3 Auto adaptive plan flow
Trigger: same no-filter branch in `loadTasks`.
Sequence:
1. auto-compute week start
2. call `adaptiveReschedule`
3. bind plan in UI

## 8. ENVIRONMENT AND INFRASTRUCTURE

### Required env/runtime inputs

```env
# Common
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/

# AI behavior
PRODUCTIVITY_AI_OLLAMA_ENABLED=false
PRODUCTIVITY_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434
PRODUCTIVITY_AI_OLLAMA_MODEL=llama3.2
PRODUCTIVITY_AI_OLLAMA_TEMPERATURE=0.2
PRODUCTIVITY_AI_OLLAMA_FALLBACK_ENABLED=true
```

- If Ollama is enabled but unreachable:
  - with fallback enabled -> heuristic response
  - with fallback disabled -> endpoint returns 409/500 path from thrown exception

### Eureka/gateway resolution

- Eureka app name: `productivity-service`
- Gateway route: `lb://productivity-service` for `/api/productivity/**` (`api-gateway/application.properties`)

### Database/migrations/seeding

- Schema (mysql profile): `freelancing_productivity` (`application-mysql.yml`)
- Strategy: `spring.jpa.hibernate.ddl-auto=update`
- Seed behavior: `ProductivityDataInitializer` inserts sample data only when tasks and todo lists are empty.

## 9. ANGULAR ENVIRONMENT CONFIG

- No `angular/src/environments/*.ts` found in current workspace.
- Service URLs are hardcoded as **relative** API paths in Angular services (good for gateway/proxy).
- Proxy config (`angular/proxy.conf.json`) routes `/api` -> `http://localhost:8081`.
- Angular serve config (`angular/angular.json`) enables proxy via `serve.options.proxyConfig=proxy.conf.json`.
- Backend CORS is permissive (`@CrossOrigin(origins="*")` on controllers), which matches dev and containerized clients.

> INSPECT_REQUIRED: if environment-specific Angular base URLs are expected, add explicit environment files and move any future absolute URLs there.

## 10. INTEGRATION TEST CHECKLIST

- [ ] Owner boundary on conflict resolve — call `/owners/{ownerId}/conflicts/resolve` with foreign task IDs — verify request is rejected (currently vulnerable).
- [ ] Owner boundary on AI decompose — call `/owners/{ownerId}/ai/decompose` with mismatched owner context — verify owner is enforced (currently not enforced).
- [ ] Goal unassign contract — send `PUT /tasks/{id}` with `{"goalId":null}` — verify no-op and then verify `POST /tasks/{id}/clear-goal` actually clears.
- [ ] Adaptive ordering correctness — mixed due dates/priorities — verify plan is earliest due first and stable.
- [ ] Cascade integrity on delete — delete task/goal with dependencies — verify no dangling references (currently risk).
- [ ] Auto conflict sequence robustness — force one API call in auto-apply `forkJoin` to fail — verify partial state handling and decision log consistency.
- [ ] Weekly review and insights load failure isolation — break one endpoint and verify board remains usable.
- [ ] Ollama fallback path — enable Ollama and simulate unavailable endpoint — verify fallback behavior toggles with `PRODUCTIVITY_AI_OLLAMA_FALLBACK_ENABLED`.
- [ ] Todo pagination/filter typing — pass done=`''` and date filters — verify backend binding and result consistency.
- [ ] ICS export fidelity — tasks with varying planned minutes — verify duration contract (currently fixed 30m).

