export type StatutFormation = 'OUVERTE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE';

export type NiveauFormation = 'DEBUTANT' | 'INTERMEDIAIRE' | 'AVANCE';

export type TypeFormation =
  | 'WEB_DEVELOPMENT'
  | 'DEVOPS'
  | 'CYBERSECURITY'
  | 'DESIGN'
  | 'MOBILE_DEVELOPMENT'
  | 'AI'
  | 'DATA_SCIENCE';

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
  niveau?: NiveauFormation | null;
  examenRequisId?: number | null;
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
  niveau?: NiveauFormation | null;
  examenRequisId?: number | null;
}

export const NIVEAU_FORMATION_LABELS: Record<NiveauFormation, string> = {
  DEBUTANT: 'Débutant',
  INTERMEDIAIRE: 'Intermédiaire',
  AVANCE: 'Avancé',
};

export const TYPE_FORMATION_LABELS: Record<TypeFormation, string> = {
  WEB_DEVELOPMENT: 'Développement Web',
  DEVOPS: 'DevOps',
  CYBERSECURITY: 'Cybersécurité',
  DESIGN: 'Design',
  MOBILE_DEVELOPMENT: 'Développement Mobile',
  AI: 'Intelligence Artificielle',
  DATA_SCIENCE: 'Data Science',
};
