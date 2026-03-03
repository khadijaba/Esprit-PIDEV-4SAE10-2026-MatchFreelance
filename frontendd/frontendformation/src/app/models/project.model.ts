export type ProjectStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface Project {
  id: number;
  title: string;
  description: string;
  budget: number;
  duration: number;
  createdAt: string;
  status: ProjectStatus;
}

export interface ProjectRequest {
  title: string;
  description: string;
  budget: number;
  duration: number;
  status?: ProjectStatus;
}

