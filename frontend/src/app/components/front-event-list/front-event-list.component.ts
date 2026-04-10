import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../../services/event.service';
import { ToastService } from '../../services/toast.service';
import { Event } from '../../models/event.model';

@Component({
    selector: 'app-front-event-list',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './front-event-list.component.html',
})
export class FrontEventListComponent implements OnInit {
    events: Event[] = [];
    filteredEvents: Event[] = [];
    loading = true;
    
    // AI Search State
    aiSearchQuery = '';
    aiSearchActive = false;
    aiSearchLoading = false;
    aiSearchResults: Event[] = [];

    private destroyRef = inject(DestroyRef);

    constructor(
        private eventService: EventService,
        private toast: ToastService
    ) { }

    ngOnInit() {
        this.load();
    }

    load() {
        this.loading = true;
        this.eventService.getAll()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => {
                    this.events = data;
                    this.filteredEvents = data;
                    this.loading = false;
                },
                error: () => {
                    this.loading = false;
                    this.toast.error('Failed to load events');
                },
            });
    }

    performAiSearch() {
        if (!this.aiSearchQuery.trim()) {
            this.resetSearch();
            return;
        }

        if (this.aiSearchLoading) return;

        this.aiSearchLoading = true;
        this.aiSearchActive = true;

        this.eventService.searchByNaturalLanguage(this.aiSearchQuery)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => {
                    this.aiSearchResults = data;
                    this.filteredEvents = data;
                    this.aiSearchLoading = false;
                    if (data.length === 0) {
                        this.toast.info('No matching events found for your query.');
                    }
                },
                error: (err) => {
                    console.error('Error performing AI search:', err);
                    this.aiSearchLoading = false;
                    this.toast.error('Failed to search. Using regular search instead.');
                    this.resetSearch();
                },
            });
    }

    resetSearch() {
        this.aiSearchActive = false;
        this.aiSearchQuery = '';
        this.aiSearchResults = [];
        this.filteredEvents = this.events;
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

    typeLabel(type: string): string {
        return type.replace(/_/g, ' ');
    }
}
