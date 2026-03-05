export type ContractStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface Contract {
  id: number;
  projectId: number;
  freelancerId: number;
  clientId: number;
  freelancerName?: string;
  clientName?: string;
  terms: string;
  proposedBudget?: number;
  extraTasksBudget?: number;
  applicationMessage?: string;
  status: ContractStatus;
  startDate: string;
  endDate: string;
  createdAt: string;
  pendingExtraAmount?: number;
  pendingExtraReason?: string;
  pendingExtraRequestedAt?: string;
  progressPercent?: number;
  clientRating?: number;
  clientReview?: string;
  clientReviewedAt?: string;
}

export interface ContractRequest {
  projectId: number;
  freelancerId: number;
  clientId: number;
  terms?: string;
  proposedBudget?: number;
  extraTasksBudget?: number;
  status?: ContractStatus;
  startDate?: string;
  endDate?: string;
}
