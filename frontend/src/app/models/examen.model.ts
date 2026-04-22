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
  seuilReussi?: number;
  datePassage: string;
  dateDelivrance: string;
}

export interface CertificatVerifyResponse {
  valid: boolean;
  numeroCertificat?: string;
  examenTitre?: string;
  score?: number;
  freelancerId?: number;
  dateDelivrance?: string;
  formationsRecommandees?: Array<{
    id?: number;
    formationId?: number;
    titre?: string;
    typeFormation?: string;
    niveau?: string;
    raison?: string;
  }>;
  certificat?: Certificat;
  message?: string;
}

export interface FreelancerRanking {
  freelancerId: number;
  averageScore: number;
  examsTaken: number;
  certificatesCount: number;
  lastExamDate?: string | null;
  rank?: number;
  globalScore?: number;
  successRate?: number;
  certificationsCount?: number;
  attemptsCount?: number;
  lastAttemptAt?: string | null;
}

export interface ProjectMatchItem {
  projectId?: number;
  id?: number;
  title?: string | null;
  titre?: string | null;
  budget?: number | null;
  matchScore?: number;
  matchedSkills?: string[];
  scoreAlignementSkills?: number;
  statut?: string | null;
  raison?: string | null;
}

export interface FreelancerProjectMatching {
  freelancerId: number;
  generatedAt?: string;
  projects: ProjectMatchItem[];
}

export interface SuccessPrediction {
  examenId: number;
  freelancerId: number;
  probabiliteReussite?: number;
  scoreMetierRealiste?: number;
  recommendation?: string;
  recommandation?: string;
  explicationNaturelle?: string;
}

export interface RemediationStep {
  sequence: number;
  moduleTitre: string;
  objectif?: string;
  dureeEstimeeMinutes?: number;
}

export interface RemediationPlan {
  freelancerId: number;
  examenId: number;
  resume?: string;
  etapes: RemediationStep[];
}
