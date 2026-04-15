import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdaptatifDemarrageDto,
  AdaptatifEtapeReponseDto,
  AdaptatifRepondreRequest,
  AmenagementTemps,
  Certificat,
  DemarrerAdaptatifRequest,
  EvaluationRisque,
  Examen,
  ExamenDraft,
  ExamenRequest,
  FreelancerProjectMatching,
  FreelancerRanking,
  ObjectifTheme,
  PassageExamen,
  QuestionValidationRequestPayload,
  QuestionValidationResult,
  RemediationPlan,
  ReponseExamenRequest,
  SuccessPrediction,
  TypeParcours,
} from '../models/examen.model';

/**
 * URL relative : le proxy (ng serve) envoie /api/* vers http://localhost:8050.
 * Ainsi pas de CORS (même origine localhost:4200).
 */
@Injectable({ providedIn: 'root' })
export class ExamenService {
  private readonly api = '/api/examens';
  private readonly apiCertificats = '/api/certificats';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Examen[]> {
    return this.http.get<Examen[]>(this.api);
  }

  getByFormation(formationId: number): Observable<Examen[]> {
    return this.http.get<Examen[]>(`${this.api}/formation/${formationId}`);
  }

  getById(id: number): Observable<Examen> {
    return this.http.get<Examen>(`${this.api}/${id}`);
  }

  getPourPassage(id: number, parcours: TypeParcours = 'STANDARD'): Observable<Examen> {
    return this.http.get<Examen>(`${this.api}/${id}/passage`, { params: { parcours } });
  }

  getPourRevision(examenId: number, freelancerId: number, parcours: TypeParcours = 'STANDARD'): Observable<Examen> {
    return this.http.get<Examen>(`${this.api}/examen/${examenId}/revision`, {
      params: { freelancerId: String(freelancerId), parcours },
    });
  }

  getAmenagementTemps(freelancerId: number): Observable<AmenagementTemps> {
    return this.http.get<AmenagementTemps>(`${this.api}/freelancer/${freelancerId}/amenagement-temps`);
  }

  setAmenagementTemps(
    freelancerId: number,
    body: { multiplicateurChrono?: number; motif?: string; actif?: boolean }
  ): Observable<AmenagementTemps> {
    return this.http.post<AmenagementTemps>(`${this.api}/freelancer/${freelancerId}/amenagement-temps`, body);
  }

  listObjectifsTheme(freelancerId: number): Observable<ObjectifTheme[]> {
    return this.http.get<ObjectifTheme[]>(`${this.api}/freelancer/${freelancerId}/objectifs-theme`);
  }

  creerObjectifTheme(
    freelancerId: number,
    body: { examenId: number; theme: string; objectifScore?: number }
  ): Observable<ObjectifTheme> {
    return this.http.post<ObjectifTheme>(`${this.api}/freelancer/${freelancerId}/objectifs-theme`, body);
  }

  supprimerObjectifTheme(freelancerId: number, objectifId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/freelancer/${freelancerId}/objectifs-theme/${objectifId}`);
  }

  demarrerAdaptatif(examenId: number, body: DemarrerAdaptatifRequest): Observable<AdaptatifDemarrageDto> {
    return this.http.post<AdaptatifDemarrageDto>(`${this.api}/${examenId}/adaptatif/demarrer`, body);
  }

  repondreAdaptatif(
    examenId: number,
    token: string,
    body: AdaptatifRepondreRequest
  ): Observable<AdaptatifEtapeReponseDto> {
    return this.http.post<AdaptatifEtapeReponseDto>(
      `${this.api}/${examenId}/adaptatif/session/${encodeURIComponent(token)}/repondre`,
      body
    );
  }

  getEvaluationRisque(examenId: number, freelancerId: number): Observable<EvaluationRisque> {
    return this.http.get<EvaluationRisque>(`${this.api}/${examenId}/parcours/risque/freelancer/${freelancerId}`);
  }

  getSimulationReussite(examenId: number, freelancerId: number): Observable<SuccessPrediction> {
    return this.http.get<SuccessPrediction>(
      `${this.api}/${examenId}/freelancer/${freelancerId}/simulateur-reussite`
    );
  }

  getPlanRemediation(examenId: number, freelancerId: number): Observable<RemediationPlan> {
    return this.http.get<RemediationPlan>(
      `${this.api}/${examenId}/freelancer/${freelancerId}/plan-remediation`
    );
  }

  create(dto: ExamenRequest): Observable<Examen> {
    return this.http.post<Examen>(this.api, dto);
  }

  /** Génère un examen à partir des modules de la formation (backend + Formation MS). formationId est envoyé dans le corps. */
  generateFromFormation(
    formationId: number,
    body?: { seuilReussi?: number; suffixeTitre?: string; useLlm?: boolean }
  ): Observable<Examen> {
    return this.http.post<Examen>(`${this.api}/generation/auto-generate`, {
      formationId,
      seuilReussi: body?.seuilReussi ?? 60,
      suffixeTitre: body?.suffixeTitre,
      preview: false,
      useLlm: body?.useLlm ?? true,
    });
  }

  /**
   * Même génération que {@link generateFromFormation} mais sans enregistrement en base (réponse 200, pas d’id).
   */
  previewGenerateFromFormation(
    formationId: number,
    body?: { seuilReussi?: number; suffixeTitre?: string; useLlm?: boolean }
  ): Observable<ExamenDraft> {
    return this.http.post<ExamenDraft>(`${this.api}/generation/auto-generate`, {
      formationId,
      seuilReussi: body?.seuilReussi ?? 60,
      suffixeTitre: body?.suffixeTitre,
      preview: true,
      useLlm: body?.useLlm ?? true,
    });
  }

  /** Enregistre en base un examen issu d’une prévisualisation (sans ids). */
  confirmAutoGenerated(examen: ExamenDraft): Observable<Examen> {
    const payload: ExamenRequest = {
      formationId: examen.formationId,
      titre: examen.titre,
      description: examen.description ?? undefined,
      seuilReussi: examen.seuilReussi,
      questions: (examen.questions ?? []).map((q) => ({
        ordre: q.ordre,
        enonce: q.enonce,
        optionA: q.optionA,
        optionB: q.optionB,
        optionC: q.optionC,
        optionD: q.optionD,
        bonneReponse: q.bonneReponse,
        parcoursInclusion: q.parcoursInclusion,
        theme: q.theme ?? undefined,
        skill: q.skill ?? undefined,
        niveauDifficulte: q.niveauDifficulte ?? undefined,
        explication: q.explication ?? undefined,
      })),
    };
    return this.create(payload);
  }

  update(id: number, dto: Partial<ExamenRequest>): Observable<Examen> {
    return this.http.put<Examen>(`${this.api}/${id}`, dto);
  }

  /** Ajoute des questions « squelette » (ex. lot Commun) à un examen déjà créé. */
  appendQuestionsModele(
    examenId: number,
    body: { nombre: number; parcoursInclusion?: string; niveauDifficulte?: string }
  ): Observable<Examen> {
    return this.http.post<Examen>(`${this.api}/examen/${examenId}/questions-modele`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  passerExamen(examenId: number, request: ReponseExamenRequest): Observable<PassageExamen> {
    return this.http.post<PassageExamen>(`${this.api}/${examenId}/passer`, request);
  }

  getResultatsByFreelancer(freelancerId: number): Observable<PassageExamen[]> {
    return this.http.get<PassageExamen[]>(`${this.api}/resultats/freelancer/${freelancerId}`);
  }

  getResultatsByExamen(examenId: number): Observable<PassageExamen[]> {
    return this.http.get<PassageExamen[]>(`${this.api}/${examenId}/resultats`);
  }

  getPassage(examenId: number, freelancerId: number): Observable<PassageExamen> {
    return this.http.get<PassageExamen>(`${this.api}/${examenId}/freelancer/${freelancerId}`);
  }

  getCertificatByPassage(passageExamenId: number): Observable<Certificat> {
    return this.http.get<Certificat>(`${this.apiCertificats}/passage/${passageExamenId}`);
  }

  getCertificatById(id: number): Observable<Certificat> {
    return this.http.get<Certificat>(`${this.apiCertificats}/${id}`);
  }

  getCertificatsByFreelancer(freelancerId: number): Observable<Certificat[]> {
    return this.http.get<Certificat[]>(`${this.apiCertificats}/freelancer/${freelancerId}`);
  }

  /** Télécharge le PDF du certificat via le proxy puis ouvre-le dans un nouvel onglet (évite 404 en navigation directe). */
  getCertificatPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiCertificats}/${id}/pdf`, { responseType: 'blob' });
  }

  /** Validation IA (Ollama / LLM) avant publication d’une question. */
  validateQuestionAi(body: QuestionValidationRequestPayload): Observable<QuestionValidationResult> {
    return this.http.post<QuestionValidationResult>(`${this.api}/validate-question-ai`, body);
  }

  getGlobalRanking(): Observable<FreelancerRanking[]> {
    return this.http.get<FreelancerRanking[]>(`${this.api}/ranking/global`);
  }

  getTopPerformers(limit = 10): Observable<FreelancerRanking[]> {
    return this.http.get<FreelancerRanking[]>(`${this.api}/ranking/top-performers`, {
      params: { limit: String(limit) },
    });
  }

  getProjectMatching(freelancerId: number, limit = 5): Observable<FreelancerProjectMatching> {
    return this.http.get<FreelancerProjectMatching>(
      `${this.api}/ranking/freelancer/${freelancerId}/project-matching`,
      { params: { limit: String(limit) } }
    );
  }
}
