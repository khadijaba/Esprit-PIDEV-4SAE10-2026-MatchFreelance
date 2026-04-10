export type ProjectStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

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

