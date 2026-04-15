import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface PreviewResponse {
  previewId: number;
  contractId: number;
  htmlUrl: string;
  screenshotUrl?: string;
  generatedAt: string;
  version: number;
  status: string;
  designStyle: string;
  featuresCount: number;
  features?: string[];
  clientFeedback?: string;
}

export interface PreviewFeedback {
  feedback: string;
  status?: string;
}

export interface RegenerateRequest {
  feedback: string;
  designStyle?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ContractPreviewService {
  private baseUrl = 'http://localhost:8081/api/contracts';

  constructor(private http: HttpClient) {}

  async generatePreview(contractId: number, designStyle: string = 'modern'): Promise<PreviewResponse> {
    return firstValueFrom(
      this.http.post<PreviewResponse>(
        `${this.baseUrl}/${contractId}/preview?designStyle=${designStyle}`,
        {}
      )
    );
  }

  async getContractPreviews(contractId: number): Promise<PreviewResponse[]> {
    return firstValueFrom(
      this.http.get<PreviewResponse[]>(`${this.baseUrl}/${contractId}/preview`)
    );
  }

  async submitFeedback(contractId: number, previewId: number, feedback: PreviewFeedback): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(
        `${this.baseUrl}/${contractId}/preview/${previewId}/feedback`,
        feedback
      )
    );
  }

  async regeneratePreview(
    contractId: number,
    previewId: number,
    request: RegenerateRequest
  ): Promise<PreviewResponse> {
    return firstValueFrom(
      this.http.post<PreviewResponse>(
        `${this.baseUrl}/${contractId}/preview/${previewId}/regenerate`,
        request
      )
    );
  }
}
