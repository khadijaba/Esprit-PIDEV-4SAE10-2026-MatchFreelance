import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project, ProjectRequest, ProjectStatus } from '../models/project.model';
import { PageResponse } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  // Use relative URL so Angular dev server proxy (proxy.conf.json) forwards to backend on 8082
  private readonly api = '/api/projects';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Project[]> {
    return this.http.get<Project[]>(this.api);
  }

  getPage(params: {
    q?: string;
    status?: ProjectStatus;
    clientId?: number;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<PageResponse<Project>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v === undefined || v === null || v === '') return;
      httpParams = httpParams.set(k, String(v));
    });
    return this.http.get<PageResponse<Project>>(`${this.api}/page`, { params: httpParams });
  }

  getById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.api}/${id}`);
  }

  getByStatus(status: ProjectStatus): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.api}/status/${status}`);
  }

  search(title: string): Observable<Project[]> {
    const params = new HttpParams().set('title', title);
    return this.http.get<Project[]>(`${this.api}/search`, { params });
  }

  getByClientId(clientId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.api}/client/${clientId}`);
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
}

