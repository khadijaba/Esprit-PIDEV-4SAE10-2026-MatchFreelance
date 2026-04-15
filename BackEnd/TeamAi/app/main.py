"""
Team IA — FastAPI sur le port 5000.

Démarrage (depuis le dossier TeamAi/) :
  python -m pip install -r requirements.txt
  uvicorn app.main:app --host 0.0.0.0 --port 5000

Le proxy Angular réécrit /api/ai -> /api ; la Gateway réécrit /api/ai/* -> ce service /api/*.

Variables d'environnement :
  GATEWAY_URL              URL de l'API Gateway (défaut http://127.0.0.1:8086).
  SCHEDULE_GAP_WATCH       Seuil d'écart temps-avancement pour niveau WATCH (défaut 0.10).
  SCHEDULE_GAP_AT_RISK     Seuil pour AT_RISK (défaut 0.18).
  SCHEDULE_MIN_TIME_ALERT  Part minimale de temps écoulé pour alerter WATCH (défaut 0.22).
  OPENAI_API_KEY         (optionnel) OpenAI si LLM_PROVIDER=openai ou en secours (auto).
  OPENAI_MODEL           Modèle OpenAI (défaut gpt-4o-mini).
  OLLAMA_BASE_URL        API Ollama (défaut http://127.0.0.1:11434).
  OLLAMA_MODEL           Modèle Ollama (défaut llama3.2) — ex. ollama pull llama3.2
  LLM_PROVIDER           auto (Ollama puis OpenAI) | ollama | openai
"""
from __future__ import annotations

import logging
from typing import Any

from fastapi import FastAPI, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from starlette.requests import Request

from app.analyze import analyze_project
from app.description_coach import coach_description
from app.matching import compute_compatible
from app.planning_assistant import adjust_plan as planning_adjust_plan
from app.planning_assistant import generate_initial_plan
from app.schedule_risk import run_assessment as run_schedule_overrun_assessment

app = FastAPI(title="Team IA", version="1.0.0")
_log = logging.getLogger("uvicorn.error")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def _log_sor_requests(request: Request, call_next):
    if request.url.path.rstrip("/") in ("/api/sor", "/api/schedule-overrun-assessment") and request.method == "POST":
        _log.warning("SOR POST reçu — path brut=%s", request.url.path)
    return await call_next(request)


@app.on_event("startup")
async def _startup_log_routes() -> None:
    """Aide au diagnostic si une vieille instance uvicorn tourne encore (404 sur un endpoint pourtant présent dans le code)."""
    rows = []
    for r in app.routes:
        p = getattr(r, "path", None)
        m = getattr(r, "methods", None)
        if p and m:
            rows.append(f"{sorted(m)} {p}")
    _log.info("Team IA démarré — %s routes enregistrées. Détail: %s", len(rows), sorted(rows))


class AnalyzeProjectBody(BaseModel):
    title: str = Field(..., min_length=1)
    description: str = Field(..., min_length=1)


class BuildTeamBody(BaseModel):
    projectId: int
    projectTitle: str
    projectAnalysis: dict[str, Any]
    freelancers: list[dict[str, Any]]
    maxTeamSize: int | None = None


class ComputeCompatibleBody(BaseModel):
    projectId: int
    projectTitle: str
    requiredSkills: list[str]


class ScheduleOverrunBody(BaseModel):
    """Si project, phases et deliverables sont fournis, l'évaluation est locale ; sinon appel gateway."""

    projectId: int
    project: dict[str, Any] | None = None
    phases: list[dict[str, Any]] | None = None
    deliverables: list[dict[str, Any]] | None = None


class DescriptionCoachBody(BaseModel):
    """Suggestions de reformulation : périmètre, livrables, critères d'acceptation."""

    title: str = Field(..., min_length=1)
    description: str = Field(..., min_length=1)
    useLlm: bool = False


class PlanningInitialBody(BaseModel):
    projectTitle: str = Field(..., min_length=1)
    projectDescription: str = Field(..., min_length=1)
    durationDays: int = Field(default=30, ge=1, le=365)
    startDate: str | None = None
    requiredSkills: list[str] = Field(default_factory=list)
    useLlm: bool = False


class PlanningAdjustBody(BaseModel):
    currentPhases: list[dict[str, Any]] = Field(default_factory=list)
    scheduleAssessment: dict[str, Any] | None = None
    useLlm: bool = False


@app.get("/api/health")
def health():
    return {"status": "ok", "service": "team-ai"}


@app.get("/api/debug/team-ai-routes")
def debug_team_ai_routes():
    """Ouvrir dans le navigateur : liste les routes réellement chargées dans CE processus."""
    out: list[dict[str, Any]] = []
    for r in app.routes:
        p = getattr(r, "path", None)
        m = getattr(r, "methods", None)
        if p and m:
            out.append({"path": p, "methods": sorted(m)})
    return {"count": len(out), "routes": sorted(out, key=lambda x: (x["path"], x["methods"]))}


@app.get("/api/sor")
def schedule_overrun_get_probe():
    """Sonde GET : si cette URL répond 200 mais POST /api/sor reste en 404, le problème vient d’ailleurs (proxy, autre processus)."""
    return {
        "hint": "L’évaluation planning est en POST avec JSON {\"projectId\": <id>}.",
        "postUrl": "/api/sor",
    }


async def _schedule_overrun_core(
    body: ScheduleOverrunBody,
    authorization: str | None,
):
    return await run_schedule_overrun_assessment(
        body.projectId,
        authorization,
        body.project,
        body.phases,
        body.deliverables,
    )


@app.post("/api/sor")
@app.post("/api/sor/")
async def post_schedule_overrun_sor(
    body: ScheduleOverrunBody,
    authorization: str | None = Header(default=None),
):
    return await _schedule_overrun_core(body, authorization)


@app.post("/api/schedule-overrun-assessment")
async def post_schedule_overrun_long(
    body: ScheduleOverrunBody,
    authorization: str | None = Header(default=None),
):
    return await _schedule_overrun_core(body, authorization)


@app.post("/schedule-overrun-assessment")
async def post_schedule_overrun_no_api_prefix(
    body: ScheduleOverrunBody,
    authorization: str | None = Header(default=None),
):
    return await _schedule_overrun_core(body, authorization)


@app.post("/api/analyze-project")
def analyze_project_endpoint(body: AnalyzeProjectBody):
    return analyze_project(body.title.strip(), body.description.strip())


@app.post("/api/analyze-description")
@app.post("/api/description-coach")
@app.post("/api/dc")
@app.post("/description-coach")
def description_coach_endpoint(body: DescriptionCoachBody):
    """Reformulation / checklist / brouillon ; LLM optionnel (OPENAI_API_KEY + useLlm=true).

    Utiliser /api/analyze-description côté client (même motif que /api/analyze-project) si un proxy renvoie 404 sur description-coach.
    """
    return coach_description(body.title.strip(), body.description.strip(), body.useLlm)


@app.post("/api/planning-assistant/initial-plan")
@app.post("/api/planning-initial-plan")
def planning_initial_plan_endpoint(body: PlanningInitialBody):
    return generate_initial_plan(
        project_title=body.projectTitle.strip(),
        project_description=body.projectDescription.strip(),
        duration_days=body.durationDays,
        start_date_iso=body.startDate,
        required_skills=body.requiredSkills,
        use_llm=body.useLlm,
    )


@app.post("/api/planning-assistant/adjust-plan")
@app.post("/api/planning-adjust-plan")
def planning_adjust_plan_endpoint(body: PlanningAdjustBody):
    return planning_adjust_plan(
        current_phases=body.currentPhases,
        schedule_assessment=body.scheduleAssessment,
        use_llm=body.useLlm,
    )


@app.post("/api/build-team")
def build_team(body: BuildTeamBody):
    """Construction d'équipe : réponse minimale (logique avancée possible plus tard)."""
    max_n = body.maxTeamSize or 5
    team: list[dict[str, Any]] = []
    for i, f in enumerate(body.freelancers[:max_n]):
        fid = f.get("freelancerId")
        if fid is None:
            continue
        team.append(
            {
                "freelancerId": int(fid),
                "role": (body.projectAnalysis.get("roles") or ["Membre"])[0]
                if i == 0
                else "Contributeur",
                "isLeader": i == 0,
                "score": float(f.get("rating") or 70) - i,
                "matchRationale": "Sélection indicative Team IA (à enrichir).",
            }
        )
    return {
        "team": team,
        "technicalLeaderId": team[0]["freelancerId"] if team else None,
        "notificationsToSend": [],
        "rationale": "Équipe proposée de façon heuristique ; branchez un moteur d'optimisation pour la production.",
    }


@app.post("/api/compute-compatible-freelancers")
async def compute_compatible_endpoint(
    body: ComputeCompatibleBody,
    authorization: str | None = Header(default=None),
):
    return await compute_compatible(
        body.projectId,
        body.projectTitle,
        body.requiredSkills,
        authorization,
    )
