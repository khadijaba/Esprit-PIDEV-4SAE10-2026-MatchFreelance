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
