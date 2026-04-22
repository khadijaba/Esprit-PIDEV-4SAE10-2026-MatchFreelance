/**
 * Résultat du Talent Matching Engine pour un freelancer.
 * Score = (Compétences × 0,7) + (Expérience × 0,3).
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
  /** Note utilisateur (0–5), 0 = non disponible — affichage optionnel */
  userRating: number;
  /** Nombre de projets complétés — affichage optionnel, 0 = non disponible */
  completedProjects: number;
}
