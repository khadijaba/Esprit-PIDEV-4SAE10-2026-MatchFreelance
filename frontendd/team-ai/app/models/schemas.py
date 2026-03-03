"""
Schémas Pydantic pour les requêtes/réponses de l'API Team AI.
"""
from typing import Optional
from pydantic import BaseModel, Field


# --- Analyse de projet ---
class AnalyzeRequest(BaseModel):
    title: str = Field(..., min_length=1, description="Titre du projet")
    description: str = Field(..., min_length=1, description="Description du projet")


class BudgetEstimate(BaseModel):
    min_amount: float = Field(..., alias="minAmount", description="Budget min estimé")
    max_amount: float = Field(..., alias="maxAmount", description="Budget max estimé")
    currency: str = "USD"

    class Config:
        populate_by_name = True


class AnalyzeResponse(BaseModel):
    complexity: str = Field(..., description="simple | medium | complex")
    roles: list[str] = Field(default_factory=list, description="Rôles nécessaires (ex: Développeur Backend, UX Designer)")
    required_skills: list[str] = Field(default_factory=list, alias="requiredSkills")
    budget_estimate: Optional[BudgetEstimate] = Field(None, alias="budgetEstimate")
    duration_estimate_days: Optional[int] = Field(None, alias="durationEstimateDays", ge=1, le=3650)
    technical_leader_role: Optional[str] = Field(None, alias="technicalLeaderRole")
    summary: Optional[str] = Field(None, description="Résumé court de l'analyse")

    class Config:
        populate_by_name = True


# --- Construction d'équipe ---
class FreelancerInput(BaseModel):
    id: int = Field(..., alias="freelancerId")
    full_name: Optional[str] = Field(None, alias="fullName")
    email: Optional[str] = None
    skills: list[str] = Field(default_factory=list)  # noms de compétences
    years_of_experience: Optional[float] = Field(None, alias="yearsOfExperience")
    rating: Optional[float] = Field(None, ge=0, le=5)
    completed_projects: Optional[int] = Field(None, alias="completedProjects", ge=0)

    class Config:
        populate_by_name = True


class ProjectAnalysisInput(BaseModel):
    complexity: str = "medium"
    roles: list[str] = Field(default_factory=list)
    required_skills: list[str] = Field(default_factory=list, alias="requiredSkills")
    technical_leader_role: Optional[str] = Field(None, alias="technicalLeaderRole")

    class Config:
        populate_by_name = True


class BuildTeamRequest(BaseModel):
    project_id: int = Field(..., alias="projectId")
    project_title: str = Field(..., alias="projectTitle")
    project_analysis: ProjectAnalysisInput = Field(..., alias="projectAnalysis")
    freelancers: list[FreelancerInput] = Field(default_factory=list)
    max_team_size: int = Field(8, alias="maxTeamSize", ge=1, le=20)

    class Config:
        populate_by_name = True


class TeamMemberProposal(BaseModel):
    freelancer_id: int = Field(..., alias="freelancerId")
    role: str = Field(..., description="Rôle assigné")
    is_leader: bool = Field(False, alias="isLeader")
    score: float = Field(..., ge=0, le=100, description="Score d'adéquation 0-100")
    match_rationale: Optional[str] = Field(None, alias="matchRationale")

    class Config:
        populate_by_name = True


class NotificationToSend(BaseModel):
    freelancer_id: int = Field(..., alias="freelancerId")
    email: Optional[str] = None
    subject: Optional[str] = None
    message: str = Field(..., description="Message à envoyer au freelancer")

    class Config:
        populate_by_name = True


class BuildTeamResponse(BaseModel):
    team: list[TeamMemberProposal] = Field(default_factory=list)
    technical_leader_id: Optional[int] = Field(None, alias="technicalLeaderId")
    notifications_to_send: list[NotificationToSend] = Field(default_factory=list, alias="notificationsToSend")
    rationale: Optional[str] = Field(None, description="Explication globale de la composition")

    class Config:
        populate_by_name = True
