import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateDeliverableRequest,
  CreatePhaseMeetingRequest,
  CreatePhaseRequest,
  DecisionCopilotResponse,
  PhaseDeliverable,
  PhaseMeeting,
  ProjectPhase,
  ReviewDeliverableRequest,
} from '../models/project-supervision.model';

@Injectable({ providedIn: 'root' })
export class ProjectSupervisionService {
  private readonly api = '/api/projects';

  constructor(private http: HttpClient) {}

  listPhases(projectId: number): Observable<ProjectPhase[]> {
    return this.http.get<ProjectPhase[]>(`${this.api}/${projectId}/phases`);
  }

  createPhase(projectId: number, body: CreatePhaseRequest): Observable<ProjectPhase> {
    return this.http.post<ProjectPhase>(`${this.api}/${projectId}/phases`, body);
  }

  startPhase(projectId: number, phaseId: number): Observable<ProjectPhase> {
    return this.http.put<ProjectPhase>(`${this.api}/${projectId}/phases/${phaseId}/start`, {});
  }

  closePhase(projectId: number, phaseId: number): Observable<ProjectPhase> {
    return this.http.post<ProjectPhase>(`${this.api}/${projectId}/phases/${phaseId}/close`, {});
  }

  deletePhase(projectId: number, phaseId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${projectId}/phases/${phaseId}`);
  }

  listDeliverables(projectId: number, phaseId: number): Observable<PhaseDeliverable[]> {
    return this.http.get<PhaseDeliverable[]>(`${this.api}/${projectId}/phases/${phaseId}/deliverables`);
  }

  createDeliverable(projectId: number, phaseId: number, body: CreateDeliverableRequest): Observable<PhaseDeliverable> {
    return this.http.post<PhaseDeliverable>(`${this.api}/${projectId}/phases/${phaseId}/deliverables`, body);
  }

  reviewDeliverable(
    projectId: number,
    phaseId: number,
    deliverableId: number,
    body: ReviewDeliverableRequest
  ): Observable<PhaseDeliverable> {
    return this.http.put<PhaseDeliverable>(
      `${this.api}/${projectId}/phases/${phaseId}/deliverables/${deliverableId}/review`,
      body
    );
  }

  listMeetings(projectId: number, phaseId: number): Observable<PhaseMeeting[]> {
    return this.http.get<PhaseMeeting[]>(`${this.api}/${projectId}/phases/${phaseId}/meetings`);
  }

  createMeeting(projectId: number, phaseId: number, body: CreatePhaseMeetingRequest): Observable<PhaseMeeting> {
    return this.http.post<PhaseMeeting>(`${this.api}/${projectId}/phases/${phaseId}/meetings`, body);
  }

  getDecisionCopilot(projectId: number, phaseId: number): Observable<DecisionCopilotResponse> {
    return this.http.get<DecisionCopilotResponse>(`${this.api}/${projectId}/phases/${phaseId}/decision-copilot`);
  }
}
