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
  examenTitre: string;
  score: number;
  datePassage: string;
  dateDelivrance: string;
}
