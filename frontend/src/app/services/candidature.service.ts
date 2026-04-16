import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CandidatureResponse } from '../models/candidature.model';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private readonly api = '/api/candidatures';

  constructor(private http: HttpClient) {}

  listByProject(projectId: number): Observable<CandidatureResponse[]> {
    return this.http.get<CandidatureResponse[]>(`${this.api}/project/${projectId}`);
  }
}
