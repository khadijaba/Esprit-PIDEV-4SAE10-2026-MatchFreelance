import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AnalyzeProjectRequest,
  AnalyzeProjectResponse,
  BuildTeamRequest,
  BuildTeamResponse,
} from '../models/team-ai.model';

/**
 * Service pour le moteur Team AI (Python) : analyse de projet (NLP/LLM) et construction d'équipe.
 * Proxy : /api/ai -> http://localhost:5000 (voir proxy.conf.json).
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
}
