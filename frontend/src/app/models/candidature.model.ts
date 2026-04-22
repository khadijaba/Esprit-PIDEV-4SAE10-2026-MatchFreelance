export type CandidatureStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Candidature {
  id: number;
  projectId: number;
  freelancerId: number;
  freelancerName?: string | null;
  message?: string | null;
  proposedBudget?: number | null;
  extraTasksBudget?: number | null;
  status: CandidatureStatus;
  createdAt?: string | null;
  aiMatchScore?: number | null;
  aiInsights?: string | null;
  interviewCount?: number | null;
  eligibleForAcceptance?: boolean | null;
}

export interface CandidatureCreateRequest {
  projectId: number;
  freelancerId: number;
  message?: string;
  proposedBudget: number;
  extraTasksBudget?: number;
}

export interface Interview {
  id: number;
  candidatureId: number;
  scheduledAt: string;
  status: InterviewStatus;
  notes?: string | null;
}

export interface InterviewScheduleRequest {
  scheduledAt: string;
  notes?: string;
  status?: InterviewStatus;
}
