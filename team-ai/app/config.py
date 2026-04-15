"""
Configuration du service Team AI.
Variables d'environnement optionnelles : HOST, PORT, OPENAI_API_KEY.
"""
import os
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    host: str = "0.0.0.0"
    port: int = 5000
    # URL du backend (Gateway) pour récupérer skills et users (notification compatibilité)
    backend_url: str = "http://localhost:8086"
    # Pour extraction LLM (optionnel)
    openai_api_key: str = ""
    openai_base_url: str = ""  # ex: https://api.openai.com/v1 ou URL Ollama
    use_llm: bool = False

    class Config:
        env_prefix = "TEAM_AI_"
        env_file = ".env"
        extra = "ignore"


def get_settings() -> Settings:
    return Settings(
        backend_url=os.environ.get("TEAM_AI_BACKEND_URL", os.environ.get("BACKEND_URL", "http://localhost:8086")),
        openai_api_key=os.environ.get("OPENAI_API_KEY", ""),
        openai_base_url=os.environ.get("OPENAI_BASE_URL", ""),
        use_llm=os.environ.get("TEAM_AI_USE_LLM", "").lower() in ("1", "true", "yes"),
    )
