export type ContractStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface Contract {
  id: number;
  projectId: number;
  freelancerId: number;
  clientId: number;
  freelancerName?: string | null;
  clientName?: string | null;
  terms?: string | null;
  proposedBudget?: number | null;
  extraTasksBudget?: number | null;
  applicationMessage?: string | null;
  status: ContractStatus;
  startDate?: string | null;
  endDate?: string | null;
  createdAt?: string | null;
  progressPercent?: number | null;
}
