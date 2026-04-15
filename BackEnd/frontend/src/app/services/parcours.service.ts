import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ParcoursIntelligentResponse } from '../models/parcours.model';

@Injectable({ providedIn: 'root' })
export class ParcoursService {
  private readonly api = '/api/skills';

  constructor(private http: HttpClient) {}

  getParcoursIntelligent(freelancerId: number): Observable<ParcoursIntelligentResponse> {
    return this.http.get<ParcoursIntelligentResponse>(
      `${this.api}/parcours/intelligent`,
      { params: { freelancerId: String(freelancerId) } }
    );
  }
}
