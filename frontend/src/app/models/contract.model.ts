/** Aligné sur ContractResponseDTO (microservice Contract). */
export type ContractStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | string;

export interface Contract {
  id: number;
  projectId: number;
  freelancerId?: number;
  clientId?: number;
  freelancerName?: string;
  clientName?: string;
  terms?: string;
  proposedBudget?: number;
  extraTasksBudget?: number;
  applicationMessage?: string;
  status?: ContractStatus;
  startDate?: string;
  endDate?: string;
  createdAt?: string;
  pendingExtraAmount?: number | null;
  pendingExtraReason?: string | null;
  pendingExtraRequestedAt?: string | null;
  progressPercent?: number | null;
  clientRating?: number | null;
  clientReview?: string | null;
  clientReviewedAt?: string | null;
}

/** Alias rétrocompatible (liste projet / recrutement). */
export type ContractSummary = Contract;

export interface ContractAmendPayload {
  projectId: number;
  freelancerId: number;
  clientId: number;
  terms?: string;
  proposedBudget?: number;
  extraTasksBudget?: number;
  applicationMessage?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
}
