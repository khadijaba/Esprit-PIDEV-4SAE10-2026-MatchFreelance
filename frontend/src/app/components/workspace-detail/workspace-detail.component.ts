import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { WorkspaceService } from '../../services/workspace.service';
import { MilestoneService } from '../../services/milestone.service';
import { ToastService } from '../../services/toast.service';
import { Workspace, Milestone, MilestoneStatus, CreateMilestoneRequest, WorkspaceStatus } from '../../models/workspace.model';

@Component({
  selector: 'app-workspace-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './workspace-detail.component.html',
})
export class WorkspaceDetailComponent implements OnInit {
  workspace?: Workspace;
  milestones: Milestone[] = [];
  loading = true;
  showMilestoneForm = false;
  editingMilestone: Milestone | null = null;

  newMilestone: CreateMilestoneRequest = {
    title: '',
    description: '',
    deadline: '',
    budgetAllocated: 0,
    status: 'TODO',
    workspaceId: 0,
  };

  statusColumns: MilestoneStatus[] = ['TODO', 'IN_PROGRESS', 'REVIEW', 'COMPLETED'];

  constructor(
    private workspaceService: WorkspaceService,
    private milestoneService: MilestoneService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.loadWorkspace(id);
  }

  loadWorkspace(id: number) {
    this.loading = true;
    this.workspaceService.getById(id).subscribe({
      next: (w) => {
        this.workspace = w;
        this.milestones = w.milestones || [];
        this.loading = false;
      },
      error: () => {
        this.toast.error('Workspace not found');
        this.router.navigate(['/admin/workspaces']);
      },
    });
  }

  getMilestonesByStatus(status: MilestoneStatus): Milestone[] {
    return this.milestones.filter((m) => m.status === status);
  }

  statusLabel(status: MilestoneStatus): string {
    const labels: Record<MilestoneStatus, string> = {
      TODO: 'To Do',
      IN_PROGRESS: 'In Progress',
      REVIEW: 'Review',
      COMPLETED: 'Completed',
      OVERDUE: 'Overdue',
    };
    return labels[status] ?? status;
  }

  statusColor(status: MilestoneStatus): string {
    const map: Record<MilestoneStatus, string> = {
      TODO: 'bg-gray-100 text-gray-700 border-gray-200',
      IN_PROGRESS: 'bg-blue-100 text-blue-700 border-blue-200',
      REVIEW: 'bg-amber-100 text-amber-700 border-amber-200',
      COMPLETED: 'bg-emerald-100 text-emerald-700 border-emerald-200',
      OVERDUE: 'bg-red-100 text-red-700 border-red-200',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700 border-gray-200';
  }

  columnHeaderColor(status: MilestoneStatus): string {
    const map: Record<MilestoneStatus, string> = {
      TODO: 'border-t-gray-400',
      IN_PROGRESS: 'border-t-blue-500',
      REVIEW: 'border-t-amber-500',
      COMPLETED: 'border-t-emerald-500',
      OVERDUE: 'border-t-red-500',
    };
    return map[status] ?? 'border-t-gray-400';
  }

  workspaceStatusClass(status: WorkspaceStatus): string {
    const map: Record<WorkspaceStatus, string> = {
      ACTIVE: 'bg-emerald-100 text-emerald-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      ARCHIVED: 'bg-gray-100 text-gray-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  progressColor(progress: number): string {
    if (progress >= 75) return 'bg-emerald-500';
    if (progress >= 50) return 'bg-blue-500';
    if (progress >= 25) return 'bg-amber-500';
    return 'bg-gray-400';
  }

  openMilestoneForm(milestone?: Milestone) {
    if (milestone) {
      this.editingMilestone = milestone;
      this.newMilestone = {
        title: milestone.title,
        description: milestone.description,
        deadline: milestone.deadline,
        budgetAllocated: milestone.budgetAllocated,
        status: milestone.status,
        workspaceId: this.workspace!.id,
        assigneeId: milestone.assigneeId ?? undefined,
      };
    } else {
      this.editingMilestone = null;
      this.newMilestone = {
        title: '',
        description: '',
        deadline: '',
        budgetAllocated: 0,
        status: 'TODO',
        workspaceId: this.workspace!.id,
      };
    }
    this.showMilestoneForm = true;
  }

  closeMilestoneForm() {
    this.showMilestoneForm = false;
    this.editingMilestone = null;
  }

  saveMilestone() {
    if (!this.newMilestone.title.trim()) {
      this.toast.error('Title is required');
      return;
    }

    if (this.editingMilestone) {
      this.milestoneService.update(this.editingMilestone.id, this.newMilestone).subscribe({
        next: () => {
          this.toast.success('Milestone updated');
          this.closeMilestoneForm();
          this.loadWorkspace(this.workspace!.id);
        },
        error: () => this.toast.error('Failed to update milestone'),
      });
    } else {
      this.milestoneService.create(this.newMilestone).subscribe({
        next: () => {
          this.toast.success('Milestone created');
          this.closeMilestoneForm();
          this.loadWorkspace(this.workspace!.id);
        },
        error: () => this.toast.error('Failed to create milestone'),
      });
    }
  }

  updateMilestoneProgress(milestone: Milestone, progress: number) {
    this.milestoneService.updateProgress(milestone.id, progress).subscribe({
      next: () => {
        this.loadWorkspace(this.workspace!.id);
      },
      error: () => this.toast.error('Failed to update progress'),
    });
  }

  completeMilestone(milestone: Milestone) {
    this.milestoneService.complete(milestone.id).subscribe({
      next: () => {
        this.toast.success('Milestone completed!');
        this.loadWorkspace(this.workspace!.id);
      },
      error: () => this.toast.error('Failed to complete milestone'),
    });
  }

  deleteMilestone(id: number) {
    if (!confirm('Delete this milestone?')) return;
    this.milestoneService.delete(id).subscribe({
      next: () => {
        this.toast.success('Milestone deleted');
        this.loadWorkspace(this.workspace!.id);
      },
      error: () => this.toast.error('Failed to delete milestone'),
    });
  }

  deleteWorkspace() {
    if (!confirm('Delete this workspace and all milestones?')) return;
    this.workspaceService.delete(this.workspace!.id).subscribe({
      next: () => {
        this.toast.success('Workspace deleted');
        this.router.navigate(['/admin/workspaces']);
      },
      error: () => this.toast.error('Failed to delete workspace'),
    });
  }

  archiveWorkspace() {
    this.workspaceService.updateStatus(this.workspace!.id, 'ARCHIVED').subscribe({
      next: () => {
        this.toast.success('Workspace archived');
        this.loadWorkspace(this.workspace!.id);
      },
      error: () => this.toast.error('Failed to archive workspace'),
    });
  }
}
