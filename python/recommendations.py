"""
Scoring & Recommandations :
- Score de complétion 0–100 % par freelancer
- Recommandation de formations selon compétences et certificats
- Parcours avec prérequis en chaîne (formation C → cert B → formation B → cert A…)
Les résultats sont sauvegardés en JSON pour affichage sur la plateforme (frontend/public/reports/recommendations.json).
"""
import json
import sys
from pathlib import Path
from typing import Any

import requests

from config import API_BASE_URL, API_TOKEN

REPORTS_DIR = Path(__file__).resolve().parent / "reports"
FRONTEND_REPORTS_DIR = Path(__file__).resolve().parent.parent / "frontend" / "public" / "reports"


def api_get(path: str) -> list[Any] | dict[str, Any] | None:
    url = f"{API_BASE_URL}{path}"
    headers = {}
    if API_TOKEN:
        headers["Authorization"] = f"Bearer {API_TOKEN}"
    try:
        r = requests.get(url, headers=headers, timeout=10)
        r.raise_for_status()
        return r.json()
    except requests.RequestException as e:
        print(f"[Recommendations] Erreur API {path}: {e}", file=sys.stderr)
        return None


def fetch_formations() -> list[dict]:
    data = api_get("/api/formations")
    return data if isinstance(data, list) else []


def fetch_formation_ouverte_ids() -> set[int]:
    data = api_get("/api/formations/ouvertes")
    if not isinstance(data, list):
        return set()
    return {f.get("id") for f in data if f.get("id")}


def fetch_examens_formation(formation_id: int) -> list[dict]:
    data = api_get(f"/api/examens/formation/{formation_id}")
    return data if isinstance(data, list) else []


def score_completion_freelancer(
    freelancer_id: int,
    inscriptions: list[dict],
    resultats: list[dict],
    certificats: list[dict],
) -> float:
    """
    Score de complétion 0–100 % :
    - 40 % nombre de certificats (max 5 = 100 % de cette part)
    - 30 % inscriptions au statut VALIDEE (max 3 = 100 %) = inscriptions acceptées par l'admin
    - 30 % score moyen aux examens (normalisé 0–100)
    """
    nb_cert = len(certificats)
    # Formations validées = inscriptions dont l'admin a passé le statut à VALIDEE (acceptation)
    nb_formations_validees = len([i for i in inscriptions if i.get("statut") == "VALIDEE"])
    score_examens = 0.0
    if resultats:
        scores = [float(r.get("score") or 0) for r in resultats]
        score_examens = sum(scores) / len(scores)
    part_cert = min(1.0, nb_cert / 5.0) * 40
    part_form = min(1.0, nb_formations_validees / 3.0) * 30
    part_exam = min(100.0, score_examens) / 100.0 * 30
    return round(part_cert + part_form + part_exam, 1)


def get_scores_completion() -> list[dict]:
    """Retourne le score 0–100 % pour chaque freelancer (API User + Formation + Evaluation)."""
    users = api_get("/api/users")
    if not isinstance(users, list):
        return []
    freelancers = [u for u in users if u.get("role") == "FREELANCER"]
    out = []
    for f in freelancers:
        fid = f.get("id") or f.get("userId")
        if not fid:
            continue
        inscriptions = api_get(f"/api/inscriptions/freelancer/{fid}") or []
        resultats = api_get(f"/api/examens/resultats/freelancer/{fid}") or []
        certificats = api_get(f"/api/certificats/freelancer/{fid}") or []
        score = score_completion_freelancer(
            fid, inscriptions, resultats, certificats
        )
        out.append({
            "freelancer_id": fid,
            "email": f.get("email"),
            "fullName": f.get("fullName"),
            "score_completion_pct": score,
            "nb_certificats": len(certificats),
            "nb_formations_validees": len([i for i in inscriptions if i.get("statut") == "VALIDEE"]),
        })
    return sorted(out, key=lambda x: -x["score_completion_pct"])


def recommend_formations_for_freelancer(
    freelancer_id: int,
    formations_ouvertes_ids: set[int],
    certificats: list[dict],
    skills: list[dict],
    formations_all: list[dict],
) -> list[dict]:
    """
    Recommande des formations : ouvertes, pas déjà certifié sur cet examen,
    priorité si type_formation correspond à une compétence du freelancer.
    """
    examens_deja_certifies = {c.get("examenId") for c in certificats if c.get("examenId")}
    skill_categories = {s.get("category") or s.get("name") or "" for s in skills}
    recommended = []
    for formation in formations_all:
        fid = formation.get("id")
        if fid not in formations_ouvertes_ids:
            continue
        type_f = (formation.get("typeFormation") or "").upper().replace(" ", "_")
        examens = fetch_examens_formation(fid) if fid else []
        exam_ids = {e.get("id") for e in examens if e.get("id")}
        if examens_deja_certifies & exam_ids:
            continue  # déjà certifié pour au moins un examen de cette formation
        match_skill = 1 if type_f and any(type_f in (sc or "").upper() for sc in skill_categories) else 0
        recommended.append({
            "formation_id": fid,
            "titre": formation.get("titre"),
            "typeFormation": type_f,
            "match_competence": bool(match_skill),
        })
    recommended.sort(key=lambda x: (-x["match_competence"], x["titre"] or ""))
    return recommended[:10]


def parcours_prerequis_chaine(formations_all: list[dict]) -> dict[int, list[dict]]:
    """
    Pour chaque formation ayant un examenRequisId, retourne la chaîne des prérequis
    (formations à valider avant : cert A → formation B → cert B → formation C).
    Enrichit chaque étape avec formation_titre et examen_titre pour l'affichage.
    """
    formations_all = formations_all or []
    formation_titres = {f.get("id"): (f.get("titre") or "").strip() or None for f in formations_all if f.get("id")}
    formation_to_examen_requis = {}
    examen_to_formation = {}
    examen_titres = {}
    for f in formations_all:
        fid = f.get("id")
        ex_requis = f.get("examenRequisId")
        if ex_requis:
            formation_to_examen_requis[fid] = ex_requis
    for f in formations_all:
        fid = f.get("id")
        if not fid:
            continue
        examens = fetch_examens_formation(fid) or []
        for e in examens:
            eid = e.get("id")
            if eid:
                examen_to_formation[eid] = fid
                examen_titres[eid] = (e.get("titre") or "").strip() or None

    def chain(formation_id: int) -> list[dict]:
        seen = set()
        path = []
        current_fid = formation_id
        while current_fid:
            if current_fid in seen:
                break
            seen.add(current_fid)
            ex_requis = formation_to_examen_requis.get(current_fid)
            if not ex_requis:
                break
            formation_prereq = examen_to_formation.get(ex_requis)
            if not formation_prereq:
                path.append({
                    "type": "examen_requis",
                    "examen_id": ex_requis,
                    "examen_titre": examen_titres.get(ex_requis),
                })
                break
            path.append({
                "type": "formation",
                "formation_id": formation_prereq,
                "examen_requis_id": ex_requis,
                "formation_titre": formation_titres.get(formation_prereq),
                "examen_requis_titre": examen_titres.get(ex_requis),
            })
            current_fid = formation_prereq
        return path

    return {fid: chain(fid) for fid in formation_to_examen_requis}


def run_recommendations() -> dict:
    """Lance scoring, recommandations et parcours prérequis."""
    formations_all = fetch_formations()
    ouvertes_ids = fetch_formation_ouverte_ids()
    users = api_get("/api/users")
    if not isinstance(users, list):
        users = []
    freelancers = [u for u in users if u.get("role") == "FREELANCER"]

    scores = get_scores_completion()
    parcours = parcours_prerequis_chaine(formations_all)

    recommendations_by_freelancer = []
    for f in freelancers[:20]:  # limiter à 20 pour éviter trop d'appels API
        fid = f.get("id") or f.get("userId")
        if not fid:
            continue
        certs = api_get(f"/api/certificats/freelancer/{fid}") or []
        skills = api_get(f"/api/skills/freelancer/{fid}") or []
        recs = recommend_formations_for_freelancer(
            fid, ouvertes_ids, certs, skills, formations_all
        )
        recommendations_by_freelancer.append({
            "freelancer_id": fid,
            "email": f.get("email"),
            "recommendations": recs,
        })

    return {
        "scores_completion": scores,
        "recommendations_by_freelancer": recommendations_by_freelancer,
        "parcours_prerequis": {str(k): v for k, v in parcours.items()},
    }


def save_recommendations_to_frontend(result: dict) -> None:
    """Sauvegarde le résultat en JSON dans reports/ et copie vers frontend pour affichage sur la plateforme."""
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    path_local = REPORTS_DIR / "recommendations.json"
    with open(path_local, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    if FRONTEND_REPORTS_DIR.parent.exists():
        FRONTEND_REPORTS_DIR.mkdir(parents=True, exist_ok=True)
        path_front = FRONTEND_REPORTS_DIR / "recommendations.json"
        with open(path_front, "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        print(f"[Recommendations] Données copiées vers la plateforme : {path_front}")


if __name__ == "__main__":
    print("[Recommendations] Chargement…")
    result = run_recommendations()
    save_recommendations_to_frontend(result)
    print("[Recommendations] Scores (top 5):", result["scores_completion"][:5])
    print("[Recommendations] Parcours prérequis (formation_id → chaîne):", list(result["parcours_prerequis"].items())[:3])
    if result["recommendations_by_freelancer"]:
        r0 = result["recommendations_by_freelancer"][0]
        print("[Recommendations] Exemple recommandations pour", r0.get("email"), ":", r0.get("recommendations", [])[:3])
