export type ContractStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface Contract {
  id: number;
  projectId: number;
  freelancerId: number;
  clientId: number;
  terms: string;
  proposedBudget?: number;
  applicationMessage?: string;
  status: ContractStatus;
  startDate: string;
  endDate: string;
  createdAt: string;
}

export interface ContractRequest {
  projectId: number;
  freelancerId: number;
  clientId: number;
  terms?: string;
  status?: ContractStatus;
  startDate?: string;
  endDate?: string;
}
