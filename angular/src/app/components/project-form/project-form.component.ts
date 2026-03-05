import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { ProjectRequest, ProjectStatus } from '../../models/project.model';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-form.component.html',
})
export class ProjectFormComponent implements OnInit {
  isEdit = false;
  projectId?: number;
  saving = false;
  error = '';

  form: ProjectRequest = {
    title: '',
    description: '',
    minBudget: 1000,
    maxBudget: 5000,
    duration: 1,
    status: 'OPEN',
  };

  statuses: { value: ProjectStatus; label: string }[] = [
    { value: 'OPEN', label: 'Open' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  constructor(
    private projectService: ProjectService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.projectId = +id;
      this.projectService.getById(this.projectId).subscribe({
        next: (p) => {
          this.form = {
            title: p.title,
            description: p.description,
            minBudget: p.minBudget,
            maxBudget: p.maxBudget,
            duration: p.duration,
            status: p.status,
          };
        },
        error: () => this.router.navigate(['/projects']),
      });
    }
  }

  onSubmit() {
    this.saving = true;
    this.error = '';

    const obs = this.isEdit
      ? this.projectService.update(this.projectId!, this.form)
      : this.projectService.create(this.form);

    obs.subscribe({
      next: (p) => {
        this.toast.success(this.isEdit ? 'Project updated successfully' : 'Project created successfully');
        this.router.navigate(['/projects', p.id]);
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.message || 'Something went wrong. Please check your inputs.';
        this.error = msg;
        this.toast.error(msg);
      },
    });
  }
}

