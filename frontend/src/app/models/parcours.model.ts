export interface SkillDto {
  id: number;
  name: string;
  category: string;
  freelancerId: number;
  level?: string;
  yearsOfExperience?: number;
}

export interface FormationProposeeDto {
  id: number;
  titre: string;
  typeFormation: string;
  description?: string;
  dureeHeures?: number;
  statut?: string;
}

export interface ParcoursIntelligentResponse {
  freelancerId: number;
  competencesActuelles: SkillDto[];
  categoriesActuelles: string[];
  gapsDetectes: string[];
  formationsProposees: FormationProposeeDto[];
}
