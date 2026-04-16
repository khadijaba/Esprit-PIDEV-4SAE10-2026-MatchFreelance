import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FreelancerFitBatchDto } from '../models/freelancer-fit.model';
import { ProjectEffortEstimate } from '../models/project-effort.model';
import { ProjectMlRisk } from '../models/project-ml-risk.model';
import { ProjectInsights } from '../models/project-insights.model';
import { Project, ProjectRequest, ProjectStatus } from '../models/project.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  // Use relative URL so Angular dev server proxy (proxy.conf.json) forwards to backend on 8082
  private readonly api = '/api/projects';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Project[]> {
    return this.http.get<Project[]>(this.api);
  }

  getById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.api}/${id}`);
  }

  getByStatus(status: ProjectStatus): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.api}/status/${status}`);
  }

  getByOwnerId(projectOwnerId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.api}/owner/${projectOwnerId}`);
  }

  /** Alias rétrocompatible pour anciens composants. */
  getByOwner(projectOwnerId: number): Observable<Project[]> {
    return this.getByOwnerId(projectOwnerId);
  }

  getByRequiredSkill(skill: string): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.api}/skill/${skill}`);
  }

  search(title: string): Observable<Project[]> {
    const params = new HttpParams().set('title', title);
    return this.http.get<Project[]>(`${this.api}/search`, { params });
  }

  create(project: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.api, project);
  }

  update(id: number, project: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.api}/${id}`, project);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  /** Estimation durée + score réussite contextualisé (microservice Project + Candidatures). */
  getFreelancerFit(projectId: number, freelancerIds: number[]): Observable<FreelancerFitBatchDto> {
    const ids = [...new Set(freelancerIds.map(Number))].filter((n) => Number.isFinite(n)).join(',');
    const params = new HttpParams().set('freelancerIds', ids);
    return this.http.get<FreelancerFitBatchDto>(`${this.api}/${projectId}/freelancer-fit`, { params });
  }

  /** Note client 1–5 après mission (enrichit le score). */
  submitFreelancerRating(projectId: number, freelancerId: number, rating: number): Observable<unknown> {
    return this.http.post(`${this.api}/${projectId}/freelancer-ratings`, { freelancerId, rating });
  }

  /** Score de risque (RandomForest ONNX ou repli heuristique). */
  getMlRisk(projectId: number): Observable<ProjectMlRisk> {
    return this.http.get<ProjectMlRisk>(`${this.api}/${projectId}/risk-ml`);
  }

  /** Estimation charge jours-homme (heuristique). */
  getEffortEstimate(projectId: number): Observable<ProjectEffortEstimate> {
    return this.http.get<ProjectEffortEstimate>(`${this.api}/${projectId}/effort-estimate`);
  }

  /** Indicateurs qualité d’annonce, attractivité, candidatures (agrégation Candidature via le MS Project). */
  getInsights(projectId: number): Observable<ProjectInsights> {
    return this.http.get<ProjectInsights>(`${this.api}/${projectId}/insights`);
  }
}

