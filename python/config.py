"""Configuration chargée depuis les variables d'environnement (.env)."""
import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()

# API Gateway (backend Spring Boot)
API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8050").rstrip("/")
API_TOKEN = os.getenv("API_TOKEN", "")

# SMTP (rappels par email)
SMTP_HOST = os.getenv("SMTP_HOST", "")
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USER = os.getenv("SMTP_USER", "")
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD", "")
SMTP_FROM = os.getenv("SMTP_FROM", "noreply@matchfreelance.com")

# Webhook (rappels en POST JSON)
WEBHOOK_URL = os.getenv("WEBHOOK_URL", "")

# ETL
ETL_DB_PATH = os.getenv("ETL_DB_PATH", str(Path(__file__).resolve().parent / "data" / "analytics.db"))

# Lien vers la plateforme (pour les emails)
APP_BASE_URL = os.getenv("APP_BASE_URL", "http://localhost:4200").rstrip("/")

# Envoyer un email même quand le freelancer n'a aucun rappel (true/false)
SEND_EMAIL_WHEN_NO_RAPPELS = os.getenv("SEND_EMAIL_WHEN_NO_RAPPELS", "false").strip().lower() in ("1", "true", "yes")

# Emails qui reçoivent toujours un résumé (ex: khadijab.benayed@esprit.tn), même sans rappel ou non inscrits en FREELANCER
RAPPELS_ALWAYS_SEND_TO = [e.strip() for e in os.getenv("RAPPELS_ALWAYS_SEND_TO", "").split(",") if e.strip()]

# Rappel « formation se termine dans X jours »
RAPPEL_FORMATION_JOURS_AVANT_FIN = int(os.getenv("RAPPEL_FORMATION_JOURS_AVANT_FIN", "7"))
# Rappel « examen non passé après X jours » (inscription validée)
RAPPEL_EXAMEN_JOURS_RETARD = int(os.getenv("RAPPEL_EXAMEN_JOURS_RETARD", "14"))
# Webhook au format Slack / Teams (envoi du message formaté pour Slack)
WEBHOOK_SLACK = os.getenv("WEBHOOK_SLACK", "").strip().lower() in ("1", "true", "yes")
