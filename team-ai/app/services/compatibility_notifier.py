"""
Notification automatique de compatibilité projet.

Score = (Compétences × 0,7) + (Expérience × 0,3). Note et projets réalisés non utilisés.
Les freelancers avec score >= 70% reçoivent une notification.
"""
import logging
import httpx
from app.config import get_settings

logger = logging.getLogger(__name__)
from app.models.schemas import (
    ComputeCompatibleRequest,
    ComputeCompatibleResponse,
    CompatibleFreelancerNotification,
)

WEIGHTS = {"skill_match": 0.7, "experience": 0.3}
MAX_YEARS_EXPERIENCE = 10
DEFAULT_THRESHOLD = 70


MIN_TOKEN_LENGTH = 2
MIN_SUBSTRING_LENGTH = 3


def _normalize_skill(s: str) -> str:
    return (s or "").strip().lower().replace("_", " ").replace("-", " ")


def _skill_matches_required(required_norm: str, freelancer_normalized: list[str]) -> bool:
    """Aligné frontend : match exact, sous-chaîne, ou par mots (ex: Node.js → node, js)."""
    if not required_norm or not freelancer_normalized:
        return False
    r = required_norm.strip().lower().replace("_", " ")
    f_set = {_.strip().lower().replace("_", " ") for _ in freelancer_normalized if _ and len(_.strip()) >= MIN_TOKEN_LENGTH}
    if not f_set:
        return False
    for f in f_set:
        if r == f:
            return True
        if len(r) >= MIN_SUBSTRING_LENGTH and len(f) >= MIN_SUBSTRING_LENGTH and (r in f or f in r):
            return True
    words = [w for w in r.replace(",", " ").split() if len(w.strip()) >= MIN_TOKEN_LENGTH]
    for word in words:
        if len(word) < MIN_SUBSTRING_LENGTH:
            continue
        for f in f_set:
            if word == f:
                return True
            if len(f) >= MIN_SUBSTRING_LENGTH and (word in f or f in word):
                return True
    return False


def _compute_skill_match(required: list[str], skill_names: list[str], skill_categories: list[str]) -> float:
    if not required:
        return 100.0
    required_norm = [_normalize_skill(s) for s in required if s]
    all_freelancer = [_normalize_skill(s) for s in skill_names if s] + [
        _normalize_skill(c) for c in skill_categories if c
    ]
    all_freelancer = [x for x in all_freelancer if x]
    matched = sum(1 for r in required_norm if _skill_matches_required(r, all_freelancer))
    return (matched / len(required_norm)) * 100.0 if required_norm else 100.0


def _compute_experience(skills: list[dict]) -> float:
    if not skills:
        return 0.0
    total = sum((s.get("yearsOfExperience") or s.get("years_of_experience") or 0) for s in skills)
    avg = total / len(skills)
    return min(100.0, (avg / MAX_YEARS_EXPERIENCE) * 100.0)


def _build_message(project_title: str, required_skills: list[str], score: int, project_id: int) -> str:
    skills_text = ", ".join(required_skills[:10]) if required_skills else "Voir le projet"
    if len(required_skills) > 10:
        skills_text += ", …"
    return (
        f"Nouveau projet compatible pour vous !\n"
        f"Titre : {project_title}\n"
        f"Compétences requises : {skills_text}\n"
        f"Score de compatibilité : {score}%\n"
        f"Cliquez ici pour voir le projet et postuler."
    )


class CompatibilityNotifier:
    """Calcule les freelancers compatibles avec un projet et prépare les notifications."""

    def __init__(self, backend_url: str | None = None):
        self.backend_url = (backend_url or get_settings().backend_url).rstrip("/")

    def compute(self, req: ComputeCompatibleRequest, threshold: int = DEFAULT_THRESHOLD) -> ComputeCompatibleResponse:
        """
        Récupère skills et users depuis le backend, calcule le score pour chaque freelancer,
        filtre score >= threshold, retourne la liste des notifications à envoyer.
        """
        # Normalize: accept comma-separated strings (e.g. ["java, angular, spring boot"] -> ["java", "angular", "spring boot"])
        raw_required = (req.required_skills or [])
        if isinstance(raw_required, str):
            raw_required = [raw_required]
        required = [part.strip() for s in raw_required for part in (s.split(",") if isinstance(s, str) else [str(s)]) if part.strip()]
        logger.info("compute_compatible: required_skills=%s (count=%d)", required, len(required))
        if not required:
            logger.warning("compute_compatible: no required skills, returning empty")
            return ComputeCompatibleResponse(notifications=[], threshold=threshold)

        with httpx.Client(timeout=15.0) as client:
            users = []
            for role_param in ("FREELANCER", "Freelancer", "freelancer"):
                try:
                    users_resp = client.get(f"{self.backend_url}/api/users", params={"role": role_param})
                    logger.info("GET /api/users?role=%s -> %s", role_param, users_resp.status_code)
                    users_resp.raise_for_status()
                    users_data = users_resp.json()
                    if isinstance(users_data, list):
                        users = users_data
                    elif isinstance(users_data, dict) and "content" in users_data:
                        users = users_data["content"]
                    else:
                        users = users_data if isinstance(users_data, list) else []
                    if users:
                        break
                except Exception as e:
                    logger.debug("GET /api/users?role=%s failed: %s", role_param, e)
                    continue
            if not users:
                try:
                    users_resp = client.get(f"{self.backend_url}/api/users")
                    users_resp.raise_for_status()
                    users_data = users_resp.json()
                    if isinstance(users_data, list):
                        all_users = users_data
                    elif isinstance(users_data, dict) and "content" in users_data:
                        all_users = users_data["content"]
                    else:
                        all_users = []
                    role_lower = {str(u.get("role", "")).lower() for u in all_users}
                    if "freelancer" in role_lower:
                        users = [u for u in all_users if str(u.get("role", "")).lower() == "freelancer"]
                except Exception:
                    pass

            all_skills: list[dict] = []
            try:
                skills_resp = client.get(f"{self.backend_url}/api/skills")
                logger.info("GET /api/skills -> %s", skills_resp.status_code)
                skills_resp.raise_for_status()
                raw = skills_resp.json()
                if isinstance(raw, list):
                    all_skills = raw
                elif isinstance(raw, dict) and "content" in raw:
                    all_skills = raw["content"] if isinstance(raw["content"], list) else []
                else:
                    all_skills = []
                if all_skills:
                    sample = all_skills[0]
                    logger.info("all_skills sample keys: %s, has freelancerId: %s", list(sample.keys()), "freelancerId" in sample or "freelancer_id" in sample)
            except Exception as e:
                logger.warning("GET /api/skills failed: %s", e)
                all_skills = []

            by_freelancer: dict[int, list[dict]] = {}
            for s in all_skills:
                fid = s.get("freelancerId") or s.get("freelancer_id")
                if fid is not None:
                    fid = int(fid) if not isinstance(fid, int) else fid
                    if fid not in by_freelancer:
                        by_freelancer[fid] = []
                    by_freelancer[fid].append(s)

            if not by_freelancer and users:
                for u in users:
                    uid = u.get("id")
                    if uid is None:
                        continue
                    uid = int(uid) if not isinstance(uid, int) else uid
                    try:
                        fr_resp = client.get(f"{self.backend_url}/api/skills/freelancer/{uid}")
                        fr_resp.raise_for_status()
                        raw = fr_resp.json()
                        if isinstance(raw, list) and raw:
                            by_freelancer[uid] = raw
                        elif isinstance(raw, dict):
                            lst = raw.get("skills") or raw.get("content") or raw.get("data")
                            if isinstance(lst, list) and lst:
                                by_freelancer[uid] = lst
                    except Exception as e:
                        logger.debug("fetch skills freelancer %s: %s", uid, e)
                        continue

        logger.info("compute_compatible: users=%d, by_freelancer=%d (freelancer ids: %s)", len(users), len(by_freelancer), list(by_freelancer.keys())[:20])

        def _user_id(u: dict):
            i = u.get("id")
            return int(i) if i is not None else None

        users_by_id = {_user_id(u): u for u in users if _user_id(u) is not None}
        notifications: list[CompatibleFreelancerNotification] = []

        for fid, fl_skills in by_freelancer.items():
            user = users_by_id.get(int(fid) if not isinstance(fid, int) else fid)

            skill_names = []
            for s in fl_skills:
                name = (s.get("name") or s.get("skillName") or s.get("title") or "").strip()
                if name:
                    skill_names.append(name)
            skill_categories = [str(s.get("category") or "") for s in fl_skills]

            skill_match = _compute_skill_match(required, skill_names, skill_categories)
            experience = _compute_experience(fl_skills)

            score = round(
                (skill_match / 100.0) * WEIGHTS["skill_match"] * 100
                + (experience / 100.0) * WEIGHTS["experience"] * 100
            )
            score = min(100, max(0, score))
            logger.info("compute_compatible: fid=%s skill_match=%.0f experience=%.0f score=%s %s", fid, skill_match, experience, score, "OK" if score >= threshold else "skip")

            if score < threshold:
                continue

            if user:
                full_name = (
                    (user.get("fullName") or user.get("full_name") or user.get("username") or user.get("email") or f"Freelancer #{fid}")
                )
                full_name = full_name.strip() or f"Freelancer #{fid}" if isinstance(full_name, str) else f"Freelancer #{fid}"
                email = user.get("email")
            else:
                full_name = f"Freelancer #{fid}"
                email = None

            message = _build_message(req.project_title, required, score, req.project_id)
            notifications.append(
                CompatibleFreelancerNotification(
                    freelancerId=fid,
                    fullName=full_name,
                    email=email,
                    score=score,
                    subject="Nouveau projet compatible pour vous !",
                    message=message,
                )
            )

        notifications.sort(key=lambda x: -x.score)
        return ComputeCompatibleResponse(notifications=notifications, threshold=threshold)
