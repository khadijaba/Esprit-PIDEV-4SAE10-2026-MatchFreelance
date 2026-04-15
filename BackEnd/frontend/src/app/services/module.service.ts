import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Module, ModuleRequest } from '../models/module.model';

@Injectable({ providedIn: 'root' })
export class ModuleService {
  private readonly api = '/api/modules';

  constructor(private http: HttpClient) {}

  getByFormation(formationId: number): Observable<Module[]> {
    return this.http.get<Module[]>(`${this.api}/formation/${formationId}`);
  }

  getById(id: number): Observable<Module> {
    return this.http.get<Module>(`${this.api}/${id}`);
  }

  create(module: ModuleRequest): Observable<Module> {
    return this.http.post<Module>(this.api, module);
  }

  update(id: number, module: ModuleRequest): Observable<Module> {
    return this.http.put<Module>(`${this.api}/${id}`, module);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
