/**
 * Une entrée d'évolution de compétence (ajout ou progression de niveau).
 */
export interface SkillEvolutionEntry {
  id: string;
  freelancerId: number;
  skillId: number;
  skillName: string;
  category?: string;
  /** Niveau précédent (vide si nouvel ajout) */
  previousLevel?: string;
  /** Nouveau niveau (Junior, Intermediate, Advanced, Expert) */
  newLevel: string;
  date: string; // ISO
  /** manual = saisi par le freelancer, project = validé via projet réalisé */
  source: 'manual' | 'project';
}

/** Ordre des niveaux pour calcul de progression (1 = débutant, 4 = expert) */
export const LEVEL_ORDER: Record<string, number> = {
  BEGINNER: 1,
  JUNIOR: 1,
  INTERMEDIATE: 2,
  ADVANCED: 3,
  EXPERT: 4,
};

/** Libellés affichés (Backend utilise BEGINNER, INTERMEDIATE, etc.) */
export const LEVEL_LABELS: Record<string, string> = {
  BEGINNER: 'Junior',
  JUNIOR: 'Junior',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
  EXPERT: 'Expert',
};

export function getLevelLabel(level: string | undefined): string {
  if (!level) return '—';
  return LEVEL_LABELS[level.toUpperCase()] ?? level;
}

export function getLevelOrder(level: string | undefined): number {
  if (!level) return 0;
  return LEVEL_ORDER[level.toUpperCase()] ?? 0;
}
