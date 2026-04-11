import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FeedbackService } from '../../services/feedback.service';
import { ToastService } from '../../services/toast.service';
import { Feedback, FeedbackStatus } from '../../models/feedback.model';

@Component({
  selector: 'app-feedback-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './feedback-list.component.html',
})
export class FeedbackListComponent implements OnInit {
  feedbacks: Feedback[] = [];
  filtered: Feedback[] = [];
  statusFilter: string = 'ALL';
  categoryFilter: string = 'ALL';
  loading = true;

  statCounts = { total: 0, pending: 0, approved: 0, rejected: 0, flagged: 0, avgRating: 0 };

  constructor(private feedbackService: FeedbackService, private toast: ToastService) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.feedbackService.getAll().subscribe({
      next: (data) => {
        this.feedbacks = data;
        this.computeStats();
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load feedbacks');
      },
    });
  }

  computeStats() {
    this.statCounts.total = this.feedbacks.length;
    this.statCounts.pending = this.feedbacks.filter((f) => f.status === 'PENDING').length;
    this.statCounts.approved = this.feedbacks.filter((f) => f.status === 'APPROVED').length;
    this.statCounts.rejected = this.feedbacks.filter((f) => f.status === 'REJECTED').length;
    this.statCounts.flagged = this.feedbacks.filter((f) => f.status === 'FLAGGED').length;
    const rated = this.feedbacks.filter((f) => f.status === 'APPROVED');
    this.statCounts.avgRating = rated.length > 0 ? Math.round((rated.reduce((s, f) => s + f.rating, 0) / rated.length) * 10) / 10 : 0;
  }

  applyFilters() {
    this.filtered = this.feedbacks.filter((f) => {
      if (this.statusFilter !== 'ALL' && f.status !== this.statusFilter) return false;
      if (this.categoryFilter !== 'ALL' && f.category !== this.categoryFilter) return false;
      return true;
    });
  }

  approve(id: number) {
    this.feedbackService.approve(id).subscribe({
      next: () => { this.toast.success('Feedback approved'); this.load(); },
      error: () => this.toast.error('Failed to approve'),
    });
  }

  reject(id: number) {
    this.feedbackService.reject(id).subscribe({
      next: () => { this.toast.success('Feedback rejected'); this.load(); },
      error: () => this.toast.error('Failed to reject'),
    });
  }

  flag(id: number) {
    this.feedbackService.flag(id).subscribe({
      next: () => { this.toast.success('Feedback flagged'); this.load(); },
      error: () => this.toast.error('Failed to flag'),
    });
  }

  deleteFeedback(id: number) {
    if (!confirm('Delete this feedback?')) return;
    this.feedbackService.delete(id).subscribe({
      next: () => { this.toast.success('Feedback deleted'); this.load(); },
      error: () => this.toast.error('Failed to delete'),
    });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'bg-green-100 text-green-700';
      case 'PENDING': return 'bg-yellow-100 text-yellow-700';
      case 'REJECTED': return 'bg-red-100 text-red-700';
      case 'FLAGGED': return 'bg-orange-100 text-orange-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  }

  stars(rating: number): string[] {
    return Array(5).fill('').map((_, i) => (i < rating ? 'filled' : 'empty'));
  }
}
