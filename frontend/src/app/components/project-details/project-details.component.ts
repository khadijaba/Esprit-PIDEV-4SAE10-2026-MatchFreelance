import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';

@Component({
  selector: 'app-project-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './project-details.component.html',
})
export class ProjectDetailsComponent implements OnInit {
  project?: Project;
  loading = true;

  constructor(
    private projectService: ProjectService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        this.loading = false;
      },
      error: () => {
        this.toast.error('Project not found');
        this.router.navigate(['/projects']);
      },
    });
  }

  onDelete() {
    if (!this.project || !confirm('Delete this project?')) return;
    this.projectService.delete(this.project.id).subscribe({
      next: () => {
        this.toast.success('Project deleted');
        this.router.navigate(['/projects']);
      },
      error: () => this.toast.error('Failed to delete project'),
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

  formatStatus(status: ProjectStatus): string {
    return status.replace('_', ' ');
  }
}

