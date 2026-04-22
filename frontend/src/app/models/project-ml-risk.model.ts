export interface ProjectMlRisk {
  projectId: number;
  riskScore0To100: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  probabilityHighRisk: number;
  flags: string[];
  summary: string;
  modelId: string;
  heuristicFallback: boolean;
}
