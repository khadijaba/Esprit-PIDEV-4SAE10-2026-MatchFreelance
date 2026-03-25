export interface QuestionDto {
  id?: number;
  examenId?: number;
  ordre: number;
  enonce: string;
  optionA?: string;
  optionB?: string;
  optionC?: string;
  optionD?: string;
  bonneReponse: string;
}

export interface Examen {
  id: number;
  formationId: number;
  titre: string;
  description?: string | null;
  seuilReussi: number;
  questions: QuestionDto[];
}

export interface ExamenRequest {
  formationId: number;
  titre: string;
  description?: string;
  seuilReussi?: number;
  questions: QuestionDto[];
}

export type ResultatExamen = 'REUSSI' | 'ECHOUE';

export interface PassageExamen {
  id: number;
  freelancerId: number;
  examenId: number;
  examenTitre: string;
  score: number;
  resultat: ResultatExamen;
  datePassage: string;
  /** Certificat auto-généré lorsque resultat === 'REUSSI', renvoyé directement par l’API. */
  certificat?: Certificat;
}

export interface ReponseExamenRequest {
  freelancerId: number;
  reponses: string[];
}

export interface Certificat {
  id: number;
  numeroCertificat: string;
  passageExamenId: number;
  freelancerId: number;
  examenId: number;
  formationId?: number;
  examenTitre: string;
  score: number;
  /** Seuil de l'examen (aligné PDF / affichage web). */
  seuilReussi?: number;
  datePassage: string;
  dateDelivrance: string;
}

/** Réponse GET /api/certificats/verify/{numero} (scan QR). */
export interface CertificatVerifyResponse {
  valid: boolean;
  numeroCertificat?: string;
  message?: string;
  freelancerId?: number;
  examenId?: number;
  formationId?: number;
  examenTitre?: string;
  score?: number;
  dateDelivrance?: string;
  formationsRecommandees?: Array<{
    id?: number;
    titre?: string;
    typeFormation?: string;
    niveau?: string;
  }>;
}
