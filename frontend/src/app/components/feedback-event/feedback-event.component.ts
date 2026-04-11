import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FeedbackService } from '../../services/feedback.service';
import { ToastService } from '../../services/toast.service';
import { Feedback, FeedbackStats, CreateFeedbackRequest, FeedbackCategory } from '../../models/feedback.model';

@Component({
  selector: 'app-feedback-event',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './feedback-event.component.html',
})
export class FeedbackEventComponent implements OnInit {
  eventId!: number;
  feedbacks: Feedback[] = [];
  stats: FeedbackStats | null = null;
  loading = true;
  showForm = false;

  form: CreateFeedbackRequest = {
    eventId: 0,
    rating: 5,
    comment: '',
    authorName: '',
    authorEmail: '',
    category: 'OVERALL',
    anonymous: false,
  };

  hoverRating = 0;
  Math = Math;

  constructor(
    private feedbackService: FeedbackService,
    private toast: ToastService,
    private route: ActivatedRoute,
    public router: Router
  ) {}

  ngOnInit() {
    this.eventId = +this.route.snapshot.paramMap.get('eventId')!;
    this.form.eventId = this.eventId;
    this.load();
  }

  load() {
    this.loading = true;
    this.feedbackService.getByEventId(this.eventId).subscribe({
      next: (data) => {
        this.feedbacks = data;
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
    this.feedbackService.getStatsByEventId(this.eventId).subscribe({
      next: (s) => this.stats = s,
    });
  }

  setRating(r: number) {
    this.form.rating = r;
  }

  submitFeedback() {
    if (!this.form.comment?.trim()) {
      this.toast.error('Please add a comment');
      return;
    }
    if (!this.form.anonymous && !this.form.authorName?.trim()) {
      this.toast.error('Please enter your name or check anonymous');
      return;
    }
    this.feedbackService.create(this.form).subscribe({
      next: () => {
        this.toast.success('Feedback submitted! It will appear after approval.');
        this.showForm = false;
        this.form = { eventId: this.eventId, rating: 5, comment: '', authorName: '', authorEmail: '', category: 'OVERALL', anonymous: false };
        this.load();
      },
      error: () => this.toast.error('Failed to submit feedback'),
    });
  }

  stars(rating: number): string[] {
    return Array(5).fill('').map((_, i) => (i < rating ? 'filled' : 'empty'));
  }

  barWidth(count: number): string {
    if (!this.stats || this.stats.totalFeedbacks === 0) return '0%';
    return Math.round((count / this.stats.totalFeedbacks) * 100) + '%';
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
}
