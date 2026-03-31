import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Formation, FormationRequest } from '../models/formation.model';

@Injectable({ providedIn: 'root' })
export class FormationService {
  private readonly api = '/api/formations';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Formation[]> {
    return this.http.get<Formation[] | Record<string, unknown>>(this.api).pipe(
      map((data) => {
        if (Array.isArray(data)) return data;
        const arr =
          (data as Record<string, unknown>)['content'] ??
          (data as Record<string, unknown>)['data'] ??
          (data as Record<string, unknown>)['formations'];
        return Array.isArray(arr) ? arr : [];
      }),
      catchError(() => of([]))
    );
  }

  getOuvertes(): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.api}/ouvertes`).pipe(
      catchError(() => this.getAll())
    );
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
