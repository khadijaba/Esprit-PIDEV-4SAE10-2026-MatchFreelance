import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AnalyzeProjectRequest,
  AnalyzeProjectResponse,
  BuildTeamRequest,
  BuildTeamResponse,
  DescriptionCoachRequest,
  DescriptionCoachResponse,
  PlanningAssistantAdjustRequest,
  PlanningAssistantInitialRequest,
  PlanningAssistantResponse,
  ScheduleOverrunRequest,
  ScheduleOverrunResponse,
} from '../models/team-ai.model';

/**
 * Service pour le moteur Team AI (Python) : analyse de projet (NLP/LLM) et construction d'équipe.
 * Proxy dev : /api -> gateway8086 ; la gateway envoie /api/ai/** vers Team AI (voir proxy.conf.js).
 */
@Injectable({ providedIn: 'root' })
export class TeamAiService {
  private readonly base = '/api/ai';

  constructor(private http: HttpClient) {}

  /**
   * Analyse la description du projet : rôles, compétences, budget, durée, complexité.
   */
  analyzeProject(body: AnalyzeProjectRequest): Observable<AnalyzeProjectResponse> {
    return this.http.post<AnalyzeProjectResponse>(`${this.base}/analyze-project`, body);
  }

  /**
   * Construit une équipe optimale et retourne les notifications à envoyer à chaque membre.
   */
  buildTeam(body: BuildTeamRequest): Observable<BuildTeamResponse> {
    return this.http.post<BuildTeamResponse>(`${this.base}/build-team`, body);
  }

  /**
   * Prévision de dépassement : temps écoulé vs avancement (phases / livrables).
   * Envoie le JWT (intercepteur) pour que le service Python appelle la gateway avec les mêmes droits.
   */
  scheduleOverrunAssessment(body: ScheduleOverrunRequest): Observable<ScheduleOverrunResponse> {
    // Chemin court /api/ai/sor → gateway → /api/sor (évite certains 404 sur le nom long côté proxy).
    return this.http.post<ScheduleOverrunResponse>(`${this.base}/sor`, body);
  }

  /** Suggestions de reformulation (périmètre, livrables, critères d'acceptation). */
  descriptionCoach(body: DescriptionCoachRequest): Observable<DescriptionCoachResponse> {
    // Même schéma d’URL que analyze-project (évite certains 404 gateway/proxy sur « description-coach »).
    return this.http.post<DescriptionCoachResponse>(`${this.base}/analyze-description`, body);
  }

  /** Planning assistant: génère les phases initiales (LLM ou heuristique fallback). */
  planningInitialPlan(body: PlanningAssistantInitialRequest): Observable<PlanningAssistantResponse> {
    return this.http.post<PlanningAssistantResponse>(`${this.base}/planning-assistant/initial-plan`, body);
  }

  /** Planning assistant: propose un réajustement si retard/risque détecté. */
  planningAdjustPlan(body: PlanningAssistantAdjustRequest): Observable<PlanningAssistantResponse> {
    return this.http.post<PlanningAssistantResponse>(`${this.base}/planning-assistant/adjust-plan`, body);
  }
}
