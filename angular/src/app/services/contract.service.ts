import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Contract, ContractRequest } from '../models/contract.model';
import { FinancialSummary, ContractHealth, PaymentMilestone } from '../models/contract-advanced.model';

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

  proposeExtraBudget(contractId: number, amount: number, reason: string, freelancerId: number): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${contractId}/propose-extra-budget`, {
      amount,
      reason: reason || undefined,
      freelancerId,
    });
  }

  respondToExtraBudget(contractId: number, accept: boolean, clientId: number): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${contractId}/respond-extra-budget`, { accept, clientId });
  }

  updateProgress(contractId: number, progressPercent: number, freelancerId: number): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${contractId}/progress`, { progressPercent, freelancerId });
  }

  rateContract(contractId: number, rating: number, review: string | undefined, clientId: number): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${contractId}/rating`, {
      rating,
      review: review || undefined,
      clientId,
    });
  }

  downloadPdf(contractId: number): Observable<Blob> {
    return this.http.get(`${this.api}/${contractId}/pdf`, {
      responseType: 'blob',
    });
  }

  getFinancialSummary(contractId: number): Observable<FinancialSummary> {
    return this.http.get<FinancialSummary>(`${this.api}/${contractId}/financial-summary`);
  }

  getPaymentSchedule(contractId: number): Observable<PaymentMilestone[]> {
    return this.http.get<PaymentMilestone[]>(`${this.api}/${contractId}/payment-schedule`);
  }

  getContractHealth(contractId: number): Observable<ContractHealth> {
    return this.http.get<ContractHealth>(`${this.api}/${contractId}/health`);
  }
}
