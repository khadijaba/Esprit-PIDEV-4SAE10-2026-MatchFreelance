import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../../services/event.service';
import { RewardService } from '../../services/reward.service';
import { Event, Participation } from '../../models/event.model';
import { Reward, RewardType, RewardRequest } from '../../models/reward.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-participation-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './participation-list.component.html',
})
export class ParticipationListComponent implements OnInit {
  eventId: number | null = null;
  event: Event | null = null;
  participants: Participation[] = [];
  loading = false;
  showRewardModal = false;
  selectedParticipant: Participation | null = null;
  rewardForm!: FormGroup;

  rewardTypes: RewardType[] = ['BADGE', 'POINTS', 'CERTIFICATE', 'PRIORITY_BOOST', 'DISCOUNT', 'FEATURED_PROFILE', 'TOKEN', 'OTHER'];

  private destroyRef = inject(DestroyRef);

  constructor(
    private eventService: EventService,
    private rewardService: RewardService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.initializeForm();
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.eventId = Number(id);
      this.loadEventAndParticipants();
    }
  }

  initializeForm() {
    this.rewardForm = this.fb.group({
      type: ['BADGE', Validators.required],
      value: ['', Validators.required],
      description: ['', Validators.required],
      visibleOnProfile: [true],
    });
  }

  loadEventAndParticipants() {
    if (!this.eventId) return;
    this.loading = true;

    this.eventService.getById(this.eventId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (event) => {
          this.event = event;
        },
        error: (err) => {
          this.toast.error('Failed to load event');
          this.loading = false;
        },
      });

    this.eventService.getParticipants(this.eventId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (participants) => {
          this.participants = participants;
          this.loading = false;
        },
        error: (err) => {
          this.toast.error('Failed to load participants');
          this.loading = false;
        },
      });
  }

  openRewardModal(participant: Participation) {
    this.selectedParticipant = participant;
    this.rewardForm.reset({ type: 'BADGE', visibleOnProfile: true });
    this.showRewardModal = true;
  }

  closeRewardModal() {
    this.showRewardModal = false;
    this.selectedParticipant = null;
    this.rewardForm.reset();
  }

  awardReward() {
    if (!this.rewardForm.valid || !this.selectedParticipant || !this.eventId) return;

    const reward: RewardRequest = {
      ...this.rewardForm.value,
      recipientId: this.selectedParticipant.userId,
      eventId: this.eventId,
    };

    this.rewardService.awardReward(this.eventId, reward)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Reward awarded successfully');
          this.closeRewardModal();
        },
        error: () => {
          this.toast.error('Failed to award reward');
        },
      });
  }

  getRewardTypeBadgeClass(type: string): string {
    const colors: Record<string, string> = {
      BADGE: 'bg-purple-100 text-purple-700',
      POINTS: 'bg-blue-100 text-blue-700',
      CERTIFICATE: 'bg-green-100 text-green-700',
      PRIORITY_BOOST: 'bg-orange-100 text-orange-700',
      DISCOUNT: 'bg-pink-100 text-pink-700',
      FEATURED_PROFILE: 'bg-indigo-100 text-indigo-700',
      TOKEN: 'bg-yellow-100 text-yellow-700',
      OTHER: 'bg-gray-100 text-gray-700',
    };
    return colors[type] || 'bg-gray-100 text-gray-700';
  }

  getStatusBadge(status: string | null): string {
    const map: Record<string, string> = {
      REGISTERED: 'bg-blue-100 text-blue-700',
      ACTIVE: 'bg-green-100 text-green-700',
      COMPLETED: 'bg-emerald-100 text-emerald-700',
      DISQUALIFIED: 'bg-red-100 text-red-700',
    };
    return map[status || 'REGISTERED'] || 'bg-gray-100 text-gray-700';
  }
}
