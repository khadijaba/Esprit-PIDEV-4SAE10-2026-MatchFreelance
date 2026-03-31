import { Formation } from './formation.model';

/**
 * Résultat du Smart Career Path Generator.
 */
export interface SkillDemand {
  skill: string;
  projectCount: number;
  percentOfProjects: number;
  avgBudget: number;
  totalBudget: number;
}

export interface CareerPathRecommendation {
  /** Compétences très demandées que le freelancer n'a pas */
  missingSkillsWithDemand: SkillDemand[];
  /** Formations recommandées (liées à ces compétences) */
  suggestedFormations: { formation: Formation; coversSkills: string[] }[];
  /** Message d'impact (ex. "Si vous ajoutez Docker + Kubernetes → +35% opportunités") */
  impactMessage: string;
  /** Stats globales marché (nombre total de projets analysés) */
  totalProjectsAnalyzed: number;
}
