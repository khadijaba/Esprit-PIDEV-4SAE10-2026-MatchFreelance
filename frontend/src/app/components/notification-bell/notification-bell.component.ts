import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NotificationService } from '../../services/notification.service';

@Component({
    selector: 'app-notification-bell',
    standalone: true,
    imports: [CommonModule, RouterLink],
    template: `
        <div class="relative">
            <!-- Bell Button -->
            <button (click)="togglePanel()" class="relative rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                @if (notificationService.unreadCount() > 0) {
                <span class="absolute -top-0.5 -right-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs font-bold text-white ring-2 ring-white">
                    {{ notificationService.unreadCount() > 9 ? '9+' : notificationService.unreadCount() }}
                </span>
                }
            </button>

            <!-- Dropdown Panel -->
            @if (isOpen) {
            <div class="absolute right-0 top-12 z-50 w-80 sm:w-96 rounded-xl border border-gray-200 bg-white shadow-2xl">
                <!-- Header -->
                <div class="flex items-center justify-between border-b border-gray-100 px-4 py-3">
                    <h3 class="text-sm font-bold text-gray-900">Notifications</h3>
                    <div class="flex items-center gap-2">
                        @if (notificationService.unreadCount() > 0) {
                        <button (click)="notificationService.markAllAsRead()"
                            class="text-xs font-medium text-indigo-600 hover:text-indigo-700 transition">
                            Mark all read
                        </button>
                        }
                        @if (notificationService.notifications().length > 0) {
                        <button (click)="notificationService.clearAll()"
                            class="text-xs font-medium text-gray-400 hover:text-red-500 transition">
                            Clear all
                        </button>
                        }
                    </div>
                </div>

                <!-- Notifications List -->
                <div class="max-h-80 overflow-y-auto">
                    @if (notificationService.notifications().length === 0) {
                    <div class="px-4 py-8 text-center">
                        <span class="text-3xl">🔔</span>
                        <p class="mt-2 text-sm text-gray-500">No notifications yet</p>
                    </div>
                    }

                    @for (n of notificationService.notifications(); track n.id) {
                    <div class="flex items-start gap-3 border-b border-gray-50 px-4 py-3 transition hover:bg-gray-50"
                        [class.bg-indigo-50/40]="!n.read">
                        <span class="mt-0.5 text-lg">{{ n.icon }}</span>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2">
                                <p class="text-sm font-semibold text-gray-900 truncate">{{ n.title }}</p>
                                @if (!n.read) {
                                <span class="h-2 w-2 rounded-full bg-indigo-500 shrink-0"></span>
                                }
                            </div>
                            <p class="mt-0.5 text-xs text-gray-600 line-clamp-2">{{ n.message }}</p>
                            <div class="mt-1 flex items-center gap-2">
                                <span class="text-xs text-gray-400">{{ notificationService.getTimeAgo(n.timestamp) }}</span>
                                @if (n.link) {
                                <a [routerLink]="n.link" (click)="onClickNotification(n.id)"
                                    class="text-xs font-medium text-indigo-600 hover:text-indigo-700">
                                    View →
                                </a>
                                }
                            </div>
                        </div>
                        <button (click)="notificationService.remove(n.id)"
                            class="shrink-0 rounded p-1 text-gray-300 transition hover:bg-gray-100 hover:text-gray-500">
                            <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>
                    }
                </div>
            </div>

            <!-- Click-outside overlay -->
            <div (click)="isOpen = false" class="fixed inset-0 z-40" style="background: transparent;"></div>
            }
        </div>
    `,
})
export class NotificationBellComponent {
    isOpen = false;

    constructor(public notificationService: NotificationService) {}

    togglePanel() {
        this.isOpen = !this.isOpen;
    }

    onClickNotification(id: number) {
        this.notificationService.markAsRead(id);
        this.isOpen = false;
    }
}
