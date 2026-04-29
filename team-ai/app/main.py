"""
Point d'entrée du service Team AI.
Lancer avec : uvicorn app.main:app --host 0.0.0.0 --port 5000
"""
import logging
from fastapi import FastAPI

logging.basicConfig(level=logging.INFO)
logging.getLogger("app").setLevel(logging.INFO)
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.api.routes import router

settings = get_settings()
app = FastAPI(
    title="Team AI",
    description="Analyse de projets (NLP) et construction d'équipe optimale pour la plateforme freelancing",
    version="1.0.0",
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(router)


@app.get("/health")
def health():
    return {"status": "ok", "service": "team-ai"}
