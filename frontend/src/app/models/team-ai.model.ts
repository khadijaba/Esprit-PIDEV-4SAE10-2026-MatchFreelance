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

/** Requête pour calcul des freelancers compatibles (notification automatique). */
export interface ComputeCompatibleRequest {
  projectId: number;
  projectTitle: string;
  requiredSkills: string[];
}

/** Un freelancer sélectionné pour notification de compatibilité. */
export interface CompatibleFreelancerNotification {
  freelancerId: number;
  fullName?: string;
  email?: string;
  score: number;
  subject?: string;
  message: string;
}

/** Réponse du service Python compute-compatible-freelancers. */
export interface ComputeCompatibleResponse {
  notifications: CompatibleFreelancerNotification[];
  threshold: number;
}

export type ScheduleRiskLevel = 'OK' | 'WATCH' | 'AT_RISK';

/** Corps optionnel : si project + phases + deliverables sont fournis, calcul côté Python sans refetch gateway. */
export interface ScheduleOverrunRequest {
  projectId: number;
  project?: Record<string, unknown>;
  phases?: Record<string, unknown>[];
  deliverables?: Record<string, unknown>[];
}

export interface ScheduleOverrunBanner {
  title: string;
  message: string;
  severity: 'warning' | 'danger';
}

export interface ScheduleOverrunMetrics {
  timeElapsedRatio?: number | null;
  progressRatio?: number | null;
  gapProgressVsTime?: number | null;
  deliverablesAccepted?: number;
  deliverablesTotal?: number;
  overduePhasesCount?: number;
  methodVersion?: string;
}

/** Réponse de POST /api/ai/schedule-overrun-assessment (Team AI Python). */
export interface ScheduleOverrunResponse {
  scheduleRiskLevel: ScheduleRiskLevel;
  scheduleRiskScore0To100: number;
  summary: string;
  flags: string[];
  metrics: ScheduleOverrunMetrics;
  banner: ScheduleOverrunBanner | null;
  error?: string;
}

export interface DescriptionCoachHelp {
  whatItIs: string;
  purpose: string;
  howItWorks: string;
  limits: string;
}

export interface DescriptionCoachSnippets {
  scope: string;
  deliverables: string;
  acceptance: string;
  contextReminder: string;
}

/** POST /api/ai/description-coach — reformulation & checklist (LLM optionnel côté serveur). */
export interface DescriptionCoachRequest {
  title: string;
  description: string;
  useLlm?: boolean;
}

export interface DescriptionCoachResponse {
  methodVersion: string;
  help: DescriptionCoachHelp;
  gaps: string[];
  gapCodes: string[];
  checklist: string[];
  questionsToClarify: string[];
  suggestedSnippets: DescriptionCoachSnippets;
  draftEnrichedDescription: string;
  llmEnrichedMarkdown: string | null;
  llmUsed: boolean;
  /** ollama | openai si le LLM a répondu */
  llmBackend?: string | null;
  summary: string;
}

export interface PlanningPhaseProposal {
  phaseOrder: number;
  name: string;
  plannedDays: number;
  startDate?: string | null;
  dueDate?: string | null;
  milestones: string[];
  deliverables: { title: string; type: string }[];
  acceptanceCriteria: string[];
}

export interface PlanningAssistantInitialRequest {
  projectTitle: string;
  projectDescription: string;
  durationDays: number;
  startDate?: string | null;
  requiredSkills?: string[];
  useLlm?: boolean;
}

export interface PlanningAssistantAdjustRequest {
  currentPhases: Record<string, unknown>[];
  scheduleAssessment?: Record<string, unknown> | null;
  useLlm?: boolean;
}

export interface PlanningAssistantResponse {
  methodVersion: string;
  planKind: 'INITIAL' | 'ADJUSTED';
  llmUsed: boolean;
  llmBackend?: string | null;
  complexity?: string;
  summary: string;
  adjustmentReason?: Record<string, unknown>;
  recommendedActions?: string[];
  phases: PlanningPhaseProposal[];
}
