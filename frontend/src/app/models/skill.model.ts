export type SkillCategory =
  | 'WEB_DEVELOPMENT'
  | 'MOBILE_DEVELOPMENT'
  | 'DATA_SCIENCE'
  | 'DEVOPS'
  | 'CYBERSECURITY'
  | 'DESIGN'
  | 'AI';

export interface Skill {
  id: number;
  name: string;
  category: SkillCategory;
  freelancerId: number;
  level?: string;
  yearsOfExperience?: number;
  createdAt?: string;
}

export interface SkillRequest {
  name: string;
  category: SkillCategory;
  freelancerId: number;
  level?: string;
  yearsOfExperience?: number;
}

export const SKILL_CATEGORY_LABELS: Record<SkillCategory, string> = {
  WEB_DEVELOPMENT: 'Développement Web',
  MOBILE_DEVELOPMENT: 'Développement Mobile',
  DATA_SCIENCE: 'Data Science',
  DEVOPS: 'DevOps',
  CYBERSECURITY: 'Cybersécurité',
  DESIGN: 'Design',
  AI: 'Intelligence Artificielle',
};
