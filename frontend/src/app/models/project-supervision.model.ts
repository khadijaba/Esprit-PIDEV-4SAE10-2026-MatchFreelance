export type ProjectPhaseStatus = 'PLANNED' | 'IN_PROGRESS' | 'IN_REVIEW' | 'APPROVED' | 'BLOCKED';
export type DeliverableType = 'DOC' | 'DESIGN' | 'CODE' | 'DEMO' | 'REPORT';
export type DeliverableReviewStatus = 'PENDING' | 'CHANGES_REQUESTED' | 'ACCEPTED';
export type PhaseMeetingDecision = 'GO' | 'GO_WITH_CHANGES' | 'NO_GO';

export interface ProjectPhase {
  id: number;
  name: string;
  description?: string | null;
  phaseOrder: number;
  status: ProjectPhaseStatus;
  startDate?: string | null;
  dueDate?: string | null;
  approvedAt?: string | null;
}

export interface CreatePhaseRequest {
  name: string;
  description?: string;
  phaseOrder: number;
  startDate?: string;
  dueDate?: string;
}

export interface PhaseDeliverable {
  id: number;
  title: string;
  description?: string | null;
  type: DeliverableType;
  reviewStatus: DeliverableReviewStatus;
  reviewComment?: string | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;
}

export interface CreateDeliverableRequest {
  title: string;
  description?: string;
  type: DeliverableType;
}

export interface ReviewDeliverableRequest {
  reviewStatus: DeliverableReviewStatus;
  reviewComment?: string;
}

export interface PhaseMeeting {
  id: number;
  meetingAt: string;
  agenda?: string | null;
  summary?: string | null;
  decision?: PhaseMeetingDecision | null;
}

export interface CreatePhaseMeetingRequest {
  meetingAt: string;
  agenda?: string;
  summary?: string;
  decision?: PhaseMeetingDecision;
}

export type DecisionCopilotRecommendation =
  | 'NO_ACTION'
  | 'REQUEST_CHANGES'
  | 'ACCEPT_PHASE'
  | 'SPLIT_PHASE'
  | 'ESCALATE_RISK';

export interface DecisionCopilotResponse {
  recommendation: DecisionCopilotRecommendation;
  confidence: number;
  summary: string;
  reasons: string[];
  suggestedActions: string[];
  ownerMessageDraft: string;
}
