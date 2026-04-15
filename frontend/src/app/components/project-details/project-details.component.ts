import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmService } from '../../services/confirm.service';
import { TalentMatchingService } from '../../services/talent-matching.service';
import { normalizeProjectRequiredSkills } from '../../utils/project-skill.util';
import { Project, ProjectStatus } from '../../models/project.model';
import { MatchingFreelancer } from '../../models/matching.model';
import { ProjectEffortEstimate } from '../../models/project-effort.model';
import { ProjectMlRisk } from '../../models/project-ml-risk.model';
import { httpErrorMessage } from '../../utils/http-error.util';

@Component({
  selector: 'app-project-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './project-details.component.html',
})
export class ProjectDetailsComponent implements OnInit {
  project?: Project;
  loading = true;
  matchingFreelancers: MatchingFreelancer[] = [];
  matchingLoading = false;
  mlRisk?: ProjectMlRisk;
  mlRiskLoading = false;
  effort?: ProjectEffortEstimate;
  effortLoading = false;
  effortError: string | null = null;

  constructor(
    private projectService: ProjectService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router,
    private confirmService: ConfirmService,
    private matchingService: TalentMatchingService
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = normalizeProjectRequiredSkills(p);
        this.loading = false;
        this.loadMatching();
        this.loadMlRisk();
        this.loadEffort();
      },
      error: () => {
        this.toast.error('Project not found');
        this.router.navigate(['/admin/projects']);
      },
    });
  }

  reloadEffort(): void {
    this.effortError = null;
    this.effort = undefined;
    this.loadEffort();
  }

  private loadEffort(): void {
    if (!this.project?.id) return;
    this.effortLoading = true;
    this.effortError = null;
    this.projectService.getEffortEstimate(this.project.id).subscribe({
      next: (e) => {
        this.effort = e;
        this.effortLoading = false;
      },
      error: (err: unknown) => {
        this.effortLoading = false;
        this.effort = undefined;
        this.effortError = httpErrorMessage(
          err,
          'Impossible de charger la charge estimée. Vérifiez le microservice Project et la gateway (8086).'
        );
        this.toast.error(this.effortError);
      },
    });
  }

  private loadMlRisk(): void {
    if (!this.project?.id) return;
    this.mlRiskLoading = true;
    this.projectService.getMlRisk(this.project.id).subscribe({
      next: (r) => {
        this.mlRisk = r;
        this.mlRiskLoading = false;
      },
      error: () => {
        this.mlRiskLoading = false;
      },
    });
  }

  private loadMatching(): void {
    if (!this.project?.id) return;
    this.matchingLoading = true;
    this.matchingService.getTopMatchingByProjectId(this.project.id).subscribe({
      next: (list) => {
        this.matchingFreelancers = list;
        this.matchingLoading = false;
      },
      error: () => {
        this.matchingLoading = false;
      },
    });
  }

  async onDelete() {
    if (!this.project) return;
    const ok = await this.confirmService.confirm('Supprimer ce projet ?');
    if (!ok) return;
    this.projectService.delete(this.project.id).subscribe({
      next: () => {
        this.toast.success('Project deleted');
        this.router.navigate(['/admin/projects']);
      },
      error: () => this.toast.error('Failed to delete project'),
    });
  }

  statusClass(status: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      DRAFT: 'bg-slate-200 text-slate-800',
      OPEN: 'bg-emerald-100 text-emerald-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  formatStatus(status: ProjectStatus): string {
    return status.replace('_', ' ');
  }

  mlRiskBadgeClass(level: string): string {
    const map: Record<string, string> = {
      LOW: 'bg-emerald-100 text-emerald-800',
      MEDIUM: 'bg-amber-100 text-amber-800',
      HIGH: 'bg-red-100 text-red-800',
    };
    return map[level] ?? 'bg-gray-100 text-gray-800';
  }
}

