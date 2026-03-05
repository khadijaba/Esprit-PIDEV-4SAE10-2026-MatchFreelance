import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { ProjectRequest } from '../../models/project.model';

@Component({
  selector: 'app-client-project-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './client-project-form.component.html',
})
export class ClientProjectFormComponent {
  saving = false;
  error = '';
  auth = inject(AuthService);
  get clientId(): number {
    return this.auth.currentUserId() ?? 1;
  }

  form: ProjectRequest = {
    title: '',
    description: '',
    minBudget: 1000,
    maxBudget: 5000,
    duration: 1,
    status: 'OPEN',
    clientId: 1,
  };

  constructor(
    private projectService: ProjectService,
    private toast: ToastService,
    private router: Router
  ) {}

  get budgetRangeInvalid(): boolean {
    const minB = Number(this.form.minBudget);
    const maxB = Number(this.form.maxBudget);
    return !Number.isNaN(minB) && !Number.isNaN(maxB) && maxB < minB;
  }

  onSubmit() {
    this.saving = true;
    this.error = '';
    this.form.clientId = this.clientId;

    const minB = Number(this.form.minBudget);
    const maxB = Number(this.form.maxBudget);
    if (Number.isNaN(minB) || minB <= 0) {
      this.saving = false;
      this.error = 'Min budget must be a positive number';
      this.toast.error(this.error);
      return;
    }
    if (Number.isNaN(maxB) || maxB <= 0) {
      this.saving = false;
      this.error = 'Max budget must be a positive number';
      this.toast.error(this.error);
      return;
    }
    if (maxB < minB) {
      this.saving = false;
      this.error = 'Max budget must be greater than or equal to min budget';
      this.toast.error(this.error);
      return;
    }
    const dur = Math.floor(Number(this.form.duration));
    if (Number.isNaN(dur) || dur < 1 || dur > 365) {
      this.saving = false;
      this.error = 'Duration must be between 1 and 365 days';
      this.toast.error(this.error);
      return;
    }

    const payload = {
      ...this.form,
      minBudget: minB,
      maxBudget: maxB,
      duration: dur,
    };

    this.projectService.create(payload).subscribe({
      next: (p) => {
        this.toast.success('Project created successfully');
        this.router.navigate(['/client/projects', p.id]);
      },
      error: (err) => {
        this.saving = false;
        const body = err?.error;
        const msg = (typeof body === 'object' && body?.message) || err?.error?.message || 'Something went wrong. Please check your inputs.';
        this.error = msg;
        this.toast.error(msg);
      },
    });
  }
}
