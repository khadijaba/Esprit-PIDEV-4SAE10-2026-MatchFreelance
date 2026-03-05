export enum SkillCategory {
  WEB_DEVELOPMENT = 'WEB_DEVELOPMENT',
  MOBILE_DEVELOPMENT = 'MOBILE_DEVELOPMENT',
  DATA_SCIENCE = 'DATA_SCIENCE',
  DESIGN = 'DESIGN',
  MARKETING = 'MARKETING',
  WRITING = 'WRITING',
  VIDEO_EDITING = 'VIDEO_EDITING',
  PHOTOGRAPHY = 'PHOTOGRAPHY',
  CONSULTING = 'CONSULTING',
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
}

export interface SkillRequest {
  name: string;
  category: SkillCategory;
  freelancerId: number;
  /** Backend field name */
  level?: string;
  yearsOfExperience?: number;
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
