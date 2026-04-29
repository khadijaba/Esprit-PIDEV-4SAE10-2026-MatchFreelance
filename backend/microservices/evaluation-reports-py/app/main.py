"""API FastAPI — rapports PDF, graphiques et résumé exécutif (module Évaluation)."""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response, JSONResponse
from pydantic import BaseModel, Field, model_validator

from app.charts import evolution_png, histogram_png
from app.exam_auto_generate import build_examen_from_modules
from app.pdf_report import build_dashboard_pdf, build_executive_summary

PREFIX = "/api/evaluation-reports"

app = FastAPI(
    title="Evaluation Reports",
    description="Histogrammes, courbes d'évolution, PDF et executive summary",
    version="1.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class HistogramRequest(BaseModel):
    scores: list[float] = Field(..., min_length=1, description="Liste des notes")
    title: str = "Répartition des notes"


class EvolutionRequest(BaseModel):
    labels: list[str] = Field(..., min_length=1)
    values: list[float] = Field(..., min_length=1)
    title: str = "Évolution des résultats"

    @model_validator(mode="after")
    def same_length(self):
        if len(self.labels) != len(self.values):
            raise ValueError("labels et values doivent avoir la même longueur")
        return self


class PdfReportRequest(BaseModel):
    title: str = "Rapport d'évaluation"
    subtitle: str = "Synthèse statistique et tendances"
    scores: list[float] = Field(..., min_length=1)
    evolution_labels: list[str] = Field(..., min_length=1)
    evolution_values: list[float] = Field(..., min_length=1)

    @model_validator(mode="after")
    def evo_same_length(self):
        if len(self.evolution_labels) != len(self.evolution_values):
            raise ValueError("evolution_labels et evolution_values doivent avoir la même longueur")
        return self


class ExecutiveSummaryRequest(BaseModel):
    scores: list[float] = Field(..., min_length=1)
    evolution_values: list[float] = Field(..., min_length=1)


class AutoExamenGenerateRequest(BaseModel):
    """Payload envoyé par le microservice Evaluation (Java), clés JSON camelCase."""

    formationId: int
    titreFormation: str = ""
    modules: list[dict] = Field(..., min_length=1)
    suffixeTitre: str | None = None
    seuilReussi: int = Field(60, ge=0, le=100)
    useLlm: bool | None = None


@app.get("/health")
def health():
    return {"status": "UP", "service": "evaluation-reports-py"}


@app.get(f"{PREFIX}/health")
def health_prefixed():
    """Même réponse que /health — utile via Gateway : GET /api/evaluation-reports/health."""
    return {"status": "UP", "service": "evaluation-reports-py"}


@app.post(f"{PREFIX}/charts/histogram", response_class=Response)
def post_histogram(body: HistogramRequest):
    try:
        data = histogram_png(body.scores, title=body.title)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f'{type(e).__name__}: {e}') from e
    return Response(content=data, media_type="image/png")


@app.post(f"{PREFIX}/charts/evolution", response_class=Response)
def post_evolution(body: EvolutionRequest):
    try:
        data = evolution_png(body.labels, body.values, title=body.title)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f'{type(e).__name__}: {e}') from e
    return Response(content=data, media_type="image/png")


@app.post(f"{PREFIX}/reports/executive-summary")
def post_executive_summary(body: ExecutiveSummaryRequest):
    try:
        return build_executive_summary(body.scores, body.evolution_values)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f'{type(e).__name__}: {e}') from e


@app.post(f"{PREFIX}/exam/auto-generate")
def post_auto_generate_examen(body: AutoExamenGenerateRequest):
    """
    Squelette d'examen QCM (points / difficultés / lots parcours) pour création via Evaluation Java.
    """
    try:
        return build_examen_from_modules(
            formation_id=body.formationId,
            titre_formation=body.titreFormation or "",
            modules=body.modules,
            suffixe_titre=body.suffixeTitre,
            seuil_reussi=body.seuilReussi,
            use_llm=body.useLlm,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e


@app.post(f"{PREFIX}/reports/pdf")
def post_pdf(body: PdfReportRequest):
    try:
        pdf_bytes = build_dashboard_pdf(
            title=body.title,
            subtitle=body.subtitle,
            scores=body.scores,
            evolution_labels=body.evolution_labels,
            evolution_values=body.evolution_values,
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=f'{type(e).__name__}: {e}') from e
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": 'attachment; filename="rapport-evaluation.pdf"'},
    )


@app.exception_handler(ValueError)
async def value_error_handler(_, exc: ValueError):
    return JSONResponse(status_code=422, content={"detail": str(exc)})

