import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
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
  get clientId(): number {
    return this.auth.currentUser()?.id ?? 0;
  }

  form: ProjectRequest = {
    title: '',
    description: '',
    minBudget: 1000,
    maxBudget: 5000,
    duration: 1,
    status: 'OPEN',
    clientId: 0,
  };

  constructor(
    private auth: AuthService,
    private projectService: ProjectService,
    private toast: ToastService,
    private router: Router
  ) {}

  onSubmit() {
    this.saving = true;
    this.error = '';
    this.form.clientId = this.clientId;

    const minB = Number(this.form.minBudget);
    const maxB = Number(this.form.maxBudget);
    if (maxB < minB) {
      this.saving = false;
      this.error = 'Max budget must be greater than or equal to min budget';
      this.toast.error(this.error);
      return;
    }

    const payload = {
      ...this.form,
      minBudget: minB,
      maxBudget: maxB,
      duration: Math.floor(Number(this.form.duration)),
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
