export type StatutFormation = 'OUVERTE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE';

/** Aligné avec les catégories skills : Développement Web, Mobile, IA, Data Science, DevOps, Cybersécurité, Design */
export type TypeFormation =
  | 'WEB_DEVELOPMENT'
  | 'MOBILE_DEVELOPMENT'
  | 'AI'
  | 'DATA_SCIENCE'
  | 'DEVOPS'
  | 'CYBERSECURITY'
  | 'DESIGN';

export interface Formation {
  id: number;
  titre: string;
  typeFormation?: TypeFormation;
  description: string | null;
  dureeHeures: number;
  dateDebut: string;
  dateFin: string;
  capaciteMax: number | null;
  statut: StatutFormation;
  /** Compatibilité Career Path (backend peut renvoyer title/skills) */
  title?: string;
  skills?: string[];
  skillNames?: string[];
  tags?: string[];
}

export interface FormationRequest {
  titre: string;
  typeFormation?: TypeFormation;
  description: string;
  dureeHeures: number;
  dateDebut: string;
  dateFin: string;
  capaciteMax: number;
  statut?: StatutFormation;
}

export const TYPE_FORMATION_LABELS: Record<TypeFormation, string> = {
  WEB_DEVELOPMENT: 'Développement Web',
  DEVOPS: 'DevOps',
  CYBERSECURITY: 'Cybersécurité',
  DESIGN: 'Design',
  MOBILE_DEVELOPMENT: 'Développement Mobile',
  AI: 'Intelligence Artificielle',
  DATA_SCIENCE: 'Data Science',
};
