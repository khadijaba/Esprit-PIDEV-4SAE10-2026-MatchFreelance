import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Formation, FormationRequest } from '../models/formation.model';

@Injectable({ providedIn: 'root' })
export class FormationService {
  private readonly api = '/api/formations';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Formation[]> {
    return this.http.get<Formation[]>(this.api);
  }

  getOuvertes(): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.api}/ouvertes`);
  }

  getById(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.api}/${id}`);
  }

  create(formation: FormationRequest): Observable<Formation> {
    return this.http.post<Formation>(this.api, formation);
  }

  update(id: number, formation: FormationRequest): Observable<Formation> {
    return this.http.put<Formation>(`${this.api}/${id}`, formation);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
