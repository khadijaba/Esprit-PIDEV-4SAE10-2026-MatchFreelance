"""
Badges & gamification : niveau (Débutant → Expert) et badges par freelancer.
Export JSON vers frontend/public/reports/badges.json pour affichage sur le profil.
"""
import json
from pathlib import Path
from typing import Any

import requests
from config import API_BASE_URL, API_TOKEN

REPORTS_DIR = Path(__file__).resolve().parent / "reports"
FRONTEND_REPORTS_DIR = Path(__file__).resolve().parent.parent / "frontend" / "public" / "reports"

# Niveaux selon score de complétion 0–100 %
LEVELS = [
    (0, 20, "Débutant"),
    (20, 40, "Intermédiaire"),
    (40, 70, "Avancé"),
    (70, 101, "Expert"),
]

# Badges : (clé, label, condition sur dict stats)
# stats: nb_certificats, nb_formations_validees, score_completion_pct, nb_skills
BADGE_RULES = [
    ("first_formation", "Première formation", lambda s: s["nb_certificats"] >= 1 or s["nb_formations_validees"] >= 1),
    ("parcours_starter", "Parcours démarré", lambda s: s["nb_certificats"] >= 1),
    ("certificats_3", "3 certificats", lambda s: s["nb_certificats"] >= 3),
    ("certificats_5", "5 certificats", lambda s: s["nb_certificats"] >= 5),
    ("expert", "Expert", lambda s: s["score_completion_pct"] >= 70),
    ("avance", "Avancé", lambda s: 40 <= s["score_completion_pct"] < 70),
]


def api_get(path: str) -> list[Any] | dict[str, Any] | None:
    url = f"{API_BASE_URL}{path}"
    headers = {}
    if API_TOKEN:
        headers["Authorization"] = f"Bearer {API_TOKEN}"
    try:
        r = requests.get(url, headers=headers, timeout=10)
        r.raise_for_status()
        return r.json()
    except Exception:
        return None


def score_completion_freelancer(
    inscriptions: list[dict],
    resultats: list[dict],
    certificats: list[dict],
) -> float:
    nb_cert = len(certificats)
    nb_formations_validees = len([i for i in inscriptions if i.get("statut") == "VALIDEE"])
    score_examens = 0.0
    if resultats:
        scores = [float(r.get("score") or 0) for r in resultats]
        score_examens = sum(scores) / len(scores)
    part_cert = min(1.0, nb_cert / 5.0) * 40
    part_form = min(1.0, nb_formations_validees / 3.0) * 30
    part_exam = min(100.0, score_examens) / 100.0 * 30
    return round(part_cert + part_form + part_exam, 1)


def get_level(score_pct: float) -> str:
    for low, high, label in LEVELS:
        if low <= score_pct < high:
            return label
    return "Débutant"


def run_badges() -> dict:
    """Calcule badges et niveau pour chaque freelancer."""
    users = api_get("/api/users")
    if not isinstance(users, list):
        users = []
    freelancers = [u for u in users if u.get("role") == "FREELANCER"]
    by_freelancer = {}
    labels_badges = {key: label for key, label, _ in BADGE_RULES}

    for f in freelancers:
        fid = f.get("id") or f.get("userId")
        if not fid:
            continue
        fid_str = str(fid)
        inscriptions = api_get(f"/api/inscriptions/freelancer/{fid}") or []
        resultats = api_get(f"/api/examens/resultats/freelancer/{fid}") or []
        certificats = api_get(f"/api/certificats/freelancer/{fid}") or []
        skills = api_get(f"/api/skills/freelancer/{fid}") or []

        score = score_completion_freelancer(inscriptions, resultats, certificats)
        level = get_level(score)
        stats = {
            "nb_certificats": len(certificats),
            "nb_formations_validees": len([i for i in inscriptions if i.get("statut") == "VALIDEE"]),
            "score_completion_pct": score,
            "nb_skills": len(skills),
        }
        badges = [key for key, _, cond in BADGE_RULES if cond(stats)]
        by_freelancer[fid_str] = {
            "level": level,
            "score_completion_pct": score,
            "badges": badges,
            "badge_labels": {key: labels_badges[key] for key in badges},
        }

    return {
        "by_freelancer": by_freelancer,
        "levels": [{"min": low, "max": high, "label": label} for low, high, label in LEVELS],
        "badge_definitions": [{"key": key, "label": label} for key, label, _ in BADGE_RULES],
    }


def save_to_frontend(result: dict) -> None:
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    path_local = REPORTS_DIR / "badges.json"
    with open(path_local, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    if FRONTEND_REPORTS_DIR.parent.exists():
        FRONTEND_REPORTS_DIR.mkdir(parents=True, exist_ok=True)
        path_front = FRONTEND_REPORTS_DIR / "badges.json"
        with open(path_front, "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        print(f"[Badges] Copié vers la plateforme : {path_front}")


if __name__ == "__main__":
    print("[Badges] Calcul des badges et niveaux…")
    result = run_badges()
    save_to_frontend(result)
    n = len(result["by_freelancer"])
    print(f"[Badges] {n} freelancer(s) traité(s). Exemple:", list(result["by_freelancer"].items())[:2])
