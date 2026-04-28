import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { EvaluationReportsService } from './evaluation-reports.service';

describe('EvaluationReportsService', () => {
  let service: EvaluationReportsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EvaluationReportsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EvaluationReportsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('calls histogram endpoint with payload', () => {
    const payload = { scores: [10, 20], title: 'histo' };

    service.histogram(payload).subscribe();

    const req = httpMock.expectOne('/api/evaluation-reports/charts/histogram');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['ok']));
  });

  it('calls evolution endpoint with payload', () => {
    const payload = { labels: ['Jan'], values: [77], title: 'evo' };

    service.evolution(payload).subscribe();

    const req = httpMock.expectOne('/api/evaluation-reports/charts/evolution');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['ok']));
  });

  it('calls pdf report endpoint with payload', () => {
    const payload = {
      title: 'Rapport',
      subtitle: 'Sub',
      scores: [50],
      evolution_labels: ['Jan'],
      evolution_values: [50],
    };

    service.pdfReport(payload).subscribe();

    const req = httpMock.expectOne('/api/evaluation-reports/reports/pdf');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['ok']));
  });
});
