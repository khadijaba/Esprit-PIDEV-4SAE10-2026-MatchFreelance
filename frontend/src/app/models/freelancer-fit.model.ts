export interface FreelancerFitDto {
  freelancerId: number;
  estimatedDurationDays: number;
  durationLowDays: number;
  durationHighDays: number;
  successScore: number;
  /** Affichage : formule (Compétences × 0,7) + (Expérience × 0,3). */
  competencesPercent?: number;
  experiencePercent?: number;
  weightedMatchPercent?: number;
  confidence: string;
  pastMissionsConsidered: number;
  summary: string;
}

export interface FreelancerFitBatchDto {
  projectId: number;
  freelancers: FreelancerFitDto[];
}
