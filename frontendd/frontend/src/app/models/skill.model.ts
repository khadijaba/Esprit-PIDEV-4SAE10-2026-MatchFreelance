/** Catégories alignées avec les formations : Développement Web, Mobile, IA, Data Science, DevOps, Cybersécurité, Design */
export enum SkillCategory {
  WEB_DEVELOPMENT = 'WEB_DEVELOPMENT',
  MOBILE_DEVELOPMENT = 'MOBILE_DEVELOPMENT',
  AI = 'AI',
  DATA_SCIENCE = 'DATA_SCIENCE',
  DEVOPS = 'DEVOPS',
  CYBERSECURITY = 'CYBERSECURITY',
  DESIGN = 'DESIGN',
  OTHER = 'OTHER'
}

export interface Skill {
  id: number;
  name: string;
  category: SkillCategory;
  freelancerId: number;
  /** Backend uses "level"; use for display as proficiency */
  level?: string;
  yearsOfExperience?: number;
  createdAt?: string;
  /** Supervision admin : compétence bloquée (non supprimée) */
  blocked?: boolean;
}

export interface SkillRequest {
  name: string;
  category: SkillCategory;
  freelancerId: number;
  /** Backend field name */
  level?: string;
  yearsOfExperience?: number;
  /** Supervision admin */
  blocked?: boolean;
}

export interface Portfolio {
  id: number;
  freelancerId: number;
  portfolioUrl?: string;
  portfolioDescription?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface FreelancerProfile {
  id: number;
  freelancerId: number;
  cvFilePath?: string;
  cvFileName?: string;
  portfolioUrl?: string;
  portfolioDescription?: string;
  createdAt: string;
  updatedAt: string;
}
