/**
 * Résultat du Skill Gap Analyzer pour un freelancer qui consulte un projet.
 */
export interface SkillGapResult {
  /** Compatibilité en % (0–100) */
  compatibility: number;
  /** Compétences requises par le projet que le freelancer possède */
  matchedSkills: string[];
  /** Compétences requises par le projet que le freelancer n'a pas */
  missingSkills: string[];
}
