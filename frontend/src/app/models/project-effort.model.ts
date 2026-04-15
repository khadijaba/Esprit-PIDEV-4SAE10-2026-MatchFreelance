export interface ProjectEffortEstimate {
  projectId: number;
  estimatedManDays: number;
  estimatedManDaysLow: number;
  estimatedManDaysHigh: number;
  declaredDurationDays: number;
  impliedCapacityOneFteDays: number;
  fteRequiredVsDeclared: number;
  flags: string[];
  summary: string;
  methodVersion: string;
}
