import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { InterviewService } from '../../services/interview.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { UserService } from '../../services/user.service';
import {
  Interview,
  InterviewStatus,
  MeetingMode,
  PageResponse,
  ReviewCreateRequest,
  ReviewResponse,
} from '../../models/interview.model';

export type InterviewDetailRole = 'client' | 'freelancer';

@Component({
  selector: 'app-interview-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './interview-detail.component.html',
})
export class InterviewDetailComponent implements OnInit {
  interview: Interview | null = null;
  role: InterviewDetailRole = 'client';
  loading = true;
  projectName = '';
  reviewsPage: PageResponse<ReviewResponse> | null = null;
  reviewsLoading = false;
  reviewPageIndex = 0;
  readonly reviewPageSize = 10;
  otherPartyLabel = '';
  listRoute: string[] = [];
  downloadingIcs = false;
  submittingReview = false;
  reviewScore = 3;
  reviewComment = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private interviewService: InterviewService,
    private projectService: ProjectService,
    private auth: AuthService,
    private toast: ToastService,
    private userService: UserService
  ) {}

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? +idParam : 0;
    this.role = (this.route.snapshot.data['role'] as InterviewDetailRole) ?? 'client';
    this.listRoute = this.role === 'client' ? ['/client/interviews'] : ['/freelancer/interviews'];

    if (!id) {
      this.toast.error('Invalid interview');
      this.router.navigate(this.listRoute);
      return;
    }

    this.interviewService.getById(id).subscribe({
      next: (interview) => {
        this.interview = interview;
        this.loading = false;
        this.loadProjectName();
        this.loadOtherPartyLabel();
        this.loadReviews();
      },
      error: () => {
        this.loading = false;
        this.toast.error('Entretien introuvable');
        this.router.navigate(this.listRoute);
      },
    });
  }

  private loadProjectName() {
    if (!this.interview?.projectId) return;
    this.projectService.getById(this.interview.projectId).subscribe({
      next: (p) => (this.projectName = p.title ?? ''),
      error: () => {},
    });
  }

  private loadOtherPartyLabel() {
    if (!this.interview) return;
    const revieweeId = this.revieweeId();
    if (revieweeId == null) return;
    this.userService.getDisplayName(revieweeId).subscribe({
      next: (name) => (this.otherPartyLabel = name || `#${revieweeId}`),
      error: () => (this.otherPartyLabel = `#${revieweeId}`),
    });
  }

  loadReviews() {
    if (!this.interview) return;
    this.reviewsLoading = true;
    this.interviewService
      .getReviewsForInterview(this.interview.id, this.reviewPageIndex, this.reviewPageSize)
      .subscribe({
        next: (page) => {
          this.reviewsPage = page;
          this.reviewsLoading = false;
        },
        error: () => {
          this.reviewsLoading = false;
          this.toast.error('Impossible de charger les avis');
        },
      });
  }

  get currentUserId(): number | undefined {
    return this.auth.currentUser()?.id;
  }

  /** Whether the current user is a participant (owner or freelancer). */
  isParticipant(): boolean {
    if (!this.interview || this.currentUserId == null) return false;
    return (
      this.interview.ownerId === this.currentUserId ||
      this.interview.freelancerId === this.currentUserId
    );
  }

  /** The other party's user id (the one we would review). */
  revieweeId(): number | null {
    if (!this.interview || this.currentUserId == null) return null;
    if (this.interview.ownerId === this.currentUserId) return this.interview.freelancerId;
    if (this.interview.freelancerId === this.currentUserId) return this.interview.ownerId;
    return null;
  }

  /** True if current user has already submitted a review for this interview. */
  hasAlreadyReviewed(): boolean {
    if (!this.reviewsPage || this.currentUserId == null) return false;
    return this.reviewsPage.content.some((r) => r.reviewerId === this.currentUserId);
  }

  /** Show create-review form only when interview is COMPLETED, user is participant, and has not reviewed yet. */
  canSubmitReview(): boolean {
    return (
      !!this.interview &&
      this.interview.status === 'COMPLETED' &&
      this.isParticipant() &&
      !this.hasAlreadyReviewed() &&
      this.revieweeId() != null
    );
  }

  submitReview() {
    if (!this.interview || this.currentUserId == null || !this.canSubmitReview()) return;
    const revieweeId = this.revieweeId();
    if (revieweeId == null) return;
    this.submittingReview = true;
    const body: ReviewCreateRequest = {
      revieweeId,
      score: this.reviewScore,
      comment: this.reviewComment?.trim() || undefined,
    };
    this.interviewService.createReview(this.interview.id, this.currentUserId, body).subscribe({
      next: () => {
        this.toast.success('Avis enregistré');
        this.submittingReview = false;
        this.reviewComment = '';
        this.loadReviews();
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Échec de l\'envoi de l\'avis');
        this.submittingReview = false;
      },
    });
  }

  downloadIcs() {
    if (!this.interview || this.downloadingIcs) return;
    this.downloadingIcs = true;
    this.interviewService.downloadIcs(this.interview.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `interview-${this.interview!.id}.ics`;
        a.click();
        URL.revokeObjectURL(url);
        this.toast.success('Calendrier téléchargé');
        this.downloadingIcs = false;
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Échec du téléchargement');
        this.downloadingIcs = false;
      },
    });
  }

  statusClass(status: InterviewStatus): string {
    const map: Record<InterviewStatus, string> = {
      PROPOSED: 'bg-amber-100 text-amber-700',
      CONFIRMED: 'bg-sky-100 text-sky-700',
      COMPLETED: 'bg-emerald-100 text-emerald-700',
      CANCELLED: 'bg-red-100 text-red-700',
      NO_SHOW: 'bg-slate-100 text-slate-600',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  modeLabel(mode: MeetingMode): string {
    return mode === 'FACE_TO_FACE' ? 'Face à face' : 'En ligne';
  }

  prevReviewPage() {
    if (this.reviewPageIndex <= 0) return;
    this.reviewPageIndex--;
    this.loadReviews();
  }

  nextReviewPage() {
    if (!this.reviewsPage || this.reviewPageIndex + 1 >= this.reviewsPage.totalPages) return;
    this.reviewPageIndex++;
    this.loadReviews();
  }
}
