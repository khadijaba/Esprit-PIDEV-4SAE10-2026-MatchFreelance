"""
Planning assistant (v1): génération de phases initiales + réajustement en cas de retard.

Mode LLM optionnel (Ollama/OpenAI) avec fallback heuristique.
"""
from __future__ import annotations

import json
import os
import re
from datetime import datetime, timedelta, timezone
from typing import Any

import httpx

METHOD_VERSION = "planning-assistant-v1"

_LLM_SYSTEM = (
    "Tu es un PMO technique. "
    "Tu dois proposer un planning projet structuré en JSON valide uniquement, sans markdown. "
    "Le JSON doit contenir phases[] avec phaseOrder, name, plannedDays, milestones[], "
    "deliverables[{title,type}], acceptanceCriteria[]. "
    "Reste réaliste, orienté exécution, sans inventer des dépendances non mentionnées."
)


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _parse_dt(v: Any) -> datetime | None:
    if not v:
        return None
    try:
        s = str(v).strip()
        if s.endswith("Z"):
            s = s[:-1] + "+00:00"
        dt = datetime.fromisoformat(s)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc)
    except Exception:
        return None


def _extract_json(text: str) -> dict[str, Any] | None:
    raw = text.strip()
    if raw.startswith("{") and raw.endswith("}"):
        try:
            data = json.loads(raw)
            return data if isinstance(data, dict) else None
        except Exception:
            return None
    m = re.search(r"\{[\s\S]*\}", raw)
    if not m:
        return None
    try:
        data = json.loads(m.group(0))
        return data if isinstance(data, dict) else None
    except Exception:
        return None


def _complexity_from_text(description: str) -> str:
    t = description.lower()
    heavy = sum(
        1
        for k in (
            "microservice",
            "migration",
            "kubernetes",
            "temps réel",
            "legacy",
            "refonte",
            "erp",
        )
        if k in t
    )
    wc = len(description.split())
    if wc < 60 and heavy == 0:
        return "simple"
    if wc < 180 and heavy < 2:
        return "medium"
    return "complex"


def _phase_template(complexity: str) -> list[tuple[str, float]]:
    if complexity == "simple":
        return [
            ("Cadrage & conception", 0.10),
            ("Réalisation principale", 0.55),
            ("Intégration & tests", 0.20),
            ("Recette & livraison", 0.15),
        ]
    if complexity == "complex":
        return [
            ("Cadrage & architecture", 0.20),
            ("Build incrémental", 0.40),
            ("Intégration & qualité", 0.25),
            ("Recette, transfert & go-live", 0.15),
        ]
    return [
        ("Cadrage & conception", 0.15),
        ("Build / développement", 0.45),
        ("Intégration & tests", 0.25),
        ("Recette & livraison", 0.15),
    ]


def _heuristic_initial_plan(
    project_title: str,
    project_description: str,
    duration_days: int,
    start_date: datetime | None,
    required_skills: list[str],
) -> dict[str, Any]:
    complexity = _complexity_from_text(project_description)
    duration = max(7, min(365, int(duration_days or 30)))
    start = start_date or _utc_now()
    templ = _phase_template(complexity)
    phases: list[dict[str, Any]] = []
    cursor = start

    for idx, (name, ratio) in enumerate(templ, start=1):
        pdays = max(2, int(round(duration * ratio)))
        # Corrige le cumul en laissant la dernière phase absorber l'écart.
        if idx == len(templ):
            used = sum(p["plannedDays"] for p in phases)
            pdays = max(2, duration - used)
        due = cursor + timedelta(days=pdays)
        phases.append(
            {
                "phaseOrder": idx,
                "name": name,
                "plannedDays": pdays,
                "startDate": cursor.isoformat(),
                "dueDate": due.isoformat(),
                "milestones": [
                    f"Jalon {idx}.1 validé ({name})",
                    f"Jalon {idx}.2 prêt pour étape suivante",
                ],
                "deliverables": [
                    {"title": f"{name} — livrable principal", "type": "DOC"},
                    {"title": f"{name} — preuve de validation", "type": "REPORT"},
                ],
                "acceptanceCriteria": [
                    "Livrables déposés et relus par le PO.",
                    "Aucun bloquant ouvert en fin de phase.",
                    "Validation explicite du passage à la phase suivante.",
                ],
            }
        )
        cursor = due

    if required_skills:
        phases[1]["milestones"].append(f"Compétences clés couvertes: {', '.join(required_skills[:5])}.")

    return {
        "methodVersion": METHOD_VERSION,
        "planKind": "INITIAL",
        "llmUsed": False,
        "llmBackend": None,
        "complexity": complexity,
        "summary": f"Plan initial généré en {len(phases)} phases (~{duration} jours).",
        "phases": phases,
    }


def _heuristic_adjust_plan(
    current_phases: list[dict[str, Any]],
    schedule_assessment: dict[str, Any] | None,
) -> dict[str, Any]:
    phases = [dict(p) for p in current_phases]
    level = str((schedule_assessment or {}).get("scheduleRiskLevel") or "OK").upper()
    overdue = int(((schedule_assessment or {}).get("metrics") or {}).get("overduePhasesCount") or 0)
    gap = float(((schedule_assessment or {}).get("metrics") or {}).get("gapProgressVsTime") or 0.0)
    shift_days = 0
    actions: list[str] = []

    if level == "AT_RISK":
        shift_days = max(3, overdue * 3, int(round(max(0.0, gap) * 10)))
        actions.extend(
            [
                f"Décaler les phases restantes de +{shift_days} jours.",
                "Introduire un jalon intermédiaire de revue au milieu de la phase en cours.",
                "Réduire le scope non critique (MVP) pour protéger la date de livraison.",
            ]
        )
    elif level == "WATCH":
        shift_days = max(1, int(round(max(0.0, gap) * 6)))
        actions.extend(
            [
                f"Replanifier légèrement les prochaines phases (+{shift_days} jours).",
                "Ajouter une revue hebdomadaire PO + équipe.",
            ]
        )
    else:
        actions.append("Pas de réajustement majeur recommandé.")

    if shift_days > 0:
        for p in phases:
            s = _parse_dt(p.get("startDate"))
            d = _parse_dt(p.get("dueDate"))
            if s:
                p["startDate"] = (s + timedelta(days=shift_days)).isoformat()
            if d:
                p["dueDate"] = (d + timedelta(days=shift_days)).isoformat()
            crit = list(p.get("acceptanceCriteria") or [])
            crit.append("Checkpoint d'avancement hebdomadaire ajouté suite au risque planning.")
            p["acceptanceCriteria"] = crit

    return {
        "methodVersion": METHOD_VERSION,
        "planKind": "ADJUSTED",
        "llmUsed": False,
        "llmBackend": None,
        "summary": f"Réajustement {level}: décalage {shift_days} jour(s)." if shift_days else "Planning conservé.",
        "adjustmentReason": {
            "scheduleRiskLevel": level,
            "overduePhasesCount": overdue,
            "gapProgressVsTime": round(gap, 4),
        },
        "recommendedActions": actions,
        "phases": phases,
    }


def _llm_prompt_initial(
    project_title: str,
    project_description: str,
    duration_days: int,
    start_date_iso: str | None,
    required_skills: list[str],
) -> str:
    return (
        "Génère un plan initial projet sous forme JSON strict.\n"
        f"Titre: {project_title}\n"
        f"Description: {project_description}\n"
        f"Durée cible (jours calendaires): {duration_days}\n"
        f"Date de démarrage souhaitée: {start_date_iso or 'non fournie'}\n"
        f"Compétences: {', '.join(required_skills) if required_skills else 'non précisées'}\n"
        "Format JSON attendu: {\"summary\":...,\"complexity\":...,\"phases\":[...]}\n"
        "Chaque phase: phaseOrder(int), name(str), plannedDays(int), milestones(str[]), "
        "deliverables([{title,type}]), acceptanceCriteria(str[])."
    )


def _llm_prompt_adjust(
    current_phases: list[dict[str, Any]],
    schedule_assessment: dict[str, Any] | None,
) -> str:
    return (
        "Réajuste le planning en JSON strict selon le risque fourni.\n"
        f"Phases actuelles: {json.dumps(current_phases, ensure_ascii=False)}\n"
        f"Évaluation planning: {json.dumps(schedule_assessment or {}, ensure_ascii=False)}\n"
        "Format JSON attendu: {\"summary\":...,\"recommendedActions\":[...],\"phases\":[...]}\n"
        "Conserver phaseOrder, proposer startDate/dueDate si possible, ajouter critères de pilotage."
    )


def _maybe_ollama_json(prompt: str) -> tuple[dict[str, Any] | None, bool]:
    base = os.environ.get("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
    model = os.environ.get("OLLAMA_MODEL", "llama3.2").strip()
    if not model:
        return None, False
    payload = {
        "model": model,
        "messages": [{"role": "system", "content": _LLM_SYSTEM}, {"role": "user", "content": prompt}],
        "stream": False,
        "options": {"temperature": 0.25},
    }
    try:
        with httpx.Client(timeout=httpx.Timeout(120.0, connect=5.0)) as client:
            r = client.post(f"{base}/api/chat", json=payload)
            r.raise_for_status()
            data = r.json()
            msg = data.get("message") if isinstance(data, dict) else None
            text = msg.get("content") if isinstance(msg, dict) else None
            if isinstance(text, str):
                js = _extract_json(text)
                if js:
                    return js, True
    except Exception:
        return None, False
    return None, False


def _maybe_openai_json(prompt: str) -> tuple[dict[str, Any] | None, bool]:
    key = os.environ.get("OPENAI_API_KEY", "").strip()
    if not key:
        return None, False
    model = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
    payload = {
        "model": model,
        "messages": [{"role": "system", "content": _LLM_SYSTEM}, {"role": "user", "content": prompt}],
        "temperature": 0.25,
        "max_tokens": 1800,
    }
    try:
        with httpx.Client(timeout=60.0) as client:
            r = client.post(
                "https://api.openai.com/v1/chat/completions",
                headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
                content=json.dumps(payload),
            )
            r.raise_for_status()
            data = r.json()
            text = (data.get("choices") or [{}])[0].get("message", {}).get("content")
            if isinstance(text, str):
                js = _extract_json(text)
                if js:
                    return js, True
    except Exception:
        return None, False
    return None, False


def _try_llm_json(prompt: str) -> tuple[dict[str, Any] | None, bool, str | None]:
    provider = os.environ.get("LLM_PROVIDER", "auto").strip().lower()
    if provider == "ollama":
        js, ok = _maybe_ollama_json(prompt)
        return js, ok, "ollama" if ok else None
    if provider == "openai":
        js, ok = _maybe_openai_json(prompt)
        return js, ok, "openai" if ok else None
    js, ok = _maybe_ollama_json(prompt)
    if ok:
        return js, True, "ollama"
    js, ok = _maybe_openai_json(prompt)
    return js, ok, "openai" if ok else None


def _normalize_phase(idx: int, p: dict[str, Any]) -> dict[str, Any]:
    return {
        "phaseOrder": int(p.get("phaseOrder") or idx),
        "name": str(p.get("name") or f"Phase {idx}"),
        "plannedDays": max(1, int(p.get("plannedDays") or 5)),
        "startDate": p.get("startDate"),
        "dueDate": p.get("dueDate"),
        "milestones": [str(x) for x in (p.get("milestones") or [])][:8],
        "deliverables": [
            {"title": str(d.get("title") or "Livrable"), "type": str(d.get("type") or "DOC")}
            for d in (p.get("deliverables") or [])
            if isinstance(d, dict)
        ][:8],
        "acceptanceCriteria": [str(x) for x in (p.get("acceptanceCriteria") or [])][:10],
    }


def generate_initial_plan(
    project_title: str,
    project_description: str,
    duration_days: int,
    start_date_iso: str | None,
    required_skills: list[str],
    use_llm: bool,
) -> dict[str, Any]:
    start_dt = _parse_dt(start_date_iso)
    fallback = _heuristic_initial_plan(project_title, project_description, duration_days, start_dt, required_skills)
    if not use_llm:
        return fallback

    prompt = _llm_prompt_initial(project_title, project_description, duration_days, start_date_iso, required_skills)
    js, ok, backend = _try_llm_json(prompt)
    if not ok or not js:
        return fallback
    phases_raw = js.get("phases")
    if not isinstance(phases_raw, list) or not phases_raw:
        return fallback

    phases = [_normalize_phase(i + 1, p if isinstance(p, dict) else {}) for i, p in enumerate(phases_raw[:8])]
    return {
        "methodVersion": METHOD_VERSION,
        "planKind": "INITIAL",
        "llmUsed": True,
        "llmBackend": backend,
        "complexity": str(js.get("complexity") or fallback.get("complexity") or "medium"),
        "summary": str(js.get("summary") or "Plan initial généré par LLM."),
        "phases": phases,
    }


def adjust_plan(
    current_phases: list[dict[str, Any]],
    schedule_assessment: dict[str, Any] | None,
    use_llm: bool,
) -> dict[str, Any]:
    fallback = _heuristic_adjust_plan(current_phases, schedule_assessment)
    if not use_llm:
        return fallback

    prompt = _llm_prompt_adjust(current_phases, schedule_assessment)
    js, ok, backend = _try_llm_json(prompt)
    if not ok or not js:
        return fallback
    phases_raw = js.get("phases")
    if not isinstance(phases_raw, list) or not phases_raw:
        return fallback

    phases = [_normalize_phase(i + 1, p if isinstance(p, dict) else {}) for i, p in enumerate(phases_raw[:12])]
    return {
        "methodVersion": METHOD_VERSION,
        "planKind": "ADJUSTED",
        "llmUsed": True,
        "llmBackend": backend,
        "summary": str(js.get("summary") or "Plan réajusté par LLM."),
        "adjustmentReason": fallback.get("adjustmentReason"),
        "recommendedActions": [str(x) for x in (js.get("recommendedActions") or [])][:10]
        or fallback.get("recommendedActions"),
        "phases": phases,
    }
