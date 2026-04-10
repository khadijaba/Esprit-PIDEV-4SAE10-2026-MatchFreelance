import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../../services/event.service';
import { ToastService } from '../../services/toast.service';
import { Event, EventType } from '../../models/event.model';

interface CalendarDay {
    date: Date;
    isCurrentMonth: boolean;
    isToday: boolean;
    events: Event[];
}

@Component({
    selector: 'app-event-calendar',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './event-calendar.component.html',
})
export class EventCalendarComponent implements OnInit {
    currentDate = new Date();
    currentMonth = new Date();
    weeks: CalendarDay[][] = [];
    events: Event[] = [];
    loading = true;
    selectedDay: CalendarDay | null = null;

    weekDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

    private destroyRef = inject(DestroyRef);

    constructor(
        private eventService: EventService,
        private toast: ToastService
    ) {}

    ngOnInit() {
        this.loadEvents();
    }

    loadEvents() {
        this.loading = true;
        this.eventService.getAll()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (data) => {
                    this.events = data;
                    this.buildCalendar();
                    this.loading = false;
                },
                error: () => {
                    this.loading = false;
                    this.toast.error('Failed to load events');
                },
            });
    }

    buildCalendar() {
        const year = this.currentMonth.getFullYear();
        const month = this.currentMonth.getMonth();

        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);

        const startDate = new Date(firstDay);
        startDate.setDate(startDate.getDate() - startDate.getDay());

        const endDate = new Date(lastDay);
        endDate.setDate(endDate.getDate() + (6 - endDate.getDay()));

        this.weeks = [];
        let currentWeek: CalendarDay[] = [];
        const cursor = new Date(startDate);

        while (cursor <= endDate) {
            const day: CalendarDay = {
                date: new Date(cursor),
                isCurrentMonth: cursor.getMonth() === month,
                isToday: this.isSameDay(cursor, this.currentDate),
                events: this.getEventsForDay(cursor),
            };
            currentWeek.push(day);

            if (currentWeek.length === 7) {
                this.weeks.push(currentWeek);
                currentWeek = [];
            }
            cursor.setDate(cursor.getDate() + 1);
        }
    }

    getEventsForDay(date: Date): Event[] {
        return this.events.filter(event => {
            const start = new Date(event.startDate);
            const end = new Date(event.endDate);
            const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
            const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59);
            return start <= dayEnd && end >= dayStart;
        });
    }

    isSameDay(a: Date, b: Date): boolean {
        return a.getFullYear() === b.getFullYear() &&
            a.getMonth() === b.getMonth() &&
            a.getDate() === b.getDate();
    }

    prevMonth() {
        this.currentMonth = new Date(
            this.currentMonth.getFullYear(),
            this.currentMonth.getMonth() - 1,
            1
        );
        this.buildCalendar();
        this.selectedDay = null;
    }

    nextMonth() {
        this.currentMonth = new Date(
            this.currentMonth.getFullYear(),
            this.currentMonth.getMonth() + 1,
            1
        );
        this.buildCalendar();
        this.selectedDay = null;
    }

    goToday() {
        this.currentMonth = new Date();
        this.buildCalendar();
        this.selectedDay = null;
    }

    selectDay(day: CalendarDay) {
        this.selectedDay = this.selectedDay === day ? null : day;
    }

    get monthLabel(): string {
        return this.currentMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    }

    typeColor(type: EventType): string {
        const map: Record<string, string> = {
            HACKATHON: 'bg-purple-500',
            CHALLENGE: 'bg-amber-500',
            WEBINAR: 'bg-blue-500',
            MILESTONE: 'bg-emerald-500',
            COMMUNITY: 'bg-pink-500',
            SKILL_RACE: 'bg-orange-500',
            FORMATION_WEBINAR: 'bg-teal-500',
            OTHER: 'bg-gray-500',
        };
        return map[type] ?? 'bg-gray-500';
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
