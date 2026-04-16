import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DecisionCopilotResponse, ProjectPhase } from '../models/project-phase.model';
import { PhaseDeliverable } from '../models/phase-deliverable.model';

export interface CreatePhaseBody {
  name: string;
  description?: string;
  phaseOrder: number;
  startDate?: string | null;
  dueDate?: string | null;
}

/** Longueurs alignées sur {@code ProjectPhase} (backend). */
const PHASE_NAME_MAX = 255;
const PHASE_DESC_MAX = 2000;

@Injectable({ providedIn: 'root' })
export class ProjectSupervisionService {
  private readonly base = '/api/projects';

  constructor(private http: HttpClient) {}

  listPhases(projectId: number): Observable<ProjectPhase[]> {
    return this.http.get<ProjectPhase[]>(`${this.base}/${projectId}/phases`);
  }

  createPhase(projectId: number, body: CreatePhaseBody): Observable<ProjectPhase> {
    const payload = this.normalizeCreatePhaseBody(body);
    return this.http.post<ProjectPhase>(`${this.base}/${projectId}/phases`, payload);
  }

  /**
   * Le microservice attend {@code LocalDateTime} sérialisé en ISO (ex. {@code 2026-04-16T00:00:00}).
   * Les seuls {@code YYYY-MM-DD} (inputs HTML ou plan IA) provoquent un 400 Jackson.
   */
  private normalizeCreatePhaseBody(body: CreatePhaseBody): Record<string, unknown> {
    const name = String(body.name ?? '')
      .trim()
      .slice(0, PHASE_NAME_MAX);
    let description = body.description;
    if (description != null && description.length > PHASE_DESC_MAX) {
      description = description.slice(0, PHASE_DESC_MAX);
    }
    return {
      name,
      description: description ?? undefined,
      phaseOrder: body.phaseOrder,
      startDate: this.toLocalDateTimeString(body.startDate, 'start'),
      dueDate: this.toLocalDateTimeString(body.dueDate, 'end'),
    };
  }

  private toLocalDateTimeString(
    value: string | null | undefined,
    kind: 'start' | 'end'
  ): string | null {
    if (value == null) return null;
    const s = String(value).trim();
    if (!s) return null;
    if (s.includes('T')) {
      return s.length >= 19 ? s.slice(0, 19) : s;
    }
    const day = s.slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) {
      return null;
    }
    return kind === 'end' ? `${day}T23:59:59` : `${day}T00:00:00`;
  }

  getDecisionCopilot(projectId: number, phaseId: number): Observable<DecisionCopilotResponse> {
    return this.http.get<DecisionCopilotResponse>(
      `${this.base}/${projectId}/phases/${phaseId}/decision-copilot`
    );
  }

  listDeliverables(projectId: number, phaseId: number): Observable<PhaseDeliverable[]> {
    return this.http.get<PhaseDeliverable[]>(
      `${this.base}/${projectId}/phases/${phaseId}/deliverables`
    );
  }

  createDeliverable(
    projectId: number,
    phaseId: number,
    body: { title: string; description?: string; type: string }
  ): Observable<PhaseDeliverable> {
    return this.http.post<PhaseDeliverable>(
      `${this.base}/${projectId}/phases/${phaseId}/deliverables`,
      body
    );
  }

  reviewDeliverable(
    projectId: number,
    phaseId: number,
    deliverableId: number,
    body: { reviewStatus: string; reviewComment?: string | null }
  ): Observable<PhaseDeliverable> {
    return this.http.put<PhaseDeliverable>(
      `${this.base}/${projectId}/phases/${phaseId}/deliverables/${deliverableId}/review`,
      body
    );
  }

  closePhase(projectId: number, phaseId: number): Observable<ProjectPhase> {
    return this.http.post<ProjectPhase>(`${this.base}/${projectId}/phases/${phaseId}/close`, {});
  }
}
