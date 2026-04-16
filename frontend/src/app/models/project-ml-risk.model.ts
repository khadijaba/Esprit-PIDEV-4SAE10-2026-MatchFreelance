export interface ProjectMlRisk {
  projectId: number;
  riskScore0To100: number;
  riskLevel: string;
  probabilityHighRisk: number;
  flags: string[];
  summary: string;
  modelId: string;
  heuristicFallback: boolean;
}
