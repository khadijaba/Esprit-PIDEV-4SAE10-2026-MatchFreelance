import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Contract, ContractAmendPayload } from '../models/contract.model';
import { ContractMessage } from '../models/contract-message.model';

@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly api = '/api/contracts';

  constructor(private http: HttpClient) {}

  listByProject(projectId: number): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.api}/project/${projectId}`);
  }

  listByFreelancer(freelancerId: number): Observable<Contract[]> {
    return this.http.get<Contract[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  getById(id: number): Observable<Contract> {
    return this.http.get<Contract>(`${this.api}/${id}`);
  }

  update(id: number, body: ContractAmendPayload): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${id}`, body);
  }

  /** Annulation directe (microservice Contract) — réservé aux contrats ACTIVE. */
  cancel(id: number): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${id}/cancel`, {});
  }

  updateProgress(id: number, progressPercent: number, freelancerId: number): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${id}/progress`, { progressPercent, freelancerId });
  }

  proposeExtraBudget(id: number, amount: number, reason: string | undefined, freelancerId: number): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${id}/propose-extra-budget`, {
      amount,
      reason: reason?.trim() || undefined,
      freelancerId,
    });
  }

  getMessages(contractId: number): Observable<ContractMessage[]> {
    return this.http.get<ContractMessage[]>(`${this.api}/${contractId}/messages`);
  }

  sendMessage(contractId: number, senderId: number, content: string): Observable<ContractMessage> {
    return this.http.post<ContractMessage>(`${this.api}/${contractId}/messages`, { senderId, content });
  }
}
