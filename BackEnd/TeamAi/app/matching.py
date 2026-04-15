"""Calcul des freelancers compatibles (score aligné sur le front : 70 % skills + 30 % exp)."""
from __future__ import annotations

import os
import re
from typing import Any

import httpx

GATEWAY_URL = os.environ.get("GATEWAY_URL", "http://127.0.0.1:8086").rstrip("/")
THRESHOLD = 70
WEIGHT_SKILL = 0.7
WEIGHT_EXP = 0.3
MAX_YEARS = 10
MIN_SUB = 3


def _norm(s: str) -> str:
    return s.strip().lower().replace("_", " ")


def skill_matches_required(required_norm: str, freelancer_norm: list[str]) -> bool:
    r = _norm(required_norm)
    if not r or not freelancer_norm:
        return False
    fset = {_norm(x) for x in freelancer_norm if x and len(x.strip()) >= 2}
    for f in fset:
        if len(f) < 2:
            continue
        if r == f:
            return True
        if (r in f or f in r) and len(r) >= MIN_SUB and len(f) >= MIN_SUB:
            return True
    words = [w for w in re_split_words(r) if len(w) >= 2]
    for word in words:
        if len(word) < MIN_SUB:
            continue
        for f in fset:
            if len(f) < 2:
                continue
            if word == f:
                return True
            if (f in word or word in f) and len(f) >= MIN_SUB:
                return True
    return False


def re_split_words(r: str) -> list[str]:
    return [x for x in re.split(r"[\s,]+", r) if x]


def compute_skill_match(required_norm: list[str], skill_names: list[str], categories: list[str]) -> float:
    if not required_norm:
        return 100.0
    all_n = [_norm(n) for n in skill_names if n]
    all_n.extend([_norm(c) for c in categories if c])
    matched = sum(1 for req in required_norm if skill_matches_required(req, all_n))
    return (matched / len(required_norm)) * 100.0


def compute_experience(skills: list[dict[str, Any]]) -> float:
    if not skills:
        return 0.0
    years = [float(s.get("yearsOfExperience") or 0) for s in skills]
    avg = sum(years) / len(years)
    return min(100.0, (avg / MAX_YEARS) * 100.0)


async def compute_compatible(
    project_id: int,
    project_title: str,
    required_skills: list[str],
    authorization: str | None,
) -> dict[str, Any]:
    required_norm = [_norm(s) for s in required_skills if s and _norm(s)]
    if not required_norm:
        return {"notifications": [], "threshold": THRESHOLD}

    headers: dict[str, str] = {}
    if authorization:
        headers["Authorization"] = authorization

    try:
        async with httpx.AsyncClient(timeout=45.0) as client:
            users_r = await client.get(
                f"{GATEWAY_URL}/api/users",
                params={"role": "FREELANCER"},
                headers=headers,
            )
            users_r.raise_for_status()
            users = users_r.json()

            skills_r = await client.get(f"{GATEWAY_URL}/api/skills", headers=headers)
            skills_r.raise_for_status()
            all_skills = skills_r.json()
    except Exception:
        return {"notifications": [], "threshold": THRESHOLD}

    by_freelancer: dict[int, list[dict[str, Any]]] = {}
    for s in all_skills:
        if s.get("blocked"):
            continue
        fid = s.get("freelancerId")
        if fid is None:
            continue
        by_freelancer.setdefault(int(fid), []).append(s)

    user_by_id = {int(u["id"]): u for u in users if u.get("id") is not None}

    notifications: list[dict[str, Any]] = []
    for fid, sks in by_freelancer.items():
        user = user_by_id.get(fid)
        if not user:
            continue
        names = [str(s.get("name") or "") for s in sks]
        cats = [str(s.get("category") or "") for s in sks]
        sm = compute_skill_match(required_norm, names, cats)
        ex = compute_experience(sks)
        score = round((sm / 100) * WEIGHT_SKILL * 100 + (ex / 100) * WEIGHT_EXP * 100)
        score = max(0, min(100, score))
        if score < THRESHOLD:
            continue
        name = (user.get("fullName") or user.get("username") or user.get("email") or f"Freelancer #{fid}").strip()
        email = user.get("email")
        msg = (
            f"Bonjour {name},\n\n"
            f"Un nouveau projet « {project_title} » correspond à votre profil (compatibilité ~{score} %).\n"
            f"Connectez-vous à la plateforme pour en savoir plus.\n"
        )
        notifications.append(
            {
                "freelancerId": fid,
                "fullName": name,
                "email": email,
                "score": float(score),
                "subject": "Nouveau projet compatible pour vous !",
                "message": msg,
            }
        )

    notifications.sort(key=lambda x: x["score"], reverse=True)
    return {"notifications": notifications, "threshold": THRESHOLD}
