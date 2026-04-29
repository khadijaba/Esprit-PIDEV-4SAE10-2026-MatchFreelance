export type ProjectPhaseStatus = 'PLANNED' | 'IN_PROGRESS' | 'IN_REVIEW' | 'APPROVED' | 'BLOCKED';

export interface ProjectPhase {
  id: number;
  name: string;
  description: string | null;
  phaseOrder: number;
  status: ProjectPhaseStatus;
  startDate: string | null;
  dueDate: string | null;
  approvedAt: string | null;
}

export interface DecisionCopilotResponse {
  recommendation: string;
  confidence: number | null;
  summary: string | null;
  reasons: string[] | null;
  suggestedActions: string[] | null;
  ownerMessageDraft: string | null;
}
