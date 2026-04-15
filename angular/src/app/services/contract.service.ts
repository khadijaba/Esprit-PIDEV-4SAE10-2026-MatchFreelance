import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Contract, ContractRequest } from '../models/contract.model';
import {
  FinancialSummary,
  ContractHealth,
  PaymentMilestone,
  ContractAiBriefing,
  ExtraBudgetAiAnalysis,
} from '../models/contract-advanced.model';

/** Matches contract-service ContractCancelPartyRequestDTO */
export interface ContractCancelPartyRequest {
  clientId?: number;
  freelancerId?: number;
}

/** Matches contract-service ContractPartyAmendRequestDTO */
export interface ContractPartyAmendRequest {
  actorClientId?: number;
  actorFreelancerId?: number;
  terms?: string;
  proposedBudget?: number;
  startDate?: string;
  endDate?: string;
  applicationMessage?: string;
}

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

  /** Client or freelancer cancels (body must include exactly one of clientId / freelancerId). */
  cancelByParty(id: number, body: ContractCancelPartyRequest): Observable<Contract> {
    return this.http.put<Contract>(`${this.api}/${id}/cancel`, body);
  }

  /** Partial update by client or freelancer (DRAFT or ACTIVE only). */
  partyAmend(id: number, body: ContractPartyAmendRequest): Observable<Contract> {
    return this.http.patch<Contract>(`${this.api}/${id}/party-amend`, body);
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

  /** Local LLM: analyze a proposed extra amount + reason before sending to client (does not save a proposal). */
  analyzeExtraBudget(
    contractId: number,
    body: { amount: number; reason: string | undefined; freelancerId: number }
  ): Observable<ExtraBudgetAiAnalysis> {
    return this.http.put<ExtraBudgetAiAnalysis>(`${this.api}/${contractId}/extra-budget-ai-analysis`, {
      amount: body.amount,
      reason: body.reason || undefined,
      freelancerId: body.freelancerId,
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

  downloadPdf(contractId: number, signature?: string): Observable<Blob> {
    if (signature) {
      // POST request with signature in body for large base64 images
      return this.http.post(`${this.api}/${contractId}/pdf`, 
        { signature }, 
        { responseType: 'blob' }
      );
    } else {
      // GET request without signature
      return this.http.get(`${this.api}/${contractId}/pdf`, { responseType: 'blob' });
    }
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

  /**
   * Local LLM briefing. Pass exactly one of viewerFreelancerId or viewerClientId (must match contract party).
   */
  getAiBriefing(
    contractId: number,
    viewer: { viewerFreelancerId: number } | { viewerClientId: number }
  ): Observable<ContractAiBriefing> {
    let params = new HttpParams();
    if ('viewerFreelancerId' in viewer) {
      params = params.set('viewerFreelancerId', String(viewer.viewerFreelancerId));
    } else {
      params = params.set('viewerClientId', String(viewer.viewerClientId));
    }
    return this.http.get<ContractAiBriefing>(`${this.api}/${contractId}/ai-briefing`, { params });
  }
}
