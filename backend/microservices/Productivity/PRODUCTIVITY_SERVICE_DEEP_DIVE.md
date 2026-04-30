# Productivity Service Deep Dive

Last updated: 2026-04-15  
Codebase snapshot: `productivity-service` in `spring-task-planning`

## 1) Purpose and scope

This file is the implementation-level reference for the current backend module.
It is written as source-of-truth context for engineers and LLM-assisted work.

`productivity-service` currently provides:

- productivity task lifecycle (`TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED`)
- goals and goal-linked task metrics
- task dependency graph management with cycle prevention and topological ordering
- planning features (task suggestions, daily knapsack plan, adaptive weekly allocation)
- conflict resolution heuristics and decision logging
- AI-assisted goal decomposition and context suggestions (Ollama optional, heuristic fallback)
- todo lists and todo items
- progress/insights/weekly review summaries
- ICS calendar export

## 2) Runtime and architecture

### Stack

- Java 17
- Spring Boot 3.2.2
- Spring Web + Validation
- Spring Data JPA
- Eureka client
- Lombok

### Databases

- Active profile defaults to `mysql` (`spring.profiles.active=mysql`)
- H2 defaults are still present in base config
- `ddl-auto=update` is used in profile config

### Service identity

- Spring app name: `productivity-service`
- Default port: `8087`
- API base path: `/api/productivity`

### Package map

- `controller` - `ProductivityController`
- `service` - core behavior (`TaskService`, `TodoService`, `GoalService`, `DependencyService`, `PlanningService`, `AdaptivePlanningService`, `DecisionIntelligenceService`, `DecisionLogService`, `CognitiveAssistService`, `ProgressService`, `InsightsService`, `IcsUtils`)
- `entity` - JPA models
- `repository` - Spring Data repositories
- `dto` - API contracts
- `exception` - global error mapping
- `config` - startup seed initializer

## 3) Configuration and ops notes

### Key config files

- `src/main/resources/application.yml`
  - `server.port=8087`
  - `spring.application.name=productivity-service`
  - `spring.profiles.active=mysql`
  - AI namespace: `productivity.ai.ollama.*`
- `src/main/resources/application-mysql.yml`
  - MySQL datasource (`freelancing_productivity`)
  - MySQL dialect
  - `ddl-auto=update`

### Ollama toggles

- `PRODUCTIVITY_AI_OLLAMA_ENABLED`
- `PRODUCTIVITY_AI_OLLAMA_BASE_URL`
- `PRODUCTIVITY_AI_OLLAMA_MODEL`
- `PRODUCTIVITY_AI_OLLAMA_TEMPERATURE`
- `PRODUCTIVITY_AI_OLLAMA_FALLBACK_ENABLED`

## 4) Domain model

### `ProductivityTask` (`productivity_tasks`)

Core fields:

- `id`, `ownerId`, `goalId`
- `title` (max 180), `description` (max 4000)
- `status` (`ProductivityTaskStatus`)
- `priority` (`ProductivityPriority`)
- `dueAt`, `plannedMinutes`, `actualMinutes`, `completedAt`
- `createdAt`, `updatedAt`

Lifecycle:

- `@PrePersist` sets timestamps and defaults
- defaults: `status=TODO`, `priority=MEDIUM`, `plannedMinutes=30` if null/invalid
- `@PreUpdate` refreshes `updatedAt`

### `ProductivityGoal` (`productivity_goals`)

- `id`, `ownerId`, `title`, `description`, `targetDate`, `createdAt`, `updatedAt`
- relation to tasks is logical (`task.goalId`), not DB foreign key

### `TaskDependency` (`task_dependencies`)

- `id`, `ownerId`, `predecessorTaskId`, `successorTaskId`, `createdAt`
- unique edge constraint: `(owner_id, predecessor_task_id, successor_task_id)`

### `DecisionLogEntry` (`decision_log_entries`)

- `id`, `ownerId`, `taskId`, `decisionType`, `reason`, `createdAt`

### `TodoList` and `TodoItem`

- `TodoList`: `id`, `ownerId`, `name`, `createdAt`
- `TodoItem`: `id`, `ownerId`, `listId`, `title`, `done`, `positionIndex`, `dueAt`, `createdAt`, `updatedAt`
- `TodoItem @PrePersist`: `positionIndex=0` if null/negative

## 5) Repository capabilities

### `ProductivityTaskRepository`

- owner-scoped sorted fetches
- status-in queries
- goal-scoped list and count metrics
- completed-task list
- due range query
- `JpaSpecificationExecutor` for paged filtering

### `TaskDependencyRepository`

- owner graph fetch
- predecessor/successor edge lookups
- duplicate edge existence checks

### Todo repositories

- `TodoListRepository` and `TodoItemRepository` support specs/paging and counters

## 6) API contract (controller truth)

Base path: `/api/productivity`

### Tasks

- `GET /owners/{ownerId}/tasks`
- `GET /owners/{ownerId}/tasks/page` (`q,status,priority,dueFrom,dueTo,page,size`)
- `POST /owners/{ownerId}/tasks`
- `PUT /tasks/{taskId}`
- `POST /tasks/{taskId}/start`
- `POST /tasks/{taskId}/clear-goal`
- `POST /tasks/{taskId}/complete`
- `DELETE /tasks/{taskId}`
- `GET /owners/{ownerId}/calendar.ics`
- `GET /tasks/{taskId}/calendar.ics`

### Todo lists and items

- `GET /owners/{ownerId}/todo-lists`
- `GET /owners/{ownerId}/todo-lists/page`
- `POST /owners/{ownerId}/todo-lists`
- `PUT /todo-lists/{listId}`
- `DELETE /todo-lists/{listId}`
- `GET /todo-lists/{listId}/items`
- `GET /todo-lists/{listId}/items/page`
- `POST /todo-lists/{listId}/items`
- `PUT /todo-items/{itemId}`
- `POST /todo-items/{itemId}/toggle`
- `DELETE /todo-items/{itemId}`

### Goals

- `GET /owners/{ownerId}/goals`
- `POST /owners/{ownerId}/goals`
- `PUT /goals/{goalId}`
- `DELETE /goals/{goalId}`

### Dependencies

- `GET /owners/{ownerId}/dependencies`
- `POST /owners/{ownerId}/dependencies`
- `DELETE /dependencies/{dependencyId}`
- `GET /owners/{ownerId}/dependencies/order`

### Planning and intelligence

- `POST /owners/{ownerId}/adaptive-reschedule`
- `POST /owners/{ownerId}/conflicts/resolve`
- `GET /owners/{ownerId}/decisions`
- `POST /owners/{ownerId}/decisions`

### AI and analytics

- `POST /owners/{ownerId}/ai/decompose`
- `GET /owners/{ownerId}/ai/context-suggestions`
- `GET /owners/{ownerId}/insights`
- `GET /owners/{ownerId}/weekly-review`

### Progress

- `GET /owners/{ownerId}/progress`

## 7) DTO validation contracts

### Task DTOs

- `TaskCreateRequestDTO`
  - `title`: `@NotBlank`, `@Size(max=180)`
  - `description`: `@Size(max=4000)`
  - `plannedMinutes`: `@Min(5)`, `@Max(720)`
  - `actualMinutes`: `@Min(0)`, `@Max(1440)`
  - optional: `priority`, `goalId`, `dueAt`
- `TaskUpdateRequestDTO`
  - same numeric/text bounds
  - optional partial fields including `status`

### Goal / dependency / decision DTOs

- `GoalCreateRequestDTO`: `title` required max 180, `description` max 2000
- `DependencyCreateRequestDTO`: both IDs `@Min(1)`
- `DecisionLogCreateRequestDTO`
  - `decisionType`: `@NotBlank`, `@Size(max=80)`
  - `reason`: `@NotBlank`, `@Size(max=4000)`

### Todo DTOs

- `TodoListCreateRequestDTO`: `name` required max 120
- `TodoItemCreateRequestDTO`: `title` required max 250
- `TodoItemUpdateRequestDTO`: optional `title` max 250, `positionIndex` min 0

### AI / planning request DTOs

- `AiDecomposeRequestDTO`: `goalText` required max 2000, `maxSteps` in `[2..12]`
- `AdaptiveRescheduleRequestDTO`: optional `weekStart`, `dailyCapacityMinutes`

## 8) Service behavior details

### `TaskService`

- `getTasksByOwnerPaged` uses JPA `Specification` filters (`q`, status, priority, due range)
- `createTask` applies defaults for priority/planned minutes if omitted
- partial update semantics: only non-null request fields are applied
- important nuance: `goalId` is only updated when request value is non-null, so null-unassign is not possible via generic update
- explicit unassign endpoint exists: `POST /tasks/{taskId}/clear-goal`
- lifecycle helpers:
  - `startTask` forbids restarting `DONE`
  - `completeTask` marks `DONE` and sets `completedAt=now`
- ICS export delegates to `IcsUtils`

### `TodoService`

- owner list paging/filtering by list name
- item paging/filtering by title, done flag, due range
- `createItem` sets `ownerId` from parent list and appends by current list size
- list deletion currently deletes items manually then list

### `GoalService`

- computes per-goal totals with repository counts
- `weeklyVelocity = doneTasks / weeksSinceGoalCreation`
- update/delete are by `goalId` only

### `DependencyService`

- validates:
  - no self-dependency
  - both tasks belong to `ownerId`
  - no duplicate edge
  - no cycle on insertion (DFS check)
- provides topological order using Kahn algorithm
- throws `IllegalStateException` on cycle detection

### `PlanningService`

- `suggest(ownerId, limit)` scores active tasks (priority, due pressure, in-progress, quick-win)
- `buildDailyPlan(ownerId, focusMinutes)`:
  - clamps focus to `[30..720]`
  - optimizes over up to 20 candidates via 0/1 knapsack
  - falls back to best single task if no combination fits

### `AdaptivePlanningService`

- builds week plan from non-`DONE` tasks
- attempts dependency topological order, then appends unlisted active tasks
- fallback to raw active list if dependency order fails
- current sort uses `.reversed()` after due-date + priority comparator chain
  - effect: likely not intuitive earliest-due-first ordering
- allocates minutes day-by-day:
  - `remaining = max(15, planned - actual)`

### `DecisionIntelligenceService`

- resolves two tasks only (`firstTaskId`, `secondTaskId`)
- score factors:
  - priority weight
  - due pressure bucket
  - in-progress bonus
  - short-task bonus
- `ownerId` is not consumed in service signature today

### `DecisionLogService`

- persists typed decision events and returns owner-scoped history

### `CognitiveAssistService`

Modes:

- heuristic-only
- Ollama enabled with heuristic fallback

Goal decomposition:

- Ollama path expects strict JSON object (`rationale`, `steps`)
- heuristic path returns template-generated action steps
- source tags: `OLLAMA`, `HEURISTIC`, `HEURISTIC_FALLBACK`

Context suggestions:

- heuristic uses completion counts by weekday
- Ollama mode expects strict JSON array of suggestion objects

Robustness:

- extracts first JSON object from model text
- normalizes confidence to range `[0.0, 1.0]`

### `ProgressService`

- total, done, in-progress, blocked counts
- overdue open task count
- completion rate
- open planned-minute total

### `InsightsService`

- completion rate and estimation accuracy score
- best/worst completion hour from done tasks
- streak currently implemented as a capped proxy (`min(streakCounter, 30)`)
- weekly review prompts are static constants

## 9) Error mapping contract

Global handler: `ValidationExceptionHandler`

- `MethodArgumentNotValidException` -> `400`, includes `errors` map
- `ConstraintViolationException` -> `400`
- `IllegalArgumentException` -> `400`
- `IllegalStateException` -> `409`
- `Exception` -> `500`, message prefixed with `Unexpected error:`

Error payload shape:

- `timestamp`
- `status`
- `message`
- `errors` (field-level only for method argument validation)

## 10) Calendar export details

`IcsUtils.tasksToIcs(...)`:

- outputs one `VEVENT` per task with non-null `dueAt`
- `DTSTART = dueAt`
- `DTEND = dueAt + 30 minutes` (currently fixed duration)
- status mapping: `DONE -> CONFIRMED`, otherwise `TENTATIVE`
- escapes `\\`, `;`, `,`, and newline

## 11) Seed data behavior

`ProductivityDataInitializer` seeds data once when both conditions hold:

- `taskRepository.count() == 0`
- `listRepository.count() == 0`

Seed profile:

- owner `2`
- two sample tasks
- one todo list with two items

## 12) Frontend integration notes

Angular client uses this endpoint surface from `angular/src/app/services/productivity.service.ts`.

Observed coupling points:

- board flow uses conflict resolver output to mutate task statuses
- decision log records auto-actions (`AUTO_*` style)
- adaptive plan and AI endpoints are used in UX flows

## 13) Confirmed caveats and gaps

### Ownership enforcement gaps

Mutations by raw ID without owner assertion exist in current services, including:

- `TaskService.updateTask/deleteTask/startTask/completeTask/clearTaskGoal`
- `GoalService.update/delete`
- `DependencyService.removeDependency`
- todo list/item mutation paths in `TodoService`

Additional owner-context gaps:

- `DecisionIntelligenceService.resolve(...)` does not receive ownerId
- `CognitiveAssistService.decomposeGoal(...)` does not receive ownerId

### Referential integrity gaps

- no DB foreign keys between tasks/goals/dependencies/todo models
- deleting a goal does not clear task `goalId` references
- deleting a task does not explicitly remove dependency edges first

### Algorithm and behavior caveats

- adaptive comparator chain likely reverses intended due ordering
- streak metric is not true consecutive-day streak
- ICS event duration ignores `plannedMinutes`

### Documentation drift

`productivity-service/README.md` is outdated versus current controller contract (for example, includes planning endpoints that are not exposed, and misses several current endpoints).

### Test coverage status

- no `src/test` directory currently exists in the module

## 14) Prompt pack (copy/paste)

### Prompt A - contract and drift audit

```text
Use PRODUCTIVITY_SERVICE_DEEP_DIVE.md as the baseline, then inspect the code to verify endpoint and DTO drift.

Deliver:
1) endpoint matrix (method, path, request DTO, response DTO, status/error codes)
2) drift findings between controller, DTO validation, and README
3) concrete patch list with file-level changes
```

### Prompt B - security and ownership audit

```text
Review productivity-service for ownerId enforcement and cross-owner mutation risk.

Output by severity:
- file + method
- vulnerable behavior
- realistic exploit scenario
- minimal backward-compatible fix
- tests to add
```

### Prompt C - planning/algorithm quality pass

```text
Evaluate planning quality across PlanningService and AdaptivePlanningService.
Focus on comparator correctness, capacity distribution, and dependency-aware ordering.

Return:
1) identified logic defects
2) corrected pseudocode
3) targeted unit test matrix
```

### Prompt D - AI fallback hardening

```text
Inspect CognitiveAssistService for Ollama contract robustness and fallback quality.

Provide:
- parsing failure modes
- prompt improvements
- deterministic fallback enhancements
- test scaffolds for malformed and partial model outputs
```

## 15) Immediate engineering backlog

1. Add owner assertions to all mutating ID-based operations.
2. Propagate and enforce `ownerId` in conflict resolution and AI decomposition services.
3. Add cleanup rules for goal/task deletes to avoid dangling references.
4. Fix adaptive sort behavior to earliest due first, then priority descending.
5. Replace fixed ICS duration with task-aware duration (`plannedMinutes` default fallback).
6. Add initial test suite for dependency cycles, planning, conflict scoring, and Ollama fallback.
7. Reconcile `productivity-service/README.md` endpoint section with controller reality.

## 16) Quick file index

- `src/main/java/com/freelancing/productivity/controller/ProductivityController.java`
- `src/main/java/com/freelancing/productivity/service/TaskService.java`
- `src/main/java/com/freelancing/productivity/service/TodoService.java`
- `src/main/java/com/freelancing/productivity/service/GoalService.java`
- `src/main/java/com/freelancing/productivity/service/DependencyService.java`
- `src/main/java/com/freelancing/productivity/service/PlanningService.java`
- `src/main/java/com/freelancing/productivity/service/AdaptivePlanningService.java`
- `src/main/java/com/freelancing/productivity/service/DecisionIntelligenceService.java`
- `src/main/java/com/freelancing/productivity/service/CognitiveAssistService.java`
- `src/main/java/com/freelancing/productivity/service/InsightsService.java`
- `src/main/java/com/freelancing/productivity/service/IcsUtils.java`
- `src/main/java/com/freelancing/productivity/exception/ValidationExceptionHandler.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-mysql.yml`

