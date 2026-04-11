import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Workspace, CreateWorkspaceRequest, WorkspaceStatus } from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly api = '/api/workspaces';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Workspace[]> {
    return this.http.get<Workspace[]>(this.api);
  }

  getById(id: number): Observable<Workspace> {
    return this.http.get<Workspace>(`${this.api}/${id}`);
  }

  getByContractId(contractId: number): Observable<Workspace> {
    return this.http.get<Workspace>(`${this.api}/contract/${contractId}`);
  }

  getByOwner(ownerId: number): Observable<Workspace[]> {
    return this.http.get<Workspace[]>(`${this.api}/owner/${ownerId}`);
  }

  getByStatus(status: WorkspaceStatus): Observable<Workspace[]> {
    return this.http.get<Workspace[]>(`${this.api}/status/${status}`);
  }

  create(req: CreateWorkspaceRequest): Observable<Workspace> {
    return this.http.post<Workspace>(this.api, req);
  }

  update(id: number, req: CreateWorkspaceRequest): Observable<Workspace> {
    return this.http.put<Workspace>(`${this.api}/${id}`, req);
  }

  updateStatus(id: number, status: WorkspaceStatus): Observable<Workspace> {
    return this.http.patch<Workspace>(`${this.api}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
