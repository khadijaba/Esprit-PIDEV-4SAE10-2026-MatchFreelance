"""
Suggestions de reformulation de description projet (périmètre, livrables, critères d'acceptation).
Heuristiques + brouillon structuré ; LLM optionnel : Ollama (local) et/ou OpenAI.
"""
from __future__ import annotations

import json
import os
import re
from typing import Any

import httpx

METHOD_VERSION = "desc-coach-v2"

_LLM_SYSTEM = (
    "Tu es un assistant pour rédiger des fiches projet B2B en français. "
    "Tu produis UNIQUEMENT un texte structuré (markdown) : Contexte, Périmètre, "
    "Livrables, Critères d'acceptation, Hors périmètre. "
    "Reste factuel, sans promettre ce que le client n'a pas dit. "
    "Ne remplace pas la vision du porteur : propose un brouillon qu'il pourra éditer."
)

# Aide affichable dans l'UI (cartes « Qu'est-ce que c'est ? », etc.)
HELP = {
    "whatItIs": (
        "Des propositions de texte et une checklist pour clarifier la description : "
        "périmètre, livrables concrets, critères d'acceptation (« c'est fini quand… »)."
    ),
    "purpose": (
        "Réduire les malentendus, obtenir des devis plus précis et comparer les candidatures "
        "sur la même base. Moins de friction après signature."
    ),
    "howItWorks": (
        "Analyse du texte : repérage des manques (livrables nommés, critères mesurables, objectifs flous). "
        "Génération de paragraphes types et de questions de clarification. "
        "Avec un LLM configuré : brouillon enrichi que vous éditez toujours vous-même."
    ),
    "limits": (
        "Ces suggestions ne remplacent pas votre vision. Utilisez « Appliquer » seulement si le brouillon "
        "vous convient ; vous gardez la version officielle validée manuellement."
    ),
}

DELIVERABLE_HINTS = re.compile(
    r"\b(livrable|remise|fichier|maquette|mockup|rapport|documentation|déploiement|release|"
    r"livraison|code\s+source|repo|binaire|apk|livraison|sprint\s+\d)\b",
    re.I,
)
ACCEPTANCE_HINTS = re.compile(
    r"\b(critère|critères|acceptation|recette|definition\s+of\s+done|dod|c'est\s+fini\s+quand|"
    r"c’est\s+fini\s+quand|livré\s+quand|conforme\s+à|validé\s+par|mesurable|jeux\s+de\s+tests)\b",
    re.I,
)
SCOPE_HINTS = re.compile(
    r"\b(périmètre|hors\s*scope|hors\s+périmètre|inclus|non\s+inclus|exclu|exclusions)\b",
    re.I,
)
VALIDATION_HINTS = re.compile(
    r"\b(qui\s+valide|product\s+owner|po\b|validation|approbation|sign\s*off)\b",
    re.I,
)
ENV_HINTS = re.compile(
    r"\b(environnement|prod|préprod|pré\-prod|staging|recette|hébergement|cloud)\b",
    re.I,
)
METRIC_HINTS = re.compile(
    r"\d+\s*(%|jours?|semaines?|mois|h\b|heures?|utilisateurs?|écrans?|pages?|kpi)\b|\b(sla|disponibilité)\b",
    re.I,
)


def _word_count(text: str) -> int:
    return len(text.split()) if text.strip() else 0


def _detect_gaps(title: str, description: str) -> list[str]:
    text = f"{title}\n{description}".strip()
    t = text.lower()
    gaps: list[str] = []

    if _word_count(description) < 35:
        gaps.append("DESCRIPTION_TROP_COURTE : précisez le contexte, les utilisateurs cibles et le résultat attendu.")

    if not DELIVERABLE_HINTS.search(t):
        gaps.append("LIVRABLES_IMPLICITES : aucun livrable concret nommé (ex. code, maquettes, doc, déploiement).")

    if not ACCEPTANCE_HINTS.search(t):
        gaps.append(
            "ACCEPTATION_FLOUE : ajoutez des critères du type « c'est fini quand… » (tests, recette, métriques)."
        )

    if not SCOPE_HINTS.search(t):
        gaps.append("PERIMETRE_VAGUE : indiquez ce qui est inclus / exclu ou ce qui est hors périmètre.")

    if not VALIDATION_HINTS.search(t):
        gaps.append("VALIDATION_NON_PRECISEE : qui valide la recette ou les livrables (vous, un métier, un PO) ?")

    if not ENV_HINTS.search(t):
        gaps.append("ENVIRONNEMENTS : précisez où le livrable doit tourner (local, recette, production, cloud…).")

    if not METRIC_HINTS.search(t):
        gaps.append("PEU_DE_MESURES : une échéance, un volume ou un indicateur rend l'objectif plus comparable.")

    return gaps


def _checklist(gaps: list[str]) -> list[str]:
    base = [
        "Objectif métier en une phrase (pour qui, quel problème).",
        "Liste des livrables avec format attendu (sources, binaires, exports, accès).",
        "Critères d'acceptation testables pour chaque livrable majeur.",
        "Périmètre : 2–3 éléments explicitement hors scope si besoin.",
        "Qui valide et en combien de temps après livraison.",
        "Contraintes techniques ou légales (RGPD, accessibilité, navigateurs cibles).",
    ]
    if any("LIVRABLES" in g for g in gaps):
        base.append("Préciser les livrables intermédiaires (jalons) si le projet est long.")
    if any("ACCEPTATION" in g for g in gaps):
        base.append("Pour chaque livrable : « c'est fini quand [test / démo / signature] ».")
    return base


def _questions(gaps: list[str], title: str) -> list[str]:
    qs = [
        f"Quels livrables exacts pour « {title.strip()[:60]} » (fichiers, accès, environnement) ?",
        "Quels environnements cibles (dev, recette, production) et qui déploie ?",
        "Qui valide la recette et selon quels critères mesurables ?",
        "Y a-t-il des dépendances (API tierces, données, comptes) que vous fournissez ?",
        "Quelle est la date ou la fenêtre de livraison souhaitée ?",
    ]
    if any("PERIMETRE" in g for g in gaps):
        qs.append("Qu'est-ce qui est explicitement exclu du projet pour éviter l'effet scope creep ?")
    return qs


def _snippets(title: str, description: str, gap_codes: list[str]) -> dict[str, str]:
    first = (description.strip().split("\n")[0][:400] if description.strip() else "").strip()
    scope = (
        f"Le périmètre couvre la réalisation liée à « {title.strip()} », incluant les développements et livrables "
        f"convenus avec le prestataire. Les éléments non listés ci-dessous sont considérés comme hors périmètre "
        f"sauf accord écrit."
    )
    deliverables = (
        "- Code source / dépôt versionné et documenté pour installation.\n"
        "- Jeu de tests ou plan de recette partagé avec vous.\n"
        "- Courte documentation utilisateur ou README d'exploitation."
    )
    acceptance = (
        "- La recette est validée lorsque les scénarios convenus passent sans bloquant majeur.\n"
        "- Les livrables sont déployés sur l'environnement cible convenu et accessibles aux validateurs.\n"
        "- Les anomalies mineures sont listées et planifiées ; les bloquants sont corrigés avant clôture."
    )
    if any("LIVRABLES" in g for g in gap_codes):
        deliverables += "\n- (À préciser) Livrables spécifiques : maquettes, exports, formations…"
    return {
        "scope": scope,
        "deliverables": deliverables.strip(),
        "acceptance": acceptance.strip(),
        "contextReminder": first or f"Projet : {title.strip()}.",
    }


def _draft_enriched(title: str, description: str, snippets: dict[str, str]) -> str:
    body = description.strip() or "(Description initiale à compléter.)"
    return (
        f"{body}\n\n"
        f"---\n"
        f"**Brouillon d'enrichissement (à relire et éditer)**\n\n"
        f"### Contexte\n{snippets['contextReminder']}\n\n"
        f"### Périmètre\n{snippets['scope']}\n\n"
        f"### Livrables attendus\n{snippets['deliverables']}\n\n"
        f"### Critères d'acceptation (« c'est fini quand… »)\n{snippets['acceptance']}\n\n"
        f"### Hors périmètre\n"
        f"- À compléter avec le prestataire (ex. maintenance long terme, contenus rédactionnels, …).\n"
    )


def _user_llm_prompt(title: str, description: str) -> str:
    return (
        f"Titre : {title}\n\nDescription actuelle :\n{description}\n\n"
        f"Améliore et complète en t'appuyant sur ce texte. Garde les infos existantes.\n"
        f"Si des éléments manquent, pose-les comme puces « À clarifier »."
    )


def _maybe_ollama_enrich(title: str, description: str) -> tuple[str | None, bool]:
    base = os.environ.get("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
    model = os.environ.get("OLLAMA_MODEL", "llama3.2").strip()
    if not model:
        return None, False
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": _LLM_SYSTEM},
            {"role": "user", "content": _user_llm_prompt(title, description)},
        ],
        "stream": False,
        "options": {"temperature": 0.4},
    }
    try:
        with httpx.Client(timeout=httpx.Timeout(120.0, connect=5.0)) as client:
            r = client.post(f"{base}/api/chat", json=payload)
            r.raise_for_status()
            data = r.json()
            msg = data.get("message") if isinstance(data, dict) else None
            text = msg.get("content") if isinstance(msg, dict) else None
            if isinstance(text, str) and text.strip():
                return text.strip(), True
    except Exception:
        return None, False
    return None, False


def _maybe_openai_enrich(title: str, description: str) -> tuple[str | None, bool]:
    key = os.environ.get("OPENAI_API_KEY", "").strip()
    if not key:
        return None, False
    model = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
    try:
        payload = {
            "model": model,
            "messages": [
                {"role": "system", "content": _LLM_SYSTEM},
                {"role": "user", "content": _user_llm_prompt(title, description)},
            ],
            "temperature": 0.4,
            "max_tokens": 1800,
        }
        with httpx.Client(timeout=60.0) as client:
            r = client.post(
                "https://api.openai.com/v1/chat/completions",
                headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
                content=json.dumps(payload),
            )
            r.raise_for_status()
            data = r.json()
            text = (data.get("choices") or [{}])[0].get("message", {}).get("content")
            if isinstance(text, str) and text.strip():
                return text.strip(), True
    except Exception:
        return None, False
    return None, False


def _maybe_llm_enrich(title: str, description: str, _base_draft: str) -> tuple[str | None, bool, str | None]:
    """Essaie Ollama et/ou OpenAI selon LLM_PROVIDER (auto | ollama | openai)."""
    provider = os.environ.get("LLM_PROVIDER", "auto").strip().lower()
    if provider == "openai":
        t, ok = _maybe_openai_enrich(title, description)
        return t, ok, "openai" if ok else None
    if provider == "ollama":
        t, ok = _maybe_ollama_enrich(title, description)
        return t, ok, "ollama" if ok else None
    t, ok = _maybe_ollama_enrich(title, description)
    if ok:
        return t, True, "ollama"
    t, ok = _maybe_openai_enrich(title, description)
    return t, ok, "openai" if ok else None


def coach_description(title: str, description: str, use_llm: bool = False) -> dict[str, Any]:
    title = (title or "").strip()
    description = (description or "").strip()
    gaps_raw = _detect_gaps(title, description)
    gaps_human = [g.split(" : ", 1)[1] if " : " in g else g for g in gaps_raw]
    gap_codes = [g.split(" : ", 1)[0] if " : " in g else "INFO" for g in gaps_raw]
    checklist = _checklist(gaps_raw)
    questions = _questions(gaps_raw, title)
    snippets = _snippets(title, description, gap_codes)
    draft = _draft_enriched(title, description, snippets)

    llm_block: str | None = None
    llm_used = False
    llm_backend: str | None = None
    if use_llm:
        llm_block, llm_used, llm_backend = _maybe_llm_enrich(title, description, draft)

    return {
        "methodVersion": METHOD_VERSION,
        "help": HELP,
        "gaps": gaps_human,
        "gapCodes": gap_codes,
        "checklist": checklist,
        "questionsToClarify": questions,
        "suggestedSnippets": snippets,
        "draftEnrichedDescription": draft,
        "llmEnrichedMarkdown": llm_block,
        "llmUsed": llm_used,
        "llmBackend": llm_backend,
        "summary": (
            f"{len(gaps_human)} point(s) à renforcer détecté(s). "
            + (
                f"Brouillon LLM ({llm_backend}) disponible ci-dessous."
                if llm_used and llm_backend
                else "Brouillon LLM disponible ci-dessous."
                if llm_used
                else "Brouillon généré par règles (éditable)."
            )
        ),
    }
