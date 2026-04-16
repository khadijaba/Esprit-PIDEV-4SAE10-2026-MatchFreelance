import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ContractSummary } from '../models/contract.model';

@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly api = '/api/contracts';

  constructor(private http: HttpClient) {}

  listByProject(projectId: number): Observable<ContractSummary[]> {
    return this.http.get<ContractSummary[]>(`${this.api}/project/${projectId}`);
  }
}
