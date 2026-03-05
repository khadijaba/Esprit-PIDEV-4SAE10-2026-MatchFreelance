#!/usr/bin/env bash
# Rappel automatique : export des rappels vers la plateforme + envoi des emails.
# À planifier en cron (ex. 0 9 * * * pour tous les jours à 9h).
cd "$(dirname "$0")"
python rappels.py --auto
exit $?
