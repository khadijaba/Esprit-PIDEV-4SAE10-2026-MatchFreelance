export interface ProjectInsights {
  projectId: number;
  listingQualityScore: number;
  marketAttractivenessScore: number;
  compositeHealthScore: number;
  successLikelihoodScore: number;
  nicheDifficulty: string;
  estimatedDaysToHireLow: number;
  estimatedDaysToHireHigh: number;
  dailyRate: number;
  applicationCount: number | null;
  ownerCompletedProjects: number;
  ownerOpenProjects: number;
  ownerTotalProjects: number;
  riskLevel: string;
  flags: string[];
  summary: string;
  modelVersion: string;
  computedAt: string;
}
