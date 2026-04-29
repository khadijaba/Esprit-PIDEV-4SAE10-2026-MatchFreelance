import { TestBed } from '@angular/core/testing';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { RapportsComponent } from './rapports.component';
import { ExamenService } from '../../services/examen.service';
import { EvaluationReportsService } from '../../services/evaluation-reports.service';
import { Examen, PassageExamen } from '../../models/examen.model';

describe('RapportsComponent', () => {
  let component: RapportsComponent;

  let examenServiceMock: {
    getAll: ReturnType<typeof vi.fn>;
    getResultatsByExamen: ReturnType<typeof vi.fn>;
  };
  let reportsServiceMock: {
    histogram: ReturnType<typeof vi.fn>;
    evolution: ReturnType<typeof vi.fn>;
    pdfReport: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    examenServiceMock = {
      getAll: vi.fn().mockReturnValue(of([])),
      getResultatsByExamen: vi.fn().mockReturnValue(of([])),
    };
    reportsServiceMock = {
      histogram: vi.fn().mockReturnValue(of(new Blob(['histo']))),
      evolution: vi.fn().mockReturnValue(of(new Blob(['evo']))),
      pdfReport: vi.fn().mockReturnValue(of(new Blob(['pdf']))),
    };

    await TestBed.configureTestingModule({
      imports: [RapportsComponent],
      providers: [
        { provide: ExamenService, useValue: examenServiceMock as unknown as ExamenService },
        { provide: EvaluationReportsService, useValue: reportsServiceMock as unknown as EvaluationReportsService },
      ],
    })
      .overrideComponent(RapportsComponent, {
        set: { template: '<div></div>' },
      })
      .compileComponents();

    const fixture = TestBed.createComponent(RapportsComponent);
    component = fixture.componentInstance;
  });

  it('loads examens on init', () => {
    const examens: Examen[] = [{ id: 1, formationId: 2, titre: 'Examen A', seuilReussi: 60, questions: [] }];
    examenServiceMock.getAll.mockReturnValue(of(examens));

    component.ngOnInit();

    expect(component.examens).toEqual(examens);
    expect(component.examensLoading).toBe(false);
    expect(component.examensError).toBeNull();
  });

  it('sets error when no exam selected before generating reports', () => {
    component.selectedExamenId = null;

    component.genererRapportsEvaluation();

    expect(component.errorPython).toContain('Sélectionnez un examen');
  });

  it('shows empty state when selected exam has no passages', () => {
    component.selectedExamenId = 10;
    examenServiceMock.getResultatsByExamen.mockReturnValue(of([]));

    component.genererRapportsEvaluation();

    expect(component.showEmptyPassages).toBe(true);
    expect(component.loadingPython).toBe(false);
  });

  it('loads passage count on exam selection change', () => {
    const passages: PassageExamen[] = [
      {
        freelancerId: 1,
        examenId: 10,
        examenTitre: 'E1',
        score: 80,
        resultat: 'REUSSI',
        datePassage: '2026-04-01T10:00:00Z',
      },
    ];
    component.selectedExamenId = 10;
    examenServiceMock.getResultatsByExamen.mockReturnValue(of(passages));

    component.onExamenSelectionChange();

    expect(component.passageCount).toBe(1);
    expect(component.infoPython).toContain('1 passage(s)');
    expect(component.errorPython).toBeNull();
  });

  it('sets service unavailable message when chart generation fails', () => {
    const passages: PassageExamen[] = [
      {
        freelancerId: 1,
        examenId: 10,
        examenTitre: 'E1',
        score: 80,
        resultat: 'REUSSI',
        datePassage: '2026-04-01T10:00:00Z',
      },
    ];
    component.selectedExamenId = 10;
    examenServiceMock.getResultatsByExamen.mockReturnValue(of(passages));
    reportsServiceMock.histogram.mockReturnValue(throwError(() => ({ status: 0 })));

    component.genererRapportsEvaluation();

    expect(component.errorPython).toContain('Service Python indisponible');
    expect(component.loadingPython).toBe(false);
  });
});
