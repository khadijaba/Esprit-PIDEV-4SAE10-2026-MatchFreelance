/**
 * Résultat du Talent Matching Engine pour un freelancer.
 * Score = (SkillMatch * 0.5) + (Experience * 0.2) + (UserRating * 0.2) + (CompletedProjects * 0.1)
 */
export interface MatchingFreelancer {
  freelancerId: number;
  fullName: string;
  email?: string;
  /** Score global 0–100 % */
  score: number;
  /** Adéquation compétences requises / compétences du freelancer (0–100) */
  skillMatch: number;
  /** Expérience normalisée (0–100), basée sur yearsOfExperience */
  experience: number;
  /** Note utilisateur (0–5) ou valeur par défaut */
  userRating: number;
  /** Nombre de projets complétés (normalisé 0–100) */
  completedProjects: number;
}
