export type ParcoursInclusion = 'COMMUN' | 'STANDARD' | 'RENFORCEMENT';
export type TypeParcours = 'STANDARD' | 'RENFORCEMENT';

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
  /** Lot pour parcours différenciés (admin). */
  parcoursInclusion?: ParcoursInclusion | string;
  /** Thème pour relier les erreurs aux modules Formation. */
  theme?: string | null;
  skill?: string | null;
  /** FACILE | MOYEN | DIFFICILE — examen adaptatif. */
  niveauDifficulte?: string | null;
  /** Explication pédagogique (correction détaillée). */
  explication?: string | null;
}

export interface Examen {
  id: number;
  formationId: number;
  titre: string;
  description?: string | null;
  seuilReussi: number;
  questions: QuestionDto[];
}

/** Brouillon renvoyé par la génération avec {@code preview: true} (pas encore en base). */
export type ExamenDraft = Omit<Examen, 'id'> & { id?: number };

export interface ExamenRequest {
  formationId: number;
  titre: string;
  description?: string;
  seuilReussi?: number;
  /** Questions : inclure parcoursInclusion / theme pour parcours différenciés. */
  questions: QuestionDto[];
}

/** Requête vers POST /api/examens/validate-question-ai */
export interface QuestionValidationRequestPayload {
  enonce?: string;
  optionA?: string;
  optionB?: string;
  optionC?: string;
  optionD?: string;
  bonneReponse?: string;
  theme?: string;
  skill?: string;
  contexteFormation?: string;
}

export interface QuestionValidationResult {
  llmConfigured: boolean;
  parseOk: boolean;
  clairScore?: number | null;
  correctTechniqueScore?: number | null;
  ambigu?: boolean | null;
  ambiguDetails?: string | null;
  publishable?: boolean | null;
  summary?: string | null;
  suggestions?: string[];
  errorMessage?: string | null;
}

export interface FreelancerRanking {
  freelancerId: number;
  rank: number;
  globalScore: number;
  averageScore: number;
  successRate: number;
  certificationsCount: number;
  attemptsCount: number;
  lastAttemptAt?: string | null;
}

export interface FreelancerProjectMatching {
  freelancerId: number;
  profileSkillTokens?: number;
  projects: ProjetMarche[];
}

export type ResultatExamen = 'REUSSI' | 'ECHOUE';

/** Une ligne de correction (mode entraînement). */
export interface CorrectionItem {
  questionId?: number;
  ordre?: number;
  theme?: string | null;
  /** Compétence agrégée (skill ou repli thème). */
  skill?: string | null;
  enonce?: string;
  reponseChoisie?: string;
  bonneReponse?: string;
  correct: boolean;
  niveauDifficulte?: string;
  poids?: number;
  pointsSurQuestion?: number;
  explication?: string | null;
}

export interface ModuleRevision {
  id?: number;
  titre?: string;
  description?: string;
  ordre?: number;
  dureeMinutes?: number;
  raison?: string;
}

export interface SuccessPrediction {
  probabiliteReussite: number;
  niveauConfiance?: string;
  scoreMoyenHistorique?: number;
  tauxReussiteHistorique?: number;
  tempsMoyenPreparationJours?: number;
  recommandation?: string;
  modulesAvantCertifiant?: ModuleRevision[];
}

export interface RemediationStep {
  sequence?: number;
  moduleId?: number;
  moduleTitre?: string;
  dureeEstimeeMinutes?: number;
  dateCible?: string;
  objectifScoreTheme?: number;
  raison?: string;
}

export interface RemediationPlan {
  freelancerId: number;
  examenId: number;
  generatedAt?: string;
  parcoursSuggere?: TypeParcours;
  objectifScoreCible?: number;
  estimationTotaleMinutes?: number;
  resume?: string;
  etapes?: RemediationStep[];
}

/** Score pondéré par compétence après un passage. */
export interface SkillScore {
  skill: string;
  pointsObtenus: number;
  pointsMax: number;
  pourcentage: number;
  bonnesReponses: number;
  totalQuestions: number;
}

export interface EvaluationRisque {
  scoreRisque: number;
  niveau: string;
  messageApprenant?: string;
  alerteFormateur?: boolean;
  messageFormateur?: string | null;
  facteurs?: string[];
  parcoursRecommande?: TypeParcours;
}

/** Compétence ajoutée sur le profil après certificat (microservice Skill). */
export interface CompetenceAttribuee {
  skillId?: number | null;
  nom?: string;
  categorie?: string;
  niveau?: string;
  statut?: string;
}

/** Projet marché suggéré après certificat (microservice Project). */
export interface ProjetMarche {
  id?: number;
  titre?: string;
  budget?: number;
  dureeJours?: number;
  statut?: string;
  raison?: string;
  /** 0–100 : adéquation compétences requises / profil Skill (liste triée : meilleur score en premier). */
  scoreAlignementSkills?: number;
}

export interface PassageExamen {
  id?: number | null;
  freelancerId: number;
  examenId: number;
  examenTitre: string;
  score: number;
  /** Pourcentage sans pondération par difficulté (si renvoyé par l’API). */
  scoreSansPonderation?: number | null;
  resultat: ResultatExamen;
  datePassage: string;
  typeParcours?: TypeParcours;
  mode?: 'ENTRAINEMENT' | 'CERTIFIANT';
  totalQuestions?: number;
  bonnesReponses?: number;
  /** Somme des poids (max points possibles sur l’examen passé). */
  pointsMax?: number | null;
  /** Points obtenus (somme des poids des bonnes réponses). */
  pointsObtenus?: number | null;
  analyseErreurs?: string[];
  messageFeedback?: string;
  formationsRecommandees?: Array<Record<string, unknown>>;
  modulesRevisionCibles?: ModuleRevision[];
  evaluationRisque?: EvaluationRisque;
  correction?: CorrectionItem[];
  /** Répartition du score par compétence (skill), même pondération que le score global. */
  scoreParSkill?: SkillScore[];
  /** Certificat auto-généré lorsque resultat === 'REUSSI', renvoyé directement par l’API. */
  certificat?: Certificat;
  /** Niveau métier déduit du score (réussite certifiante). */
  niveauCalcule?: string;
  competencesAttribuees?: CompetenceAttribuee[];
  projetsMarcheRecommandes?: ProjetMarche[];
  messageCarriere?: string;
}

export interface ReponseExamenRequest {
  freelancerId: number;
  reponses: string[];
  mode?: 'ENTRAINEMENT' | 'CERTIFIANT';
  typeParcours?: TypeParcours;
  /** Sous-ensemble « questions déjà ratées » — uniquement avec mode ENTRAINEMENT. */
  revisionCiblee?: boolean;
}

export interface AmenagementTemps {
  secondesParQuestionBase: number;
  multiplicateurChrono: number;
  secondesEffectivesParQuestion: number;
}

export interface ObjectifTheme {
  id: number;
  examenId: number;
  theme: string;
  objectifScore: number;
  actif: boolean;
  dernierScoreTheme: number | null;
  objectifAtteint: boolean;
}

export interface DemarrerAdaptatifRequest {
  freelancerId: number;
  typeParcours?: TypeParcours;
  mode?: 'ENTRAINEMENT' | 'CERTIFIANT';
}

export interface AdaptatifDemarrageDto {
  token: string;
  question: QuestionDto;
  numeroQuestion: number;
  questionsTotal: number;
  difficulteCible?: string;
}

export interface AdaptatifRepondreRequest {
  questionId: number;
  reponse: string;
}

export interface AdaptatifEtapeReponseDto {
  reponseCorrecte: boolean;
  termine: boolean;
  difficulteApresAjustement?: string;
  numeroQuestion?: number;
  questionsTotal?: number;
  prochaineQuestion?: QuestionDto;
  resultat?: PassageExamen;
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
