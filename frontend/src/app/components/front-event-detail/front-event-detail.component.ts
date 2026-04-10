import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../../services/event.service';
import { ToastService } from '../../services/toast.service';
import { Event, Participation } from '../../models/event.model';

@Component({
    selector: 'app-front-event-detail',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './front-event-detail.component.html',
})
export class FrontEventDetailComponent implements OnInit {
    event: Event | null = null;
    leaderboard: Participation[] = [];
    participantCount = 0;
    loading = true;
    registering = false;
    activeTab: 'details' | 'ai' = 'details';

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
        private toast: ToastService
    ) { }

    ngOnInit() {
        const id = Number(this.route.snapshot.paramMap.get('id'));
        
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
        
        this.eventService.getParticipants(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => (this.participantCount = data.length),
            });
        
        this.eventService.getLeaderboard(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => (this.leaderboard = data),
            });
    }

    register() {
        if (!this.event) return;
        this.registering = true;
        // Using userId=1 as placeholder (no auth system in place)
        this.eventService.register(this.event.id, 1).subscribe({
            next: () => {
                this.toast.success('Successfully registered!');
                this.participantCount++;
                this.registering = false;
            },
            error: (err) => {
                const msg = typeof err.error === 'string' ? err.error : 'Registration failed';
                this.toast.error(msg);
                this.registering = false;
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

    typeIcon(type: string): string {
        const map: Record<string, string> = {
            HACKATHON: '🏆',
            CHALLENGE: '⚡',
            WEBINAR: '🎥',
            MILESTONE: '🎯',
            COMMUNITY: '🤝',
            SKILL_RACE: '🏃',
            FORMATION_WEBINAR: '📚',
            OTHER: '📌',
        };
        return map[type] ?? '📌';
    }

    canRegister(): boolean {
        return !!this.event && (this.event.status === 'UPCOMING' || this.event.status === 'ONGOING');
    }

    plannedRewards(): any[] {
        return this.event?.plannedRewards || [];
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
}
