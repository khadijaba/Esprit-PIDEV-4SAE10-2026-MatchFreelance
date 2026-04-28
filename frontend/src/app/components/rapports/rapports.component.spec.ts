import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RapportsComponent } from './rapports.component';
import { ExamenService } from '../../services/examen.service';
import { EvaluationReportsService } from '../../services/evaluation-reports.service';
import { Examen, PassageExamen } from '../../models/examen.model';

describe('RapportsComponent', () => {
  let component: RapportsComponent;

  let examenServiceMock: jasmine.SpyObj<ExamenService>;
  let reportsServiceMock: jasmine.SpyObj<EvaluationReportsService>;

  beforeEach(async () => {
    examenServiceMock = jasmine.createSpyObj<ExamenService>('ExamenService', ['getAll', 'getResultatsByExamen']);
    reportsServiceMock = jasmine.createSpyObj<EvaluationReportsService>('EvaluationReportsService', [
      'histogram',
      'evolution',
      'pdfReport',
    ]);

    examenServiceMock.getAll.and.returnValue(of([]));
    examenServiceMock.getResultatsByExamen.and.returnValue(of([]));
    reportsServiceMock.histogram.and.returnValue(of(new Blob(['histo'])));
    reportsServiceMock.evolution.and.returnValue(of(new Blob(['evo'])));
    reportsServiceMock.pdfReport.and.returnValue(of(new Blob(['pdf'])));

    await TestBed.configureTestingModule({
      imports: [RapportsComponent],
      providers: [
        { provide: ExamenService, useValue: examenServiceMock },
        { provide: EvaluationReportsService, useValue: reportsServiceMock },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(RapportsComponent);
    component = fixture.componentInstance;
  });

  it('loads examens on init', () => {
    const examens: Examen[] = [{ id: 1, formationId: 2, titre: 'Examen A', seuilReussi: 60, questions: [] }];
    examenServiceMock.getAll.and.returnValue(of(examens));

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
    examenServiceMock.getResultatsByExamen.and.returnValue(of([]));

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
    examenServiceMock.getResultatsByExamen.and.returnValue(of(passages));

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
    examenServiceMock.getResultatsByExamen.and.returnValue(of(passages));
    reportsServiceMock.histogram.and.returnValue(throwError(() => ({ status: 0 })));

    component.genererRapportsEvaluation();

    expect(component.errorPython).toContain('Service Python indisponible');
    expect(component.loadingPython).toBe(false);
  });
});
