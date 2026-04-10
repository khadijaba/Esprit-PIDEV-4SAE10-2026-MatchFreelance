import { Component, OnInit, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../../services/event.service';
import { ToastService } from '../../services/toast.service';
import { Event, EventStatus, EventType } from '../../models/event.model';

@Component({
    selector: 'app-event-list',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './event-list.component.html',
})
export class EventListComponent implements OnInit {
    events: Event[] = [];
    filteredEvents: Event[] = [];
    searchTerm = '';
    aiSearchQuery = '';
    statusFilter: EventStatus | '' = '';
    typeFilter: EventType | '' = '';
    loading = true;
    
    // AI Search State
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
                    this.resetSearch();
                    this.applyFilters();
                    this.loading = false;
                },
                error: () => {
                    this.loading = false;
                    this.toast.error('Failed to load events');
                },
            });
    }

    applyFilters() {
        let result = this.events;
        if (this.statusFilter) {
            result = result.filter((e) => e.status === this.statusFilter);
        }
        if (this.typeFilter) {
            result = result.filter((e) => e.type === this.typeFilter);
        }
        if (this.searchTerm.trim()) {
            const q = this.searchTerm.toLowerCase();
            result = result.filter((e) => e.title.toLowerCase().includes(q));
        }
        this.filteredEvents = result;
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
                    this.applyFilters();
                },
            });
    }

    resetSearch() {
        this.aiSearchActive = false;
        this.aiSearchQuery = '';
        this.aiSearchResults = [];
        this.applyFilters();
    }

    onDelete(id: number) {
        if (!confirm('Are you sure you want to delete this event?')) return;
        this.eventService.delete(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => {
                    this.toast.success('Event deleted');
                    this.load();
                },
                error: () => this.toast.error('Failed to delete event'),
            });
    }

    statusClass(status: EventStatus): string {
        const map: Record<EventStatus, string> = {
            UPCOMING: 'bg-indigo-100 text-indigo-700',
            ONGOING: 'bg-emerald-100 text-emerald-700',
            COMPLETED: 'bg-blue-100 text-blue-700',
            CANCELLED: 'bg-red-100 text-red-700',
            CLOSED: 'bg-gray-100 text-gray-700',
        };
        return map[status] ?? 'bg-gray-100 text-gray-700';
    }

    typeLabel(type: EventType): string {
        return type.replace(/_/g, ' ');
    }

    formatStatus(status: EventStatus): string {
        return status.replace(/_/g, ' ');
    }
}
