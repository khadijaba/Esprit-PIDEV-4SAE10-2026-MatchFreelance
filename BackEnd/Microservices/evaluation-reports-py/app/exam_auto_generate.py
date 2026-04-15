"""
Génération automatique d'un examen QCM à partir des modules d'une formation.
Questions dérivées du titre et du descriptif (affirmation fidèle au texte + distracteurs).
Même règles métier que le service Java (min 7 questions, lots parcours, pondération difficulté).
"""

from __future__ import annotations

import logging
import os
import re

logger = logging.getLogger(__name__)

MIN_QUESTIONS_PAR_EXAMEN = 7
MAX_ENONCE = 1000
MAX_OPTION = 500
MAX_EXPLICATION = 2000

SENTENCE_SPLIT = re.compile(r"(?<=[.!?])\s+")


def _str(v) -> str:
    return "" if v is None else str(v)


def _shorten(s: str, max_len: int) -> str:
    if s is None or len(s) <= max_len:
        return s or ""
    return s[: max(0, max_len - 3)] + "..."


def _parse_ordre(o) -> int:
    if o is None:
        return 0
    try:
        return int(o)
    except (TypeError, ValueError):
        return 0


def _parcours_pour_ordre(ordre: int) -> str:
    return ("COMMUN", "STANDARD", "RENFORCEMENT")[ordre % 3]


def _difficulte_pour_ordre(ordre: int) -> str:
    return ("FACILE", "MOYEN", "DIFFICILE")[ordre % 3]


def _matches_parcours(inclusion: str, type_parcours: str) -> bool:
    inc = (inclusion or "COMMUN").strip().upper()
    if inc == "COMMUN":
        return True
    if type_parcours == "STANDARD":
        return inc == "STANDARD"
    return inc == "RENFORCEMENT"


def _count_pour_parcours(questions: list[dict], type_parcours: str) -> int:
    return sum(
        1 for q in questions if _matches_parcours(q.get("parcoursInclusion"), type_parcours)
    )


def _count_pour_parcours_metas(metas: list[dict], type_parcours: str) -> int:
    return sum(1 for m in metas if _matches_parcours(m.get("inclusion"), type_parcours))


def _should_use_llm(request_flag: bool | None) -> bool:
    from app.exam_llm import llm_configured

    if not llm_configured():
        return False
    if request_flag is False:
        return False
    if request_flag is True:
        return True
    return os.getenv("EXAMEN_LLM_ENABLED", "").lower() in ("1", "true", "yes")


def _collect_metas(sorted_mods: list[dict]) -> list[dict]:
    metas: list[dict] = []
    ordre = 0
    n_mod = len(sorted_mods)
    for mod in sorted_mods:
        mod_titre = _str(mod.get("titre")).strip() or f"Module {ordre + 1}"
        desc = _str(mod.get("description"))
        metas.append(
            {
                "ordre": ordre,
                "mod_titre": mod_titre,
                "desc": desc,
                "inclusion": _parcours_pour_ordre(ordre),
                "niveau": _difficulte_pour_ordre(ordre),
                "theme": _shorten(mod_titre, 120),
            }
        )
        ordre += 1
    while len(metas) < MIN_QUESTIONS_PAR_EXAMEN:
        mod = sorted_mods[ordre % n_mod]
        mod_titre = _str(mod.get("titre")).strip() or f"Module {(ordre % n_mod) + 1}"
        desc = _str(mod.get("description"))
        metas.append(
            {
                "ordre": ordre,
                "mod_titre": mod_titre,
                "desc": desc,
                "inclusion": _parcours_pour_ordre(ordre),
                "niveau": _difficulte_pour_ordre(ordre),
                "theme": _shorten(mod_titre, 120),
            }
        )
        ordre += 1
    while (
        _count_pour_parcours_metas(metas, "STANDARD") < MIN_QUESTIONS_PAR_EXAMEN
        or _count_pour_parcours_metas(metas, "RENFORCEMENT") < MIN_QUESTIONS_PAR_EXAMEN
    ):
        mod = sorted_mods[ordre % n_mod]
        mod_titre = _str(mod.get("titre")).strip() or f"Module {(ordre % n_mod) + 1}"
        desc = _str(mod.get("description"))
        metas.append(
            {
                "ordre": ordre,
                "mod_titre": mod_titre,
                "desc": desc,
                "inclusion": "COMMUN",
                "niveau": _difficulte_pour_ordre(ordre),
                "theme": _shorten(mod_titre, 120),
            }
        )
        ordre += 1
    return metas


def _heuristic_from_metas(metas: list[dict], nb_modules_source: int) -> list[dict]:
    return [
        _question_from_module(
            m["mod_titre"],
            m["desc"],
            m["ordre"],
            m["inclusion"],
            m["niveau"],
            m["theme"],
            m["ordre"] >= nb_modules_source,
        )
        for m in metas
    ]


def _truncate_merged_question(q: dict) -> dict:
    br = (q.get("bonneReponse") or "A").strip().upper()
    if not br or br[0] not in "ABCD":
        br = "A"
    else:
        br = br[0]
    return {
        "ordre": q["ordre"],
        "enonce": _shorten(q.get("enonce"), MAX_ENONCE),
        "optionA": _shorten(q.get("optionA"), MAX_OPTION),
        "optionB": _shorten(q.get("optionB"), MAX_OPTION),
        "optionC": _shorten(q.get("optionC"), MAX_OPTION),
        "optionD": _shorten(q.get("optionD"), MAX_OPTION),
        "bonneReponse": br,
        "parcoursInclusion": q.get("parcoursInclusion"),
        "niveauDifficulte": q.get("niveauDifficulte"),
        "theme": q.get("theme"),
        "skill": q.get("skill") or q.get("theme"),
        "explication": _shorten(q.get("explication") or "", MAX_EXPLICATION),
    }


def _split_sentences(text: str) -> list[str]:
    if not text or not text.strip():
        return []
    parts = SENTENCE_SPLIT.split(text.strip())
    return [p.strip() for p in parts if len(p.strip()) > 12]


def _question_from_module(
    mod_titre: str,
    desc: str,
    ordre: int,
    inclusion: str,
    niveau: str,
    theme: str,
    variante: bool,
) -> dict:
    phrases = _split_sentences(desc or "")
    idx = (ordre % len(phrases)) if phrases else 0
    if not phrases:
        enonce = (
            f"Concernant le module « {mod_titre} », quelle proposition reflète le mieux "
            "le rôle attendu de cette séquence dans le parcours ?"
        )
    else:
        r = ordre % 3
        if r == 0:
            enonce = (
                f"À propos du module « {mod_titre} », quelle proposition est la plus fidèle "
                "au contenu prévu pour les apprenants ?"
            )
        elif r == 1:
            enonce = (
                f"Pour la séquence « {mod_titre} », laquelle des affirmations correspond le mieux "
                "à ce qui est décrit dans le programme ?"
            )
        else:
            enonce = (
                f"Dans le cadre du module « {mod_titre} », identifiez l'affirmation qui repose "
                "correctement sur le descriptif publié."
            )
    if variante:
        enonce = f"{enonce} (Variante {ordre + 1}.)"

    if phrases:
        s0 = phrases[idx % len(phrases)].replace('"', "'")
        opt_a = f"D'après le descriptif, on peut retenir notamment : « {_shorten(s0, 380)} »."
        opt_b = (
            f"Le module est présenté comme sans lien avec « {_shorten(mod_titre, 80)} » "
            "et n'aborde aucun objectif du programme."
        )
        opt_c = (
            f"Il est indiqué qu'aucune notion de « {_shorten(mod_titre, 100)} » n'y est travaillée : "
            "séance purement informative, hors compétences visées."
        )
        if len(phrases) > 1:
            s1 = phrases[(idx + 1) % len(phrases)].replace('"', "'")
            opt_d = (
                "Le programme exclut explicitement tout contenu du type : "
                f"« {_shorten(s1, 360)} »."
            )
        else:
            opt_d = (
                "Le descriptif précise qu'aucune mise en pratique n'est prévue pour "
                f"« {_shorten(mod_titre, 120)} »."
            )
        explication = (
            "La bonne réponse s'appuie sur une formulation du descriptif du module "
            f"« {_shorten(mod_titre, 80)} ». Contrôlez l'exactitude avant certification."
        )
    else:
        opt_a = (
            f"Le module « {_shorten(mod_titre, 220)} » fait partie du parcours et contribue "
            "aux objectifs de la formation sur ce thème."
        )
        opt_b = "Ce module est optionnel : on peut omettre cette séquence sans impact sur la suite du parcours."
        opt_c = (
            "Il est réservé aux apprenants ayant déjà une certification externe équivalente, "
            "et non à tous les inscrits."
        )
        opt_d = "Il remplace l'ensemble des autres modules et annule les évaluations déjà réalisées dans la formation."
        explication = (
            "Sans descriptif de module, la réponse attendue reflète le fait que la séquence est bien au programme. "
            "Enrichissez les textes des modules pour des QCM plus précis."
        )

    return {
        "ordre": ordre,
        "enonce": _shorten(enonce, MAX_ENONCE),
        "optionA": _shorten(opt_a, MAX_OPTION),
        "optionB": _shorten(opt_b, MAX_OPTION),
        "optionC": _shorten(opt_c, MAX_OPTION),
        "optionD": _shorten(opt_d, MAX_OPTION),
        "bonneReponse": "A",
        "parcoursInclusion": inclusion,
        "niveauDifficulte": niveau,
        "theme": theme,
        "skill": theme,
        "explication": _shorten(explication, MAX_EXPLICATION),
    }


def build_examen_from_modules(
    formation_id: int,
    titre_formation: str,
    modules: list[dict],
    suffixe_titre: str | None = None,
    seuil_reussi: int = 60,
    use_llm: bool | None = None,
) -> dict:
    if not modules:
        raise ValueError(
            "Aucun module : impossible de générer un examen. Ajoutez des modules côté Formation."
        )

    sorted_mods = sorted(modules, key=lambda m: _parse_ordre(m.get("ordre")))
    nb_source_mods = len(sorted_mods)

    titre = f"Examen — {_str(titre_formation).strip() or f'Formation {formation_id}'}"
    if suffixe_titre and str(suffixe_titre).strip():
        titre = f"{titre} {str(suffixe_titre).strip()}"

    metas = _collect_metas(sorted_mods)
    used_llm = False
    if _should_use_llm(use_llm):
        from app.exam_llm import generate_questions_llm, merge_meta_and_llm

        llq = generate_questions_llm(
            _str(titre_formation).strip() or f"Formation {formation_id}", metas
        )
        if llq and len(llq) == len(metas):
            merged = merge_meta_and_llm(metas, llq)
            if len(merged) == len(metas):
                questions = [_truncate_merged_question(q) for q in merged]
                used_llm = True
    if not used_llm:
        if _should_use_llm(use_llm):
            logger.warning(
                "Génération examen (Python) : LLM invalide ou incomplet — repli heuristique. "
                "Vérifiez Ollama/OpenAI, le modèle et des descriptifs de modules détaillés."
            )
        questions = _heuristic_from_metas(metas, nb_source_mods)

    description = (
        "Examen généré automatiquement (Python) à partir des titres et descriptifs des modules. "
        f"Au moins {MIN_QUESTIONS_PAR_EXAMEN} questions par parcours (Standard et Renforcement). "
        + ("Questions rédigées avec assistance LLM (relecture obligatoire). " if used_llm else "")
        + "Relecture formateur recommandée avant usage certifiant."
    )

    return {
        "formationId": formation_id,
        "titre": titre,
        "description": description,
        "seuilReussi": max(0, min(100, int(seuil_reussi))),
        "questions": questions,
    }
