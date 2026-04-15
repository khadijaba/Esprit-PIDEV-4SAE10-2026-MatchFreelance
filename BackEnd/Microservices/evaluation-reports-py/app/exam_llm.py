"""
Génération de QCM via une API compatible OpenAI (OpenAI, Azure OpenAI, Ollama /v1, etc.).
Variables d'environnement :
  EXAMEN_LLM_API_KEY — pour OpenAI / Azure ; avec Ollama sur :11434, peut rester vide (Bearer factice côté client)
  EXAMEN_LLM_ENABLED       — si true, utilisation par défaut quand useLlm n'est pas false (voir exam_auto_generate)
  EXAMEN_LLM_BASE_URL      — défaut https://api.openai.com/v1  ; Ollama : http://127.0.0.1:11434/v1
  EXAMEN_LLM_MODEL         — défaut gpt-4o-mini
  EXAMEN_LLM_TIMEOUT       — secondes (défaut 120)
"""

from __future__ import annotations

import json
import logging
import os
import re
from typing import Any

logger = logging.getLogger(__name__)

try:
    import httpx
except ImportError:  # pragma: no cover
    httpx = None  # type: ignore


def _str(v: Any) -> str:
    return "" if v is None else str(v).strip()


def _ollama_compatible_url() -> bool:
    u = (os.getenv("EXAMEN_LLM_BASE_URL") or "").lower()
    return ":11434" in u or "ollama" in u


def llm_configured() -> bool:
    if httpx is None:
        return False
    key = (os.getenv("EXAMEN_LLM_API_KEY") or "").strip()
    if key:
        return True
    return _ollama_compatible_url()


def _base_url() -> str:
    return (os.getenv("EXAMEN_LLM_BASE_URL") or "https://api.openai.com/v1").rstrip("/")


def _model() -> str:
    return os.getenv("EXAMEN_LLM_MODEL") or "gpt-4o-mini"


def _timeout() -> float:
    try:
        return float(os.getenv("EXAMEN_LLM_TIMEOUT") or "120")
    except ValueError:
        return 120.0


def _extract_json_array(text: str) -> list[Any] | None:
    if not text:
        return None
    t = text.strip()
    fence = re.search(r"```(?:json)?\s*(\[.*?\])\s*```", t, re.DOTALL)
    if fence:
        t = fence.group(1)
    else:
        start = t.find("[")
        if start < 0:
            return None
        depth = 0
        for i, c in enumerate(t[start:], start):
            if c == "[":
                depth += 1
            elif c == "]":
                depth -= 1
                if depth == 0:
                    t = t[start : i + 1]
                    break
        else:
            return None
    try:
        data = json.loads(t)
    except json.JSONDecodeError:
        return None
    return data if isinstance(data, list) else None


def _chat_completion(messages: list[dict[str, str]]) -> str | None:
    if httpx is None:
        return None
    url = f"{_base_url()}/chat/completions"
    key = (os.getenv("EXAMEN_LLM_API_KEY") or "").strip()
    headers = {"Content-Type": "application/json"}
    if key:
        headers["Authorization"] = f"Bearer {key}"
    elif _ollama_compatible_url():
        headers["Authorization"] = "Bearer ollama"
    body = {
        "model": _model(),
        "messages": messages,
        "temperature": 0.42,
        "stream": False,
    }
    try:
        with httpx.Client(timeout=_timeout()) as client:
            r = client.post(url, headers=headers, json=body)
            r.raise_for_status()
            data = r.json()
    except Exception as e:
        logger.warning("LLM chat/completions échoué: %s", e)
        return None
    try:
        return data["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError):
        logger.warning("LLM réponse inattendue: %s", data)
        return None


def _normalize_question(obj: dict[str, Any], expected_ordre: int) -> dict[str, Any] | None:
    o = expected_ordre
    enonce = _str(obj.get("enonce"))
    a, b, c, d = (
        _str(obj.get("optionA")),
        _str(obj.get("optionB")),
        _str(obj.get("optionC")),
        _str(obj.get("optionD")),
    )
    br = _str(obj.get("bonneReponse")).upper().strip()
    if br not in ("A", "B", "C", "D"):
        br = "A"
    expl = _str(obj.get("explication"))
    if len(enonce) < 18 or min(len(a), len(b), len(c), len(d)) < 5:
        return None
    return {
        "ordre": o,
        "enonce": enonce,
        "optionA": a,
        "optionB": b,
        "optionC": c,
        "optionD": d,
        "bonneReponse": br,
        "explication": expl,
    }


def _chunk_prompt(titre_formation: str, chunk: list[dict[str, Any]]) -> str:
    lines = [
        f"Formation : {titre_formation.strip() or '—'}",
        "",
        "Pour chaque bloc ci-dessous, rédige UNE question QCM en français, comme dans un vrai examen professionnalisant.",
        "Exigences :",
        "- Énoncé = question fermée sur une notion concrète du descriptif (objectif, étape, outil, règle), pas une méta-question « cohérente avec le texte ».",
        "- Pas de longue citation du descriptif dans l'énoncé ; reformule pour tester la compréhension.",
        "- Une seule bonne réponse ; options courtes, distinctes ; distracteurs plausibles mais faux ou contredits par le module.",
        "- Varie les formulations (pourquoi, comment, lequel, dans quel cas, quelle conséquence).",
        "- Respecte la difficulté indiquée (FACILE/MOYEN/DIFFICILE).",
        "- Jamais « bloc », « consigne », « descriptif fourni » ni mention du processus de génération.",
        "",
    ]
    for i, m in enumerate(chunk, 1):
        desc = _str(m.get("desc"))
        if len(desc) > 1800:
            desc = desc[:1797] + "..."
        lines.append(
            f"Bloc {i} — ordre={m['ordre']}, module={_str(m.get('mod_titre'))!r}, "
            f"lot_parcours={m.get('inclusion')}, difficulté={m.get('niveau')}, thème={_str(m.get('theme'))!r}"
        )
        if not desc:
            tf = titre_formation.strip() or "—"
            mt = _str(m.get("mod_titre"))
            lines.append(
                "Contexte pédagogique : aucun descriptif n'est renseigné pour ce module. "
                f"À partir du titre « {mt} » et de la formation « {tf} », rédige UNE question QCM réaliste "
                "sur les notions enseignées habituellement sous ce thème ; distracteurs plausibles. "
                "Ne mentionne jamais qu'il manque un descriptif."
            )
        else:
            lines.append(f"Descriptif module : {desc}")
        lines.append("")
    lines.append(
        "Réponds UNIQUEMENT avec un tableau JSON (sans texte avant ou après) : "
        '[{"ordre": nombre, "enonce": "...", "optionA": "...", "optionB": "...", "optionC": "...", '
        '"optionD": "...", "bonneReponse": "A"|"B"|"C"|"D", "explication": "..."}, ...] '
        f"Le tableau doit contenir exactement {len(chunk)} objets, dans le même ordre que les blocs, "
        "avec les mêmes valeurs « ordre » que dans les blocs."
    )
    return "\n".join(lines)


def generate_questions_llm(titre_formation: str, metas: list[dict[str, Any]]) -> list[dict[str, Any]] | None:
    """
    metas : {ordre, mod_titre, desc, inclusion, niveau, theme}
    Retourne une liste de dicts question (sans parcoursInclusion / niveauDifficulte / theme) ou None.
    """
    if not metas or not llm_configured():
        return None
    CHUNK = 1 if _ollama_compatible_url() else 4
    merged: list[dict[str, Any]] = []
    system = (
        "Tu es un concepteur d'évaluations certifiantes en français. "
        "Tu réponds uniquement par un tableau JSON (liste d'objets), sans markdown, sans texte hors JSON. "
        "Chaque objet : ordre, enonce, optionA..D, bonneReponse, explication."
    )
    for i in range(0, len(metas), CHUNK):
        chunk = metas[i : i + CHUNK]
        user = _chunk_prompt(titre_formation, chunk)
        content = _chat_completion(
            [
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ]
        )
        if not content:
            return None
        arr = _extract_json_array(content)
        if not arr or len(arr) != len(chunk):
            logger.warning(
                "LLM: attendu %s questions, obtenu %s", len(chunk), len(arr) if arr else 0
            )
            return None
        for j, raw in enumerate(arr):
            if not isinstance(raw, dict):
                return None
            exp = chunk[j]["ordre"]
            nq = _normalize_question(raw, exp)
            if nq is None or nq["ordre"] != exp:
                logger.warning("LLM: question invalide ou ordre incorrect pour ordre=%s", exp)
                return None
            merged.append(nq)
    return merged


def merge_meta_and_llm(
    metas: list[dict[str, Any]], llm_questions: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    by_ordre = {q["ordre"]: q for q in llm_questions}
    for m in metas:
        o = m["ordre"]
        q = by_ordre.get(o)
        if not q:
            continue
        out.append(
            {
                "ordre": o,
                "enonce": q["enonce"],
                "optionA": q["optionA"],
                "optionB": q["optionB"],
                "optionC": q["optionC"],
                "optionD": q["optionD"],
                "bonneReponse": q["bonneReponse"],
                "parcoursInclusion": m["inclusion"],
                "niveauDifficulte": m["niveau"],
                "theme": m["theme"],
                "skill": m["theme"],
                "explication": q.get("explication") or "",
            }
        )
    return out
