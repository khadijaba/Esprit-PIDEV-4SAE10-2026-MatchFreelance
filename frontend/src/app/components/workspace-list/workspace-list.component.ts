import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WorkspaceService } from '../../services/workspace.service';
import { ToastService } from '../../services/toast.service';
import { Workspace, WorkspaceStatus } from '../../models/workspace.model';

@Component({
  selector: 'app-workspace-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './workspace-list.component.html',
})
export class WorkspaceListComponent implements OnInit {
  workspaces: Workspace[] = [];
  loading = true;

  constructor(
    private workspaceService: WorkspaceService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.workspaceService.getAll().subscribe({
      next: (data) => {
        this.workspaces = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load workspaces');
      },
    });
  }

  onDelete(id: number) {
    if (!confirm('Delete this workspace and all its milestones?')) return;
    this.workspaceService.delete(id).subscribe({
      next: () => {
        this.toast.success('Workspace deleted');
        this.load();
      },
      error: () => this.toast.error('Failed to delete'),
    });
  }

  statusClass(status: WorkspaceStatus): string {
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
}
