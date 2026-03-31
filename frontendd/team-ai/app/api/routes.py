"""
Routes REST du service Team AI.
"""
from fastapi import APIRouter, HTTPException

from app.models.schemas import (
    AnalyzeRequest,
    AnalyzeResponse,
    BuildTeamRequest,
    BuildTeamResponse,
    ComputeCompatibleRequest,
    ComputeCompatibleResponse,
)
from app.services.analyzer import ProjectAnalyzer
from app.services.team_builder import TeamBuilder
from app.services.compatibility_notifier import CompatibilityNotifier
from app.config import get_settings

router = APIRouter(prefix="/api", tags=["team-ai"])
settings = get_settings()

analyzer = ProjectAnalyzer(
    use_llm=settings.use_llm,
    api_key=settings.openai_api_key,
    base_url=settings.openai_base_url or "https://api.openai.com/v1",
)
team_builder = TeamBuilder()
compatibility_notifier = CompatibilityNotifier(backend_url=settings.backend_url)


@router.post("/analyze-project")
def analyze_project(body: AnalyzeRequest):
    """
    Analyse la description d'un projet : rôles, compétences, budget, durée, complexité.
    Utilise des règles NLP par défaut ; option LLM si configuré.
    """
    try:
        result = analyzer.analyze(body)
        return result.model_dump(by_alias=True, exclude_none=False)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/build-team")
def build_team(body: BuildTeamRequest):
    """
    Construit une équipe optimale à partir de l'analyse du projet et de la liste des freelancers.
    Retourne les membres proposés, le leader technique et les notifications à envoyer.
    """
    try:
        result = team_builder.build(body)
        return result.model_dump(by_alias=True, exclude_none=False)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/compute-compatible-freelancers", response_model=ComputeCompatibleResponse)
def compute_compatible_freelancers(body: ComputeCompatibleRequest):
    """
    Notification automatique de compatibilité projet.
    Quand un client publie un projet, calcule pour chaque freelancer un score de compatibilité
    (compétences, expérience, note, projets réalisés). Retourne les freelancers avec score >= 70%
    pour envoi de notification (email ou in-app).
    """
    try:
        result = compatibility_notifier.compute(body, threshold=70)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
