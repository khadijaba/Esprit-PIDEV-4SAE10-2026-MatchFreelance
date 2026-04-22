export interface FreelancerFitDto {
  freelancerId: number;
  estimatedDurationDays: number;
  durationLowDays: number;
  durationHighDays: number;
  successScore: number;
  confidence: string;
  pastMissionsConsidered: number;
  summary: string;
}

export interface FreelancerFitBatchDto {
  projectId: number;
  freelancers: FreelancerFitDto[];
}
