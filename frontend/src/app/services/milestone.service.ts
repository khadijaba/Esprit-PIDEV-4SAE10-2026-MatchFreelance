import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Milestone, CreateMilestoneRequest, MilestoneStatus } from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class MilestoneService {
  private readonly api = '/api/milestones';

  constructor(private http: HttpClient) {}

  getByWorkspace(workspaceId: number): Observable<Milestone[]> {
    return this.http.get<Milestone[]>(`${this.api}/workspace/${workspaceId}`);
  }

  getById(id: number): Observable<Milestone> {
    return this.http.get<Milestone>(`${this.api}/${id}`);
  }

  getByAssignee(assigneeId: number): Observable<Milestone[]> {
    return this.http.get<Milestone[]>(`${this.api}/assignee/${assigneeId}`);
  }

  getByWorkspaceAndStatus(workspaceId: number, status: MilestoneStatus): Observable<Milestone[]> {
    return this.http.get<Milestone[]>(`${this.api}/workspace/${workspaceId}/status/${status}`);
  }

  getOverdue(): Observable<Milestone[]> {
    return this.http.get<Milestone[]>(`${this.api}/overdue`);
  }

  create(req: CreateMilestoneRequest): Observable<Milestone> {
    return this.http.post<Milestone>(this.api, req);
  }

  update(id: number, req: CreateMilestoneRequest): Observable<Milestone> {
    return this.http.put<Milestone>(`${this.api}/${id}`, req);
  }

  updateProgress(id: number, progress: number): Observable<Milestone> {
    return this.http.patch<Milestone>(`${this.api}/${id}/progress`, { progress });
  }

  complete(id: number): Observable<Milestone> {
    return this.http.patch<Milestone>(`${this.api}/${id}/complete`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
