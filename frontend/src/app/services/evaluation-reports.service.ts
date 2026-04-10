import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = '/api/evaluation-reports';

export interface HistogramPayload {
  scores: number[];
  title?: string;
}

export interface EvolutionPayload {
  labels: string[];
  values: number[];
  title?: string;
}

export interface PdfReportPayload {
  title?: string;
  subtitle?: string;
  scores: number[];
  evolution_labels: string[];
  evolution_values: number[];
}

@Injectable({ providedIn: 'root' })
export class EvaluationReportsService {
  constructor(private http: HttpClient) {}

  histogram(body: HistogramPayload): Observable<Blob> {
    return this.http.post(`${BASE}/charts/histogram`, body, { responseType: 'blob' });
  }

  evolution(body: EvolutionPayload): Observable<Blob> {
    return this.http.post(`${BASE}/charts/evolution`, body, { responseType: 'blob' });
  }

  pdfReport(body: PdfReportPayload): Observable<Blob> {
    return this.http.post(`${BASE}/reports/pdf`, body, { responseType: 'blob' });
  }
}
