import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AnalyzeProjectRequest {
  title: string;
  description: string;
  /** Passe à true quand l’utilisateur coche « brouillon LLM » — le backend tente OpenAI/Ollama si configuré. */
  preferLlm?: boolean;
}

export interface BudgetEstimateDto {
  minAmount: number;
  maxAmount: number;
  currency?: string;
}

export interface AnalyzeProjectResponse {
  complexity: string;
  roles: string[];
  requiredSkills: string[];
  budgetEstimate?: BudgetEstimateDto | null;
  durationEstimateDays?: number | null;
  technicalLeaderRole?: string | null;
  summary?: string | null;
  /** rules | llm — renvoyé par Team AI pour l’UI (bandeau brouillon). */
  analysisSource?: string;
}

@Injectable({ providedIn: 'root' })
export class TeamAiService {
  private readonly http = inject(HttpClient);
  private readonly analyzeUrl = '/api/team-ai/analyze-project';

  analyzeProject(body: AnalyzeProjectRequest): Observable<AnalyzeProjectResponse> {
    return this.http.post<AnalyzeProjectResponse>(this.analyzeUrl, body);
  }
}
