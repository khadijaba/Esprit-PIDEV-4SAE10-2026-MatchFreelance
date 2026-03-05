export type CandidatureStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface Candidature {
  id: number;
  projectId: number;
  freelancerId: number;
  freelancerName?: string;
  message: string;
  proposedBudget: number;
  extraTasksBudget?: number;
  status: CandidatureStatus;
  createdAt: string;
  aiMatchScore?: number;
  aiInsights?: string;
}

export interface CandidatureRequest {
  projectId: number;
  freelancerId: number;
  message?: string;
  proposedBudget: number;
  extraTasksBudget?: number;
}
