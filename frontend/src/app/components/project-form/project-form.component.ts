import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
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

  form: ProjectRequest & { requiredSkillsStr?: string } = {
    title: '',
    description: '',
    budget: 0,
    duration: 1,
    status: 'OPEN',
    requiredSkillsStr: '',
  };

  statuses: { value: ProjectStatus; label: string }[] = [
    { value: 'OPEN', label: 'Open' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  constructor(
    private projectService: ProjectService,
    private auth: AuthService,
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
            budget: p.budget,
            duration: p.duration,
            status: p.status,
            projectOwnerId: p.projectOwnerId,
            requiredSkills: p.requiredSkills,
            requiredSkillsStr: (p.requiredSkills || []).join(', '),
          };
        },
        error: () => this.router.navigate(['/projects']),
      });
    }
  }

  onSubmit() {
    this.saving = true;
    this.error = '';

    const payload: ProjectRequest = {
      title: this.form.title,
      description: this.form.description,
      budget: this.form.budget,
      duration: this.form.duration,
      status: this.form.status,
      projectOwnerId: this.form.projectOwnerId,
      requiredSkills: this.form.requiredSkillsStr
        ? this.form.requiredSkillsStr.split(',').map((s) => s.trim()).filter(Boolean)
        : undefined,
    };
    if (!this.isEdit && payload.projectOwnerId == null) {
      payload.projectOwnerId = this.auth.getStoredUser()?.userId ?? 0;
    }

    const obs = this.isEdit
      ? this.projectService.update(this.projectId!, payload)
      : this.projectService.create(payload);

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

