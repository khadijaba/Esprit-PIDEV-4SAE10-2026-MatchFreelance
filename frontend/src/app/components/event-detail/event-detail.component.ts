import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../../services/event.service';
import { RewardService } from '../../services/reward.service';
import { ToastService } from '../../services/toast.service';
import { Event, Participation } from '../../models/event.model';
import { Reward } from '../../models/reward.model';

@Component({
    selector: 'app-event-detail',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './event-detail.component.html',
})
export class EventDetailComponent implements OnInit {
    event: Event | null = null;
    participants: Participation[] = [];
    leaderboard: Participation[] = [];
    rewards: Reward[] = [];
    loading = true;
    activeTab: 'details' | 'ai' | 'participants' | 'leaderboard' | 'rewards' = 'details';

    // AI Features State
    summary: string = '';
    summaryLoading = false;
    summaryError = '';
    
    qaQuestion: string = '';
    qaAnswer: string = '';
    qaLoading = false;
    qaError = '';
    lastQuestion: string = '';

    private destroyRef = inject(DestroyRef);

    constructor(
        private route: ActivatedRoute,
        private eventService: EventService,
        private rewardService: RewardService,
        private toast: ToastService
    ) { }

    ngOnInit() {
        const id = Number(this.route.snapshot.paramMap.get('id'));
        this.loadEvent(id);
        this.loadParticipants(id);
        this.loadLeaderboard(id);
        this.loadRewards(id);
    }

    loadEvent(id: number) {
        this.eventService.getById(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => {
                    this.event = data;
                    this.loading = false;
                },
                error: () => {
                    this.loading = false;
                    this.toast.error('Failed to load event');
                },
            });
    }

    loadParticipants(id: number) {
        this.eventService.getParticipants(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => (this.participants = data),
            });
    }

    loadLeaderboard(id: number) {
        this.eventService.getLeaderboard(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => (this.leaderboard = data),
            });
    }

    loadRewards(id: number) {
        this.rewardService.getEventRewards(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => (this.rewards = data),
            });
    }

    // AI Methods
    generateSummary() {
        if (!this.event?.id || this.summaryLoading) return;
        
        this.summaryLoading = true;
        this.summaryError = '';
        
        this.eventService.generateSummary(this.event.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => {
                    this.summary = data;
                    this.summaryLoading = false;
                },
                error: (err) => {
                    console.error('Error generating summary:', err);
                    this.summaryError = 'Failed to generate summary. Please try again.';
                    this.summaryLoading = false;
                },
            });
    }

    regenerateSummary() {
        this.summary = '';
        this.summaryError = '';
        this.generateSummary();
    }

    submitQuestion() {
        if (!this.event?.id || !this.qaQuestion.trim() || this.qaLoading) return;
        
        this.qaLoading = true;
        this.qaError = '';
        this.lastQuestion = this.qaQuestion;
        
        this.eventService.askQuestion(this.event.id, this.qaQuestion)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => {
                    this.qaAnswer = data;
                    this.qaQuestion = '';
                    this.qaLoading = false;
                },
                error: (err) => {
                    console.error('Error answering question:', err);
                    this.qaError = 'Failed to answer question. Please try again.';
                    this.qaLoading = false;
                },
            });
    }

    statusClass(status: string): string {
        const map: Record<string, string> = {
            UPCOMING: 'bg-indigo-100 text-indigo-700',
            ONGOING: 'bg-emerald-100 text-emerald-700',
            COMPLETED: 'bg-blue-100 text-blue-700',
            CANCELLED: 'bg-red-100 text-red-700',
            CLOSED: 'bg-gray-100 text-gray-700',
        };
        return map[status] ?? 'bg-gray-100 text-gray-700';
    }
}
