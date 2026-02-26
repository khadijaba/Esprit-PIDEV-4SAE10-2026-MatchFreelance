export type ProjectStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ContractSummary {
  id: number;
  projectId: number;
  freelancerId: number;
  clientId: number;
  terms: string;
  proposedBudget?: number;
  applicationMessage?: string;
  status: string;
  startDate?: string;
  endDate?: string;
  createdAt: string;
}

export interface Project {
  id: number;
  title: string;
  description: string;
  minBudget: number;
  maxBudget: number;
  duration: number;
  createdAt: string;
  status: ProjectStatus;
  clientId?: number;
  contracts?: ContractSummary[];
}

export interface ProjectRequest {
  title: string;
  description: string;
  minBudget: number;
  maxBudget: number;
  duration: number;
  status?: ProjectStatus;
  clientId?: number;
}

