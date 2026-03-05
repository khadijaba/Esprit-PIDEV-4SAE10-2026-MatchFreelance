@echo off
REM Rappel automatique : export des rappels vers la plateforme + envoi des emails.
REM À planifier dans le Planificateur de tâches Windows (ex. tous les jours à 9h).
cd /d "%~dp0"
python rappels.py --auto
if errorlevel 1 (
  echo [Rappels] Erreur. Verifiez la Gateway, les microservices et le fichier .env (SMTP).
  exit /b 1
)
