import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Contract, ContractRequest } from '../models/contract.model';

@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly api = '/api/contracts';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Contract[]> {
    return this.http.get<Contract[]>(this.api);
  }

  getById(id: number): Observable<Contract> {
    return this.http.get<Contract>(`${this.api}/${id}`);
  }

  getByProjectId(projectId: number): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.api}/project/${projectId}`);
  }

  getByFreelancerId(freelancerId: number): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  getByClientId(clientId: number): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.api}/client/${clientId}`);
  }

  create(req: ContractRequest): Observable<Contract> {
    return this.http.post<Contract>(this.api, req);
  }

  update(id: number, req: ContractRequest): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
