export interface PaymentMilestone {
  milestoneIndex: number;
  progressPercentTrigger: number;
  amount: number;
  released: boolean;
  statusDescription: string;
}

export interface FinancialSummary {
  contractId: number;
  baseBudget: number;
  extraTasksBudget: number;
  totalContractValue: number;
  platformFeePercent: number;
  platformFeeAmount: number;
  freelancerNetAmount: number;
  clientTotalAmount: number;
  amountReleasableByProgress: number;
  paymentSchedule: PaymentMilestone[];
}

export interface ContractHealth {
  contractId: number;
  healthScore: number;
  healthLevel: string;
  flags: string[];
  timelineStatus: string;
  progressVsExpectedRatio?: number;
}

/** Matches contract-service ContractAiBriefingDTO (local Ollama briefing). */
export interface ContractAiBriefing {
  contractId: number;
  summary: string;
  timelineRisk: string;
  chatTone: string;
  suggestedNextSteps: string[];
  disclaimer: string;
  model?: string;
  generatedAt?: string;
  fallback: boolean;
}

/** Matches contract-service ExtraBudgetAiAnalysisDTO (extra-budget proposal review). */
export interface ExtraBudgetAiAnalysis {
  contractId: number;
  needAssessment: string;
  needRationale: string;
  priceAssessment: string;
  priceRationale: string;
  projectFit: string;
  projectFitRationale: string;
  risksOrConcerns: string[];
  negotiationTips: string[];
  overallSummary: string;
  disclaimer: string;
  model?: string;
  generatedAt?: string;
  fallback: boolean;
}
