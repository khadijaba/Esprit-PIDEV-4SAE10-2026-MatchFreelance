import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { CandidatureService } from '../../services/candidature.service';
import { ProjectService } from '../../services/project.service';
import { ProjectSupervisionService } from '../../services/project-supervision.service';
import { ToastService } from '../../services/toast.service';
import { httpErrorMessage } from '../../utils/http-error.util';
import { Candidature } from '../../models/candidature.model';
import { Project } from '../../models/project.model';
import { CreateDeliverableRequest, DeliverableType, PhaseDeliverable, PhaseMeeting, ProjectPhase, ProjectPhaseStatus } from '../../models/project-supervision.model';
import { HttpErrorResponse } from '@angular/common/http';

type FreelancerMission = {
  candidature: Candidature;
  project: Project;
};

@Component({
  selector: 'app-freelancer-project-supervision',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './freelancer-project-supervision.component.html',
})
export class FreelancerProjectSupervisionComponent implements OnInit {
  loading = true;
  freelancerId!: number;
  missions: FreelancerMission[] = [];
  selectedMission: FreelancerMission | null = null;
  phases: ProjectPhase[] = [];
  selectedPhaseId: number | null = null;
  deliverables: PhaseDeliverable[] = [];
  meetings: PhaseMeeting[] = [];

  creatingDeliverable = false;
  newDeliverable: CreateDeliverableRequest = {
    title: '',
    description: '',
    type: 'CODE',
  };
  readonly deliverableTypes: DeliverableType[] = ['DOC', 'DESIGN', 'CODE', 'DEMO', 'REPORT'];

  constructor(
    private auth: AuthService,
    private candidatureService: CandidatureService,
    private projectService: ProjectService,
    private supervisionService: ProjectSupervisionService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    if (!user?.id) {
      this.toast.error('Connectez-vous en tant que freelancer.');
      this.loading = false;
      return;
    }
    this.freelancerId = Number(user.id);
    void this.auth.ensureFreshTokenIfNeeded();
    this.loadMissions();
  }

  loadMissions(): void {
    this.loading = true;
    this.candidatureService.listByFreelancer(this.freelancerId).subscribe({
      next: (rows) => {
        const accepted = rows.filter((c) => String(c.status).toUpperCase() === 'ACCEPTED');
        if (!accepted.length) {
          this.missions = [];
          this.selectedMission = null;
          this.loading = false;
          return;
        }
        const requests = accepted.map((c) =>
          this.projectService.getById(c.projectId).pipe(
            catchError(() => of(null))
          )
        );
        forkJoin(requests).subscribe({
          next: (projects) => {
            this.missions = accepted
              .map((c, i) => ({ candidature: c, project: projects[i] }))
              .filter((x): x is FreelancerMission => !!x.project);
            this.loading = false;
            if (this.missions.length) {
              this.selectMission(this.missions[0]);
            }
          },
          error: (err: HttpErrorResponse) => {
            this.loading = false;
            this.toast.error(httpErrorMessage(err, 'Impossible de charger les projets.'));
          },
        });
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.toast.error(httpErrorMessage(err, 'Impossible de charger vos candidatures.'));
      },
    });
  }

  selectMission(mission: FreelancerMission): void {
    this.selectedMission = mission;
    this.selectedPhaseId = null;
    this.deliverables = [];
    this.meetings = [];
    this.loadPhases();
  }

  loadPhases(): void {
    if (!this.selectedMission) return;
    this.supervisionService.listPhases(this.selectedMission.project.id).subscribe({
      next: (phases) => {
        this.phases = [...phases].sort((a, b) => a.phaseOrder - b.phaseOrder);
        if (this.phases.length) {
          this.selectPhase(this.phases[0].id);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(httpErrorMessage(err, 'Impossible de charger les phases.'));
      },
    });
  }

  selectPhase(phaseId: number): void {
    if (!this.selectedMission) return;
    this.selectedPhaseId = phaseId;
    forkJoin({
      deliverables: this.supervisionService.listDeliverables(this.selectedMission.project.id, phaseId),
      meetings: this.supervisionService.listMeetings(this.selectedMission.project.id, phaseId),
    }).subscribe({
      next: ({ deliverables, meetings }) => {
        this.deliverables = deliverables;
        this.meetings = meetings;
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(httpErrorMessage(err, 'Impossible de charger les détails de phase.'));
      },
    });
  }

  isCurrentPhase(phase: ProjectPhase): boolean {
    const active = this.currentPhase;
    return !!active && active.id === phase.id;
  }

  get currentPhase(): ProjectPhase | null {
    if (!this.phases.length) return null;
    const byPriority: ProjectPhaseStatus[] = ['IN_PROGRESS', 'BLOCKED', 'IN_REVIEW', 'PLANNED', 'APPROVED'];
    for (const st of byPriority) {
      const found = this.phases.find((p) => p.status === st);
      if (found) return found;
    }
    return this.phases[0] ?? null;
  }

  submitDeliverable(): void {
    if (!this.selectedMission || !this.selectedPhaseId) return;
    if (!this.newDeliverable.title.trim()) {
      this.toast.error('Titre du livrable requis.');
      return;
    }
    this.creatingDeliverable = true;
    const payload: CreateDeliverableRequest = {
      title: this.newDeliverable.title.trim(),
      description: this.newDeliverable.description?.trim() || '',
      type: this.newDeliverable.type,
    };
    this.supervisionService.createDeliverable(this.selectedMission.project.id, this.selectedPhaseId, payload).subscribe({
      next: () => {
        this.creatingDeliverable = false;
        this.newDeliverable.title = '';
        this.newDeliverable.description = '';
        this.newDeliverable.type = 'CODE';
        this.toast.success('Livrable soumis.');
        this.selectPhase(this.selectedPhaseId!);
      },
      error: (err: HttpErrorResponse) => {
        this.creatingDeliverable = false;
        this.toast.error(httpErrorMessage(err, 'Soumission du livrable impossible.'));
      },
    });
  }

  phaseStatusClass(status: ProjectPhaseStatus): string {
    const map: Record<ProjectPhaseStatus, string> = {
      PLANNED: 'bg-gray-100 text-gray-700',
      IN_PROGRESS: 'bg-blue-100 text-blue-700',
      IN_REVIEW: 'bg-violet-100 text-violet-700',
      APPROVED: 'bg-emerald-100 text-emerald-700',
      BLOCKED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }
}
