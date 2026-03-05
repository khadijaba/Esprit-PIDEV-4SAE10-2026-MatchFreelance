import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService } from '../../services/contract.service';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../services/toast.service';
import { InterviewService } from '../../services/interview.service';
import { Project, ProjectStatus, ContractSummary } from '../../models/project.model';
import { Candidature, CandidatureStatus } from '../../models/candidature.model';
import { TopFreelancerInInterviews } from '../../models/interview.model';
import { InterviewScheduleComponent } from '../interview-schedule/interview-schedule.component';

@Component({
  selector: 'app-client-project-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, InterviewScheduleComponent],
  templateUrl: './client-project-detail.component.html',
})
export class ClientProjectDetailComponent implements OnInit {
  project?: Project;
  candidatures: Candidature[] = [];
  freelancerNames: Record<number, string> = {};
  topFreelancers: TopFreelancerInInterviews[] = [];
  loading = true;
  get clientId(): number {
    return this.auth.currentUser()?.id ?? 0;
  }
  animatingId: number | null = null;
  animatingDirection: 'accept' | 'reject' | null = null;
  contractActionLoading = false;

  constructor(
    private auth: AuthService,
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private contractService: ContractService,
    private userService: UserService,
    private toast: ToastService,
    private interviewService: InterviewService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        if (p.clientId !== this.clientId) {
          this.toast.error('You are not the owner of this project');
          this.router.navigate(['/client']);
          return;
        }
        this.loadCandidatures(id);
        this.loadContractsIfNeeded(id);
        this.loadTopFreelancers();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Project not found');
        this.router.navigate(['/client']);
      },
    });
  }

  loadContractsIfNeeded(projectId: number) {
    if (!this.project || (this.project.status !== 'IN_PROGRESS' && this.project.status !== 'COMPLETED')) return;
    if (this.project.contracts && this.project.contracts.length > 0) return;
    this.contractService.getByProjectId(projectId).subscribe({
      next: (contracts) => {
        if (this.project) {
          this.project.contracts = contracts.map((c) => ({
            id: c.id,
            projectId: c.projectId,
            freelancerId: c.freelancerId,
            clientId: c.clientId,
            terms: c.terms,
            proposedBudget: c.proposedBudget,
            applicationMessage: c.applicationMessage,
            status: c.status,
            startDate: c.startDate,
            endDate: c.endDate,
            createdAt: c.createdAt,
          }));
          this.loadFreelancerNames();
        }
      },
    });
  }

  loadCandidatures(projectId: number) {
    this.candidatureService.getByProjectId(projectId).subscribe({
      next: (data) => {
        this.candidatures = data;
        this.loadFreelancerNames();
      },
    });
  }

  private loadFreelancerNames() {
    const ids: number[] = this.candidatures.map((c) => c.freelancerId);
    if (this.project?.contracts) {
      this.project.contracts.forEach((c) => ids.push(c.freelancerId));
    }
    this.topFreelancers.forEach((t) => ids.push(t.freelancerId));
    this.userService.getDisplayNamesMap(ids).subscribe({
      next: (map) => (this.freelancerNames = { ...this.freelancerNames, ...map }),
    });
  }

  private loadTopFreelancers() {
    const ownerId = this.clientId;
    if (!ownerId) return;
    this.interviewService.getTopFreelancers({ ownerId, limit: 5 }).subscribe({
      next: (list) => {
        this.topFreelancers = list;
        this.loadFreelancerNames();
      },
    });
  }

  onAccept(c: Candidature) {
    this.candidatureService.accept(c.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Freelancer accepted');
        this.animatingId = c.id;
        this.animatingDirection = 'accept';
        setTimeout(() => {
          if (this.project) {
            this.loadCandidatures(this.project.id);
            this.projectService.getById(this.project.id).subscribe({
              next: (p) => {
                this.project = p;
                this.loadContractsIfNeeded(p.id);
              },
            });
          }
          this.animatingId = null;
          this.animatingDirection = null;
        }, 400);
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to accept'),
    });
  }

  onReject(c: Candidature) {
    this.candidatureService.reject(c.id).subscribe({
      next: () => {
        this.toast.success('Application rejected');
        this.animatingId = c.id;
        this.animatingDirection = 'reject';
        setTimeout(() => {
          if (this.project) this.loadCandidatures(this.project.id);
          this.animatingId = null;
          this.animatingDirection = null;
        }, 400);
      },
      error: () => this.toast.error('Failed to reject'),
    });
  }

  onPay(contract: ContractSummary) {
    this.contractActionLoading = true;
    this.candidatureService.payContract(contract.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Payment recorded. Project completed.');
        this.contractActionLoading = false;
        if (this.project) {
          this.projectService.getById(this.project.id).subscribe({
            next: (p) => {
              this.project = p;
              this.loadContractsIfNeeded(p.id);
            },
          });
        }
      },
      error: (err) => {
        this.contractActionLoading = false;
        this.toast.error(err?.error?.message || 'Failed to mark as paid');
      },
    });
  }

  onCancel(contract: ContractSummary) {
    if (!confirm('Cancel this contract and reopen the project to choose another freelancer?')) return;
    this.contractActionLoading = true;
    this.candidatureService.cancelContract(contract.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Contract cancelled. Project reopened.');
        this.contractActionLoading = false;
        if (this.project) {
          this.projectService.getById(this.project.id).subscribe({
            next: (p) => {
              this.project = p;
              this.loadCandidatures(p.id);
              this.loadContractsIfNeeded(p.id);
            },
          });
        }
      },
      error: (err) => {
        this.contractActionLoading = false;
        this.toast.error(err?.error?.message || 'Failed to cancel contract');
      },
    });
  }

  statusClass(status: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      OPEN: 'bg-emerald-100 text-emerald-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  candidatureStatusClass(status: CandidatureStatus): string {
    const map: Record<CandidatureStatus, string> = {
      PENDING: 'bg-amber-100 text-amber-700',
      ACCEPTED: 'bg-emerald-100 text-emerald-700',
      REJECTED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }
}
