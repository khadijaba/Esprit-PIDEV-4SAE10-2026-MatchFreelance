import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InterviewService } from '../../services/interview.service';
import { ToastService } from '../../services/toast.service';
import { Interview, InterviewRequest, InterviewStatus } from '../../models/interview.model';

@Component({
  selector: 'app-interview-schedule',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interview-schedule.component.html',
})
export class InterviewScheduleComponent implements OnInit {
  @Input() candidatureId!: number;
  @Input() clientId!: number;
  @Output() interviewsChanged = new EventEmitter<void>();

  interviews: Interview[] = [];
  loading = true;
  showForm = false;
  formModel: InterviewRequest = { scheduledAt: '', notes: '' };

  constructor(
    private interviewService: InterviewService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.loadInterviews();
  }

  loadInterviews() {
    if (!this.candidatureId || !this.clientId) {
      this.loading = false;
      return;
    }
    this.interviewService.getByCandidatureId(this.candidatureId, this.clientId).subscribe({
      next: (data) => {
        this.interviews = data ?? [];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err?.error?.message || 'Failed to load interviews');
      },
    });
  }

  openForm() {
    this.showForm = true;
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    this.formModel = {
      scheduledAt: now.toISOString().slice(0, 16),
      notes: '',
    };
  }

  closeForm() {
    this.showForm = false;
  }

  scheduleInterview() {
    if (!this.formModel.scheduledAt) {
      this.toast.error('Date and time are required');
      return;
    }
    this.interviewService.schedule(this.candidatureId, this.clientId, {
      scheduledAt: new Date(this.formModel.scheduledAt).toISOString(),
      notes: this.formModel.notes,
    }).subscribe({
      next: () => {
        this.toast.success('Interview scheduled');
        this.loadInterviews();
        this.closeForm();
        this.interviewsChanged.emit();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to schedule interview'),
    });
  }

  updateStatus(interview: Interview, status: InterviewStatus) {
    this.interviewService.update(this.candidatureId, interview.id, this.clientId, {
      scheduledAt: interview.scheduledAt,
      status,
      notes: interview.notes,
    }).subscribe({
      next: () => {
        this.toast.success('Interview updated');
        this.loadInterviews();
        this.interviewsChanged.emit();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to update interview'),
    });
  }

  deleteInterview(interview: Interview) {
    if (!confirm('Cancel/delete this interview?')) return;
    this.interviewService.delete(this.candidatureId, interview.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Interview removed');
        this.loadInterviews();
        this.interviewsChanged.emit();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to remove interview'),
    });
  }

  statusClass(status: InterviewStatus): string {
    const map: Record<InterviewStatus, string> = {
      SCHEDULED: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-emerald-100 text-emerald-700',
      CANCELLED: 'bg-red-100 text-red-700',
      NO_SHOW: 'bg-slate-100 text-slate-600',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }
}
