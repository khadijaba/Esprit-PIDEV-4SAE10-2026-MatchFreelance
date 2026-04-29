"""
Analyse NLP/LLM du projet : rôles, compétences, budget, durée, complexité.
Utilise des règles + mots-clés par défaut ; option LLM si OPENAI_API_KEY ou Ollama.
"""
import re
from typing import Optional

from app.models.schemas import (
    AnalyzeRequest,
    AnalyzeResponse,
    BudgetEstimate,
)


# Rôles techniques courants (mapping mots-clés → rôle)
ROLE_KEYWORDS = {
    "backend": ["backend", "back-end", "api", "serveur", "server", "spring", "java", "node", "python backend"],
    "frontend": ["frontend", "front-end", "angular", "react", "vue", "interface", "ui", "ux"],
    "fullstack": ["fullstack", "full-stack", "full stack", "développeur full"],
    "devops": ["devops", "ci/cd", "docker", "kubernetes", "deployment", "infrastructure"],
    "mobile": ["mobile", "android", "ios", "flutter", "react native"],
    "data": ["data", "data science", "ml", "machine learning", "analytics", "bi"],
    "design": ["design", "ux", "ui", "figma", "designer", "maquette"],
    "lead": ["lead", "tech lead", "technical lead", "architecte", "architecture"],
    "qa": ["qa", "test", "quality", "qualité", "automation test"],
}

# Compétences techniques (pour extraction sans LLM)
SKILL_PATTERNS = [
    r"\b(java|python|javascript|typescript|node\.?js|react|angular|vue)\b",
    r"\b(spring\s*boot?|django|flask|express|nestjs)\b",
    r"\b(docker|kubernetes|aws|azure|gcp|jenkins|git)\b",
    r"\b(rest\s*api|graphql|microservices?|sql|mongodb|postgresql)\b",
    r"\b(html|css|sass|tailwind|bootstrap|redux|rxjs)\b",
    r"\b(jwt|oauth|agile|scrum|figma|ui/ux)\b",
]

# Indicateurs de complexité
COMPLEXITY_SIMPLE = re.compile(
    r"\b(simple|petit|small|landing|page\s+vitrine|formulaire)\b", re.I
)
COMPLEXITY_COMPLEX = re.compile(
    r"\b(équipe|team|plusieurs\s+dev|microservices?|multi\s*module|complexe|"
    r"architecture|plateforme|système\s+complet|intégration\s+multi)\b", re.I
)
BUDGET_PATTERN = re.compile(
    r"(?:budget|estimation|prix|coût|cost)\s*[:\-]?\s*(\d[\d\s]*)\s*(\$|usd|eur|euro|dt)?",
    re.I
)
DURATION_PATTERN = re.compile(
    r"(?:durée|duration|délai|timeline|mois|months?|semaines?|weeks?|jours?|days?)\s*[:\-]?\s*(\d+)",
    re.I
)


class ProjectAnalyzer:
    """Analyse la description d'un projet pour en extraire rôles, compétences, budget et durée."""

    def __init__(self, use_llm: bool = False, api_key: str = "", base_url: str = ""):
        self._env_use_llm = bool(use_llm)
        self._api_key = (api_key or "").strip()
        self._base_url = (base_url or "https://api.openai.com/v1").rstrip("/")

    def _llm_configured(self) -> bool:
        return bool(self._api_key) or bool(self._base_url)

    def analyze(self, req: AnalyzeRequest) -> AnalyzeResponse:
        prefer = bool(getattr(req, "prefer_llm", False))
        try_llm = (prefer or self._env_use_llm) and self._llm_configured()
        if try_llm:
            try:
                return self._analyze_llm(req)
            except Exception:
                pass
        return self._analyze_rules(req)

    def _analyze_rules(self, req: AnalyzeRequest) -> AnalyzeResponse:
        text = f"{req.title}\n{req.description}".lower()
        complexity = self._detect_complexity(text)
        roles = self._extract_roles(text)
        skills = self._extract_skills(text)
        budget = self._extract_budget(req.description)
        duration_days = self._extract_duration(req.description)
        leader_role = self._infer_leader_role(roles)

        return AnalyzeResponse(
            complexity=complexity,
            roles=roles,
            required_skills=skills,
            budget_estimate=budget,
            duration_estimate_days=duration_days,
            technical_leader_role=leader_role,
            summary=f"Projet {complexity}: {len(roles)} rôle(s), {len(skills)} compétence(s) identifiées.",
            analysis_source="rules",
        )

    def _detect_complexity(self, text: str) -> str:
        if COMPLEXITY_COMPLEX.search(text):
            return "complex"
        if COMPLEXITY_SIMPLE.search(text):
            return "simple"
        return "medium"

    def _extract_roles(self, text: str) -> list[str]:
        found = set()
        for role_name, keywords in ROLE_KEYWORDS.items():
            for kw in keywords:
                if kw in text:
                    label = role_name.replace("_", " ").title()
                    if role_name == "lead":
                        label = "Tech Lead / Architecte"
                    found.add(label)
        if not found:
            found.add("Développeur Full Stack")
        return sorted(found)

    def _extract_skills(self, text: str) -> list[str]:
        skills = set()
        for pattern in SKILL_PATTERNS:
            for m in re.finditer(pattern, text, re.I):
                skills.add(m.group(1).strip().lower())
        # Normaliser
        norm = []
        for s in sorted(skills):
            s = s.replace(" ", "").replace(".", "")
            if s and len(s) >= 2:
                norm.append(s)
        return norm[:30]

    def _extract_budget(self, description: str) -> Optional[BudgetEstimate]:
        m = BUDGET_PATTERN.search(description)
        if not m:
            return None
        try:
            amount = int(re.sub(r"\s", "", m.group(1)))
            currency = (m.group(2) or "USD").strip("$").upper() or "USD"
            if currency in ("EUR", "EURO"):
                currency = "EUR"
            elif currency == "DT":
                currency = "TND"
            else:
                currency = "USD"
            return BudgetEstimate(min_amount=amount * 0.8, max_amount=amount * 1.2, currency=currency)
        except (ValueError, IndexError):
            return None

    def _extract_duration(self, description: str) -> Optional[int]:
        # Chercher "X mois", "X weeks", "X days"
        for pattern in [
            r"(\d+)\s*(?:mois|months?)",
            r"(\d+)\s*(?:semaines?|weeks?)",
            r"(\d+)\s*(?:jours?|days?)",
        ]:
            m = re.search(pattern, description, re.I)
            if m:
                val = int(m.group(1))
                if "mois" in pattern or "month" in pattern:
                    return val * 30
                if "semaine" in pattern or "week" in pattern:
                    return val * 7
                return val
        return None

    def _infer_leader_role(self, roles: list[str]) -> Optional[str]:
        for r in roles:
            if "lead" in r.lower() or "architect" in r.lower():
                return r
        if roles:
            return roles[0]
        return "Tech Lead"

    def _analyze_llm(self, req: AnalyzeRequest) -> AnalyzeResponse:
        try:
            return self._call_llm(req)
        except Exception:
            return self._analyze_rules(req)

    def _call_llm(self, req: AnalyzeRequest) -> AnalyzeResponse:
        try:
            import httpx
        except ImportError:
            return self._analyze_rules(req)

        prompt = f"""Tu es un expert en analyse de projets techniques. À partir du titre et de la description suivants, réponds en JSON valide uniquement, sans texte avant ou après, avec les clés exactes : complexity (simple|medium|complex), roles (liste de rôles nécessaires, ex: Développeur Backend, UX Designer), required_skills (liste de compétences techniques), budget_min (nombre), budget_max (nombre), duration_days (nombre de jours), technical_leader_role (un rôle), summary (une phrase).

Titre: {req.title}
Description: {req.description}

JSON:"""

        url = f"{self._base_url.rstrip('/')}/chat/completions"
        payload = {
            "model": "gpt-3.5-turbo",
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.2,
        }
        headers = {"Content-Type": "application/json"}
        if self._api_key:
            headers["Authorization"] = f"Bearer {self._api_key}"

        with httpx.Client(timeout=30.0) as client:
            r = client.post(url, json=payload, headers=headers)
            r.raise_for_status()
            data = r.json()
        content = (data.get("choices") or [{}])[0].get("message", {}).get("content") or "{}"
        import json
        # Extraire le JSON du contenu (parfois entouré de ```json ... ```)
        content = content.strip()
        if "```" in content:
            start = content.find("{")
            end = content.rfind("}") + 1
            if start >= 0 and end > start:
                content = content[start:end]
        out = json.loads(content)

        return AnalyzeResponse(
            complexity=str(out.get("complexity", "medium")).lower()[:10] or "medium",
            roles=out.get("roles") or [],
            required_skills=out.get("required_skills") or [],
            budget_estimate=BudgetEstimate(
                min_amount=float(out.get("budget_min", 0)),
                max_amount=float(out.get("budget_max", 0)),
                currency="USD",
            ) if out.get("budget_min") is not None else None,
            duration_estimate_days=int(out["duration_days"]) if out.get("duration_days") is not None else None,
            technical_leader_role=out.get("technical_leader_role"),
            summary=out.get("summary"),
            analysis_source="llm",
        )
