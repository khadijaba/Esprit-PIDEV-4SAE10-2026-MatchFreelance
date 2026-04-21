export type CandidatureStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN' | string;

export interface CandidatureResponse {
  id: number;
  projectId: number;
  freelancerId: number;
  freelancerName: string;
  message: string;
  proposedBudget: number;
  extraTasksBudget: number;
  status: CandidatureStatus;
  createdAt: string;
  aiMatchScore: number | null;
  aiInsights: string | null;
  interviewCount: number | null;
  eligibleForAcceptance: boolean | null;
}

export interface CandidatureRequest {
  projectId: number;
  freelancerId: number;
  message?: string;
  proposedBudget: number;
  extraTasksBudget?: number;
}
