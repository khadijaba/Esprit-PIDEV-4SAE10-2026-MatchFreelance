"""
Prévisionnel & ML :
- Prédiction de réussite à l'examen (probabilité par freelancer × examen)
- Détection de décrochage (inscription validée, pas de passage après X jours)
- Recommandation de projets pour les freelancers (match titre/description vs compétences)
Export JSON vers frontend/public/reports/ml_predictions.json
"""
import json
import re
from pathlib import Path
from typing import Any

from config import API_BASE_URL, API_TOKEN, JOURS_DECROCHAGE

REPORTS_DIR = Path(__file__).resolve().parent / "reports"
FRONTEND_REPORTS_DIR = Path(__file__).resolve().parent.parent / "frontend" / "public" / "reports"

SEUIL_REUSSITE_EXAMEN = 60


def api_get(path: str, timeout: int = 5) -> list[Any] | dict[str, Any] | None:
    url = f"{API_BASE_URL}{path}"
    headers = {}
    if API_TOKEN:
        headers["Authorization"] = f"Bearer {API_TOKEN}"
    try:
        import requests
        r = requests.get(url, headers=headers, timeout=timeout)
        r.raise_for_status()
        return r.json()
    except Exception:
        return None


def _extract_words(text: str) -> set[str]:
    """Extrait les mots de 3+ caractères (même regex que pour les projets)."""
    if not text:
        return set()
    low = text.lower().replace("_", " ").replace("-", " ")
    return set(re.findall(r"[a-zàâäéèêëïîôùûüç0-9]{3,}", low))


def run_dropout_detection(
    users: list[dict],
    inscriptions_by_freelancer: dict[int, list[dict]],
    resultats_all: list[dict],
    formations_all: list[dict],
) -> list[dict]:
    """Inscriptions validées sans passage après JOURS_DECROCHAGE jours."""
    from datetime import datetime, timezone
    formation_by_id = {f.get("id"): f for f in (formations_all or []) if f.get("id")}
    examens_by_formation: dict[int, list[int]] = {}
    for f in formations_all or []:
        fid = f.get("id")
        if not fid:
            continue
        exams = api_get(f"/api/examens/formation/{fid}") or []
        exam_ids = [e.get("id") for e in exams if e.get("id")]
        if exam_ids:
            examens_by_formation[fid] = exam_ids
    passages_set = set()
    for p in resultats_all or []:
        fid = p.get("freelancerId") or p.get("freelancer_id")
        eid = p.get("examenId") or p.get("examen_id")
        if fid is not None and eid is not None:
            passages_set.add((int(fid), int(eid)))
    now = datetime.now(timezone.utc)
    dropouts = []
    for u in users:
        if u.get("role") != "FREELANCER":
            continue
        fid = u.get("id") or u.get("userId")
        if not fid:
            continue
        fid = int(fid)
        inscriptions = inscriptions_by_freelancer.get(fid) or []
        for ins in inscriptions:
            if ins.get("statut") != "VALIDEE":
                continue
            formation_id = ins.get("formationId") or ins.get("formation_id")
            if not formation_id:
                continue
            formation_id = int(formation_id)
            exam_ids = examens_by_formation.get(formation_id) or []
            a_passe = any((fid, eid) in passages_set for eid in exam_ids)
            if a_passe:
                continue
            date_ins = ins.get("dateInscription") or ins.get("date_inscription")
            if not date_ins:
                continue
            try:
                if date_ins.endswith("Z"):
                    date_ins = date_ins[:-1] + "+00:00"
                dt = datetime.fromisoformat(date_ins.replace("Z", "+00:00"))
                delta = (now - dt).days
            except Exception:
                delta = 0
            if delta < JOURS_DECROCHAGE:
                continue
            formation = formation_by_id.get(formation_id) or {}
            risk = "high" if delta > 30 else "medium"
            dropouts.append({
                "freelancer_id": fid,
                "email": u.get("email"),
                "fullName": u.get("fullName"),
                "inscription_id": ins.get("id"),
                "formation_id": formation_id,
                "formation_titre": formation.get("titre"),
                "jours_depuis_inscription": delta,
                "risk": risk,
            })
    return dropouts


def run_project_recommendations(
    freelancers: list[dict],
    projects: list[dict],
) -> tuple[dict[str, list[dict]], str | None]:
    """Pour chaque freelancer, top 5 projets dont le titre/description matchent ses compétences + certificats.
    Utilise les projets OPEN en priorité ; s'il n'y en a pas, utilise tous les projets.
    Retourne (by_freelancer, note) pour affichage."""
    projects_list = projects if isinstance(projects, list) else []
    projects_open = [p for p in projects_list if p.get("status") == "OPEN" or p.get("status") == "Ouvert"]
    # S'il n'y a aucun projet OPEN, prendre tous les projets pour avoir des recommandations
    projects_to_use = projects_open if projects_open else projects_list
    note = None
    if not projects_list:
        note = "Aucun projet récupéré depuis l'API. Démarrez le microservice Project (port 8084) et la Gateway (8050), puis relancez le script."
    elif not projects_open and projects_list:
        note = "Aucun projet au statut OPEN : recommandations basées sur tous les projets."
    by_freelancer: dict[str, list[dict]] = {}
    for f in freelancers:
        fid = f.get("id") or f.get("userId")
        if not fid:
            continue
        fid_str = str(fid)
        skills = api_get(f"/api/skills/freelancer/{fid}") or []
        certs = api_get(f"/api/certificats/freelancer/{fid}") or []
        keywords = set()
        for s in skills:
            name = (s.get("name") or "").strip()
            cat = (s.get("category") or "").strip()
            keywords |= _extract_words(name)
            keywords |= _extract_words(cat)
        for c in certs:
            titre = (c.get("examenTitre") or c.get("titre") or "").strip()
            keywords |= _extract_words(titre)
        scored = []
        for p in projects_to_use:
            title = (p.get("title") or p.get("titre") or "")
            desc = (p.get("description") or "").lower()
            text = f"{(title or '').lower()} {desc}"
            words = _extract_words(text)
            match_count = len(keywords & words) if keywords else 0
            # Score 0–100 : au moins 1 mot en commun = 20%, +10% par mot (max 100)
            if match_count == 0:
                match_score = 0
            else:
                match_score = min(100, 20 + match_count * 10)
            scored.append({
                "project_id": p.get("id"),
                "title": p.get("title") or p.get("titre"),
                "match_score": match_score,
                "match_count": match_count,
            })
        scored.sort(key=lambda x: (-x["match_score"], -(x["match_count"]), x["title"] or ""))
        top5 = scored[:5]
        # Si tous les scores sont 0 (pas de skills ou pas de match), on met un ordre de pertinence 50, 40, 30, 20, 10
        if top5 and all(x["match_score"] == 0 for x in top5):
            for i, item in enumerate(top5):
                item["match_score"] = max(0, 50 - i * 10)
        # Ne garder que les projets avec un score > 0
        by_freelancer[fid_str] = [x for x in top5 if (x.get("match_score") or 0) > 0]
    return by_freelancer, note


def run_formation_recommendations(freelancers: list[dict]) -> dict[str, list[dict]]:
    """Pour chaque freelancer, récupère les formations recommandées (gaps de compétences) via l'API Formation."""
    by_freelancer: dict[str, list[dict]] = {}
    for f in freelancers:
        fid = f.get("id") or f.get("userId")
        if not fid:
            continue
        fid_str = str(fid)
        formations = api_get(f"/api/formations/recommandations/freelancer/{fid}") or []
        if not isinstance(formations, list):
            formations = []
        # Garder uniquement les champs utiles pour l'affichage
        by_freelancer[fid_str] = [
            {
                "id": form.get("id"),
                "titre": form.get("titre"),
                "typeFormation": form.get("typeFormation"),
                "niveau": form.get("niveau"),
                "statut": form.get("statut"),
                "dateDebut": form.get("dateDebut"),
                "dateFin": form.get("dateFin"),
            }
            for form in formations
        ]
    return by_freelancer


def run_exam_success_prediction(
    users: list[dict],
    resultats_all: list[dict],
    inscriptions_by_freelancer: dict[int, list[dict]],
    certificats_by_freelancer: dict[int, list[dict]],
    skills_by_freelancer: dict[int, list[dict]],
    formations_all: list[dict],
) -> list[dict]:
    """
    Prédiction de réussite (score >= SEUIL) pour les examens pas encore passés.
    Modèle simple : régression logistique sur (nb_certificats, nb_skills, nb_formations_validees).
    """
    try:
        from sklearn.linear_model import LogisticRegression
        import numpy as np
    except ImportError:
        return []
    # Dataset : un enregistrement par passage (resultat)
    X, y = [], []
    for r in resultats_all or []:
        fid = r.get("freelancerId") or r.get("freelancer_id")
        if not fid:
            continue
        fid = int(fid)
        score = float(r.get("score") or 0)
        nb_cert = len(certificats_by_freelancer.get(fid) or [])
        nb_skills = len(skills_by_freelancer.get(fid) or [])
        inscriptions = inscriptions_by_freelancer.get(fid) or []
        nb_form_val = len([i for i in inscriptions if i.get("statut") == "VALIDEE"])
        X.append([nb_cert, nb_skills, nb_form_val])
        y.append(1 if score >= SEUIL_REUSSITE_EXAMEN else 0)
    if len(X) < 10:
        return []
    X_arr = np.array(X)
    y_arr = np.array(y)
    model = LogisticRegression(max_iter=500, random_state=42)
    model.fit(X_arr, y_arr)
    # Prédire pour (freelancer, examen) pas encore passés
    examens_by_formation: dict[int, list[dict]] = {}
    for f in formations_all or []:
        fid = f.get("id")
        if not fid:
            continue
        exams = api_get(f"/api/examens/formation/{fid}") or []
        examens_by_formation[fid] = [e for e in exams if e.get("id")]
    passages_set = set()
    for r in resultats_all or []:
        fid = r.get("freelancerId") or r.get("freelancer_id")
        eid = r.get("examenId") or r.get("examen_id")
        if fid and eid:
            passages_set.add((int(fid), int(eid)))
    predictions = []
    freelancers = [u for u in users if u.get("role") == "FREELANCER"]
    for u in freelancers:
        fid = u.get("id") or u.get("userId")
        if not fid:
            continue
        fid = int(fid)
        inscriptions = inscriptions_by_freelancer.get(fid) or []
        for ins in inscriptions:
            if ins.get("statut") != "VALIDEE":
                continue
            formation_id = ins.get("formationId") or ins.get("formation_id")
            if not formation_id:
                continue
            formation_id = int(formation_id)
            exams = examens_by_formation.get(formation_id) or []
            for ex in exams:
                eid = ex.get("id")
                if not eid or (fid, eid) in passages_set:
                    continue
                nb_cert = len(certificats_by_freelancer.get(fid) or [])
                nb_skills = len(skills_by_freelancer.get(fid) or [])
                nb_form_val = len([i for i in inscriptions if i.get("statut") == "VALIDEE"])
                proba = float(model.predict_proba(np.array([[nb_cert, nb_skills, nb_form_val]]))[0][1])
                formation = next((x for x in (formations_all or []) if x.get("id") == formation_id), {})
                predictions.append({
                    "freelancer_id": fid,
                    "email": u.get("email"),
                    "fullName": u.get("fullName"),
                    "examen_id": eid,
                    "examen_titre": ex.get("titre"),
                    "formation_id": formation_id,
                    "formation_titre": formation.get("titre"),
                    "proba_reussite": round(proba * 100, 1),
                })
    return predictions


def run_ml_predictions() -> dict:
    print("[ML] Récupération des utilisateurs…")
    users = api_get("/api/users")
    if not isinstance(users, list):
        users = []
    freelancers = [u for u in users if u.get("role") == "FREELANCER"]
    print(f"[ML] {len(users)} utilisateur(s), {len(freelancers)} freelancer(s)")

    print("[ML] Récupération des formations…")
    formations_all = api_get("/api/formations") or []
    if not isinstance(formations_all, list):
        formations_all = []

    print("[ML] Récupération des projets…")
    projects = api_get("/api/projects") or []
    if not isinstance(projects, list):
        projects = []
    if not projects and (API_BASE_URL or "").strip().endswith("8050"):
        try:
            import requests
            r = requests.get("http://localhost:8084/projects", timeout=5)
            if r.ok:
                data = r.json()
                if isinstance(data, list):
                    projects = data
        except Exception:
            pass
    print(f"[ML] {len(projects)} projet(s)")

    inscriptions_by_freelancer: dict[int, list[dict]] = {}
    resultats_all: list[dict] = []
    certificats_by_freelancer: dict[int, list[dict]] = {}
    skills_by_freelancer: dict[int, list[dict]] = {}
    for i, f in enumerate(freelancers):
        fid = f.get("id") or f.get("userId")
        if not fid:
            continue
        fid = int(fid)
        print(f"[ML] Freelancer {fid} ({i+1}/{len(freelancers)}) : inscriptions, résultats, certificats, skills…")
        inscriptions_by_freelancer[fid] = api_get(f"/api/inscriptions/freelancer/{fid}") or []
        res = api_get(f"/api/examens/resultats/freelancer/{fid}") or []
        if isinstance(res, list):
            for r in res:
                r["freelancer_id"] = fid
                resultats_all.append(r)
        certificats_by_freelancer[fid] = api_get(f"/api/certificats/freelancer/{fid}") or []
        skills_by_freelancer[fid] = api_get(f"/api/skills/freelancer/{fid}") or []

    print("[ML] Détection décrochage…")
    dropouts = run_dropout_detection(
        users, inscriptions_by_freelancer, resultats_all, formations_all,
    )
    print("[ML] Recommandations de projets par freelancer…")
    project_recs, project_note = run_project_recommendations(freelancers, projects)
    print("[ML] Formations recommandées par freelancer (API Formation)…")
    formation_recs = run_formation_recommendations(freelancers)
    print("[ML] Prédictions réussite examen…")
    exam_predictions = run_exam_success_prediction(
        users, resultats_all, inscriptions_by_freelancer,
        certificats_by_freelancer, skills_by_freelancer, formations_all
    )

    return {
        "dropouts": dropouts,
        "project_recommendations_by_freelancer": project_recs,
        "formation_recommendations_by_freelancer": formation_recs,
        "exam_success_predictions": exam_predictions,
        "meta": {
            "jours_decrochage": JOURS_DECROCHAGE,
            "seuil_reussite_examen": SEUIL_REUSSITE_EXAMEN,
            "project_recommendation_note": project_note,
            "projects_count": len(projects) if isinstance(projects, list) else 0,
        },
    }


def save_to_frontend(result: dict) -> None:
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    path_local = REPORTS_DIR / "ml_predictions.json"
    with open(path_local, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    if FRONTEND_REPORTS_DIR.parent.exists():
        FRONTEND_REPORTS_DIR.mkdir(parents=True, exist_ok=True)
        path_front = FRONTEND_REPORTS_DIR / "ml_predictions.json"
        with open(path_front, "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        print(f"[ML] Copié vers la plateforme : {path_front}")


if __name__ == "__main__":
    print("[ML] Démarrage (Gateway attendue sur", API_BASE_URL, ")…")
    try:
        result = run_ml_predictions()
        save_to_frontend(result)
        n_projects = result.get("meta", {}).get("projects_count", 0)
        n_recos = sum(len(v) for v in result.get("project_recommendations_by_freelancer", {}).values())
        n_form = sum(len(v) for v in result.get("formation_recommendations_by_freelancer", {}).values())
        print(f"[ML] Terminé. Décrochages: {len(result['dropouts'])}, Prédictions examen: {len(result['exam_success_predictions'])}, Recos projets: {n_recos}, Recos formations: {n_form}")
    except Exception as e:
        print("[ML] Erreur:", e)
        raise
