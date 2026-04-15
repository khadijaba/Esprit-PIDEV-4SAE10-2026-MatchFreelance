export type StatutInscription = 'EN_ATTENTE' | 'VALIDEE' | 'REFUSEE' | 'ANNULEE';

export interface Inscription {
  id: number;
  freelancerId: number;
  formationId: number;
  formationTitre?: string;
  statut: StatutInscription;
  dateInscription: string;
}
