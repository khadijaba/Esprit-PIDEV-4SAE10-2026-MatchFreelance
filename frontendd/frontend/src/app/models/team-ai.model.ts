/**
 * Modèles pour le service Team AI (analyse projet + construction d'équipe).
 */

export interface AnalyzeProjectRequest {
  title: string;
  description: string;
}

export interface BudgetEstimate {
  minAmount: number;
  maxAmount: number;
  currency: string;
}

export interface AnalyzeProjectResponse {
  complexity: 'simple' | 'medium' | 'complex';
  roles: string[];
  requiredSkills: string[];
  budgetEstimate?: BudgetEstimate;
  durationEstimateDays?: number;
  technicalLeaderRole?: string;
  summary?: string;
}

export interface FreelancerForTeam {
  freelancerId: number;
  fullName?: string;
  email?: string;
  skills: string[];
  yearsOfExperience?: number;
  rating?: number;
  completedProjects?: number;
}

export interface ProjectAnalysisForTeam {
  complexity: string;
  roles: string[];
  requiredSkills: string[];
  technicalLeaderRole?: string;
}

export interface BuildTeamRequest {
  projectId: number;
  projectTitle: string;
  projectAnalysis: ProjectAnalysisForTeam;
  freelancers: FreelancerForTeam[];
  maxTeamSize?: number;
}

export interface TeamMemberProposal {
  freelancerId: number;
  role: string;
  isLeader: boolean;
  score: number;
  matchRationale?: string;
}

export interface NotificationToSend {
  freelancerId: number;
  email?: string;
  subject?: string;
  message: string;
}

export interface BuildTeamResponse {
  team: TeamMemberProposal[];
  technicalLeaderId?: number;
  notificationsToSend: NotificationToSend[];
  rationale?: string;
}
