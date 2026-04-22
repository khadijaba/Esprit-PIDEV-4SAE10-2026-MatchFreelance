import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Contract } from '../models/contract.model';

@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly api = '/api/contracts';

  constructor(private http: HttpClient) {}

  getByProjectId(projectId: number): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.api}/project/${projectId}`);
  }

  getById(id: number): Observable<Contract> {
    return this.http.get<Contract>(`${this.api}/${id}`);
  }
}
