export type WorkspaceStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';
export type WorkspaceAccessLevel = 'PRIVATE' | 'SHARED';
export type MilestoneStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'COMPLETED' | 'OVERDUE';

export interface Workspace {
  id: number;
  name: string;
  description: string;
  accessLevel: WorkspaceAccessLevel;
  status: WorkspaceStatus;
  contractId: number;
  ownerId: number;
  createdAt: string;
  updatedAt: string;
  milestones: Milestone[];
  totalMilestones: number;
  completedMilestones: number;
  overallProgress: number;
}

export interface CreateWorkspaceRequest {
  name: string;
  description?: string;
  accessLevel: WorkspaceAccessLevel;
  contractId: number;
  ownerId: number;
}

export interface Milestone {
  id: number;
  title: string;
  description: string;
  deadline: string;
  progress: number;
  budgetAllocated: number;
  status: MilestoneStatus;
  completedAt: string | null;
  workspaceId: number;
  assigneeId: number | null;
  evaluationId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMilestoneRequest {
  title: string;
  description?: string;
  deadline?: string;
  budgetAllocated?: number;
  status?: MilestoneStatus;
  workspaceId: number;
  assigneeId?: number;
}
