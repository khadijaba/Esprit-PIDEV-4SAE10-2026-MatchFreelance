# Productivity Service

Personal Productivity & Task Management microservice for MatchFreelance.

## Scope
- Task planning (CRUD + lifecycle)
- To-do lists and to-do items (CRUD + toggle)
- Calendar integration via `.ics` export
- Progress tracking dashboard metrics
- Smart planning suggestions + daily plan builder

## Default runtime
- Port: `8087`
- Service name: `productivity-service`
- Database: H2 in-memory (`jdbc:h2:mem:productivity`)

## Main endpoints
- `GET /api/productivity/owners/{ownerId}/tasks`
- `POST /api/productivity/owners/{ownerId}/tasks`
- `PUT /api/productivity/tasks/{taskId}`
- `POST /api/productivity/tasks/{taskId}/start`
- `POST /api/productivity/tasks/{taskId}/complete`
- `GET /api/productivity/owners/{ownerId}/todo-lists`
- `POST /api/productivity/owners/{ownerId}/todo-lists`
- `POST /api/productivity/todo-lists/{listId}/items`
- `GET /api/productivity/owners/{ownerId}/progress`
- `GET /api/productivity/owners/{ownerId}/planning/suggestions`
- `GET /api/productivity/owners/{ownerId}/planning/daily`
- `GET /api/productivity/owners/{ownerId}/calendar.ics`

## Run
```powershell
cd productivity-service
mvn -q -DskipTests spring-boot:run
```

## Ollama (llama3.2) integration
The AI endpoints can use Ollama and automatically fallback to the built-in heuristic logic.

- Endpoints:
  - `POST /api/productivity/owners/{ownerId}/ai/decompose`
  - `GET /api/productivity/owners/{ownerId}/ai/context-suggestions`

### 1) Pull model
```powershell
ollama pull llama3.2
```

### 2) Enable Ollama for local run
```powershell
$env:PRODUCTIVITY_AI_OLLAMA_ENABLED = "true"
$env:PRODUCTIVITY_AI_OLLAMA_BASE_URL = "http://localhost:11434"
$env:PRODUCTIVITY_AI_OLLAMA_MODEL = "llama3.2"
$env:PRODUCTIVITY_AI_OLLAMA_FALLBACK_ENABLED = "true"

cd productivity-service
mvn -q -DskipTests spring-boot:run
```

If Ollama is unavailable, the service returns heuristic responses when fallback is enabled.

