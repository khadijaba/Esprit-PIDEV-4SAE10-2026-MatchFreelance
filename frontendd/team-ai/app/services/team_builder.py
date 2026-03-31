"""
Construction d'équipe optimale : sélection des meilleurs freelancers complémentaires
et désignation d'un leader technique à partir de l'analyse du projet.
"""
from app.models.schemas import (
    BuildTeamRequest,
    BuildTeamResponse,
    FreelancerInput,
    TeamMemberProposal,
    NotificationToSend,
    ProjectAnalysisInput,
)


def _normalize_skill(s: str) -> str:
    return (s or "").strip().lower().replace("_", " ").replace("-", " ")


def _skill_match_score(required: list[str], freelancer_skills: list[str]) -> float:
    if not required:
        return 100.0
    req_set = {_normalize_skill(s) for s in required}
    fl_set = {_normalize_skill(s) for s in freelancer_skills}
    matches = sum(1 for r in req_set if any(r in f or f in r for f in fl_set))
    return (matches / len(req_set)) * 100.0 if req_set else 100.0


def _role_fit(freelancer_skills: list[str], role: str) -> float:
    role_lower = _normalize_skill(role)
    fl_set = {_normalize_skill(s) for s in freelancer_skills}
    if any(role_lower in f or f in role_lower for f in fl_set):
        return 1.0
    if "backend" in role_lower and any(k in " ".join(fl_set) for k in ("java", "spring", "node", "api")):
        return 0.8
    if "frontend" in role_lower and any(k in " ".join(fl_set) for k in ("angular", "react", "vue", "ui")):
        return 0.8
    if "lead" in role_lower or "architect" in role_lower:
        return 0.7 if len(freelancer_skills) >= 3 else 0.4
    return 0.5


class TeamBuilder:
    """Sélectionne les freelancers et propose un leader à partir de l'analyse projet."""

    def build(self, req: BuildTeamRequest) -> BuildTeamResponse:
        analysis = req.project_analysis
        freelancers = req.freelancers
        max_size = min(req.max_team_size, len(freelancers)) or 1

        if not freelancers:
            return BuildTeamResponse(
                team=[],
                technical_leader_id=None,
                notifications_to_send=[],
                rationale="Aucun freelancer disponible pour constituer l'équipe.",
            )

        # Score chaque freelancer : adéquation compétences + rôle leader potentiel
        required_skills = analysis.required_skills or []
        roles = analysis.roles or ["Développeur"]
        leader_role = (analysis.technical_leader_role or "").strip() or (roles[0] if roles else "Tech Lead")

        scored = []
        for fl in freelancers:
            skill_score = _skill_match_score(required_skills, fl.skills)
            exp_bonus = min(20, (fl.years_of_experience or 0) * 2)
            rating_bonus = min(10, (fl.rating or 0) * 2)
            projects_bonus = min(10, (fl.completed_projects or 0) // 2)
            leader_fit = _role_fit(fl.skills, leader_role)
            total = skill_score * 0.6 + exp_bonus + rating_bonus + projects_bonus + leader_fit * 10
            scored.append((total, fl, leader_fit))

        scored.sort(key=lambda x: (-x[0], -x[2]))

        # Équipe : prendre les meilleurs jusqu'à max_team_size
        team: list[TeamMemberProposal] = []
        leader_id: int | None = None
        assigned_roles = list(roles) if roles else ["Développeur"]

        for i, (_, fl, leader_fit) in enumerate(scored[:max_size]):
            role_idx = min(i, len(assigned_roles) - 1)
            role = assigned_roles[role_idx]
            is_leader = leader_id is None and (leader_fit >= 0.6 or i == 0)
            if is_leader:
                leader_id = fl.id
                role = leader_role or role
            score = min(100.0, _skill_match_score(required_skills, fl.skills) + (10 if is_leader else 0))
            team.append(
                TeamMemberProposal(
                    freelancer_id=fl.id,
                    role=role,
                    is_leader=is_leader,
                    score=round(score, 1),
                    match_rationale=f"Compétences alignées avec le projet; rôle {role}.",
                )
            )

        if leader_id is None and team:
            first = team[0]
            team[0] = TeamMemberProposal(
                freelancer_id=first.freelancer_id,
                role=leader_role or first.role,
                is_leader=True,
                score=first.score,
                match_rationale=first.match_rationale,
            )
            leader_id = first.freelancer_id

        # Notifications à envoyer à chaque membre
        notifications: list[NotificationToSend] = []
        for prop in team:
            fl = next((f for f in freelancers if f.id == prop.freelancer_id), None)
            if not fl:
                continue
            subject = f"Proposition d'équipe – {req.project_title}"
            msg = (
                f"Bonjour,\n\nVous avez été sélectionné pour le projet « {req.project_title} » "
                f"en tant que {prop.role}{' (leader technique)' if prop.is_leader else ''}.\n\n"
                "Connectez-vous à la plateforme pour accepter ou refuser l'invitation."
            )
            notifications.append(
                NotificationToSend(
                    freelancer_id=prop.freelancer_id,
                    email=fl.email,
                    subject=subject,
                    message=msg,
                )
            )

        rationale = (
            f"Équipe de {len(team)} membre(s) sélectionnée selon les compétences requises "
            f"({', '.join(required_skills[:5])}{'...' if len(required_skills) > 5 else ''}). "
            f"Leader technique proposé : freelancer #{leader_id}."
        )

        return BuildTeamResponse(
            team=team,
            technical_leader_id=leader_id,
            notifications_to_send=notifications,
            rationale=rationale,
        )
