import type { CandidatureStatus } from './candidature.model';

export interface ScoreBreakdown {
  aiMatchContribution: number;
  budgetCompetitivenessContribution: number;
  responseSpeedContribution: number;
  proposalQualityContribution: number;
  pitchMatchContribution?: number;
  /** Weighted contribution from chat communication (how freelancer responds in contract chats). */
  chatCommunicationContribution?: number;
  formulaDescription: string;
}

export interface RankedCandidature {
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
  compositeScore: number;
  scoreBreakdown?: ScoreBreakdown;
  rank: number;
}

export interface BudgetStats {
  projectId: number;
  candidatureCount: number;
  minProposedBudget?: number;
  maxProposedBudget?: number;
  averageProposedBudget?: number;
  medianProposedBudget?: number;
  percentile25?: number;
  percentile75?: number;
  recommendedMin?: number;
  recommendedMax?: number;
  standardDeviation?: number;
}
