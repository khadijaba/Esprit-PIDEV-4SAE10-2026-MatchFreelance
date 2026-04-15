"""Analyse titre + description : compétences, complexité, budget/durée indicatifs (règles + lexique)."""
from __future__ import annotations

import re
from typing import Any

# Sous-chaîne (minuscule) -> libellé compétence affiché
SKILL_TRIGGERS: list[tuple[str, str]] = [
    ("spring boot", "Spring Boot"),
    ("springboot", "Spring Boot"),
    ("spring ", "Spring"),
    ("java", "Java"),
    ("kotlin", "Kotlin"),
    ("angular", "Angular"),
    ("react", "React"),
    ("vue", "Vue.js"),
    ("next.js", "Next.js"),
    ("node", "Node.js"),
    ("nodejs", "Node.js"),
    ("python", "Python"),
    ("django", "Django"),
    ("fastapi", "FastAPI"),
    ("php", "PHP"),
    ("laravel", "Laravel"),
    ("symfony", "Symfony"),
    ("mysql", "MySQL"),
    ("postgresql", "PostgreSQL"),
    ("mongo", "MongoDB"),
    ("redis", "Redis"),
    ("docker", "Docker"),
    ("kubernetes", "Kubernetes"),
    ("k8s", "Kubernetes"),
    ("aws", "AWS"),
    ("azure", "Azure"),
    ("gcp", "GCP"),
    ("terraform", "Terraform"),
    ("devops", "DevOps"),
    ("ci/cd", "CI/CD"),
    ("jenkins", "Jenkins"),
    ("git", "Git"),
    ("flutter", "Flutter"),
    ("android", "Android"),
    ("ios", "iOS"),
    ("swift", "Swift"),
    ("figma", "Figma"),
    ("ui/ux", "UI/UX"),
    ("machine learning", "Machine Learning"),
    ("deep learning", "Deep Learning"),
    ("tensorflow", "TensorFlow"),
    ("pytorch", "PyTorch"),
    ("power bi", "Power BI"),
    ("excel", "Excel"),
]

SECTION_SKILL = re.compile(
    r"(?:compétences?\s+requises?|skills?\s+requis|required\s+skills?|technologies?|tech|langages?)\s*[:\-]\s*([^\n]+)",
    re.I,
)


def _word_count(text: str) -> int:
    return len(text.split()) if text.strip() else 0


def _extract_skills_from_sections(text: str) -> list[str]:
    m = SECTION_SKILL.search(text)
    if not m:
        return []
    block = m.group(1).strip()
    parts = re.split(r"[,;\n•]|(?:\s+et\s+)|(?:\s+and\s+)", block, flags=re.I)
    return [p.strip() for p in parts if 2 <= len(p.strip()) <= 50]


def _skills_from_keywords(text: str) -> list[str]:
    t = text.lower()
    found: list[str] = []
    for needle, label in SKILL_TRIGGERS:
        if needle in t and label not in found:
            found.append(label)
    return found


def analyze_project(title: str, description: str) -> dict[str, Any]:
    text = f"{title}\n{description}".strip()
    wc = _word_count(text)

    skills = _extract_skills_from_sections(description)
    for s in _skills_from_keywords(text):
        if s not in skills:
            skills.append(s)

    heavy = sum(
        1
        for k in (
            "microservice",
            "migration",
            "kubernetes",
            "sécurité",
            "security",
            "temps réel",
            "machine learning",
            "erp",
            "refonte",
            "legacy",
        )
        if k in text.lower()
    )

    if wc < 40 and len(skills) < 2:
        complexity = "simple"
        base_days = 21
        budget_min, budget_max = 2500, 8000
    elif wc < 120 and heavy < 2:
        complexity = "medium"
        base_days = 45
        budget_min, budget_max = 6000, 22000
    else:
        complexity = "complex"
        base_days = 90
        budget_min, budget_max = 12000, 55000

    duration = max(7, min(180, base_days + len(skills) * 3))
    budget_mid = (budget_min + budget_max) // 2
    budget_min = max(500, budget_mid - (budget_max - budget_min) // 4)
    budget_max = budget_mid + (budget_max - budget_min) // 4

    roles: list[str] = []
    tl = title.lower()
    dl = description.lower()
    if any(x in dl or x in tl for x in ("frontend", "front-end", "angular", "react", "vue")):
        roles.append("Développeur front-end")
    if any(x in dl or x in tl for x in ("backend", "api", "spring", "java", "node")):
        roles.append("Développeur back-end")
    if any(x in dl for x in ("devops", "docker", "kubernetes", "ci/cd")):
        roles.append("Ingénieur DevOps")
    if any(x in dl for x in ("design", "figma", "ui/ux")):
        roles.append("Designer UI/UX")
    if not roles:
        roles = ["Développeur full-stack"]

    summary = (
        f"Analyse ({complexity}) : {len(skills)} compétence(s) détectée(s). "
        f"Budget indicatif {budget_min}–{budget_max} TND, durée indicative ~{duration} jours."
    )

    return {
        "complexity": complexity,
        "roles": roles[:6],
        "requiredSkills": skills[:20],
        "budgetEstimate": {"minAmount": float(budget_min), "maxAmount": float(budget_max), "currency": "TND"},
        "durationEstimateDays": duration,
        "technicalLeaderRole": roles[0] if roles else None,
        "summary": summary,
    }
