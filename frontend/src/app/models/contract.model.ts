/** Champs principaux renvoyés par GET /api/contracts/project/{id}. */
export interface ContractSummary {
  id: number;
  projectId: number;
  freelancerId?: number;
  clientId?: number;
  freelancerName?: string;
  status?: string;
  proposedBudget?: number;
  progressPercent?: number;
}
