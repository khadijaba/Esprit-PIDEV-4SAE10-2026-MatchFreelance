export interface Module {
  id: number;
  titre: string;
  description: string | null;
  dureeMinutes: number;
  ordre: number;
  formationId: number;
}

export interface ModuleRequest {
  titre: string;
  description: string;
  dureeMinutes: number;
  ordre: number;
  formationId: number;
}
