"""
Configuration du service Team AI.
Variables d'environnement optionnelles : TEAM_AI_*, OPENAI_API_KEY, etc.
"""
import os

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="TEAM_AI_",
        env_file=".env",
        extra="ignore",
    )

    host: str = "0.0.0.0"
    port: int = 5000
    backend_url: str = Field(default="http://localhost:8050", description="Gateway validation")
    openai_api_key: str = ""
    openai_base_url: str = ""
    use_llm: bool = False


def get_settings() -> Settings:
    return Settings(
        backend_url=os.environ.get("TEAM_AI_BACKEND_URL", os.environ.get("BACKEND_URL", "http://localhost:8050")),
        openai_api_key=os.environ.get("OPENAI_API_KEY", ""),
        openai_base_url=os.environ.get("OPENAI_BASE_URL", ""),
        use_llm=os.environ.get("TEAM_AI_USE_LLM", "").lower() in ("1", "true", "yes"),
    )
