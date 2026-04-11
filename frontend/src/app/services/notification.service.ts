import { Injectable, signal, computed } from '@angular/core';

export type NotificationType = 'event_reminder' | 'event_starting' | 'reward_earned' | 'registration_confirmed' | 'info';

export interface Notification {
    id: number;
    title: string;
    message: string;
    type: NotificationType;
    timestamp: Date;
    read: boolean;
    link?: string;
    icon?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
    private notificationsSignal = signal<Notification[]>([]);
    private nextId = 0;

    notifications = computed(() => this.notificationsSignal());
    unreadCount = computed(() => this.notificationsSignal().filter(n => !n.read).length);

    private reminderTimeouts: Map<number, ReturnType<typeof setTimeout>> = new Map();

    add(title: string, message: string, type: NotificationType, link?: string) {
        const iconMap: Record<NotificationType, string> = {
            event_reminder: '⏰',
            event_starting: '🚀',
            reward_earned: '🏆',
            registration_confirmed: '✅',
            info: 'ℹ️',
        };

        const notification: Notification = {
            id: ++this.nextId,
            title,
            message,
            type,
            timestamp: new Date(),
            read: false,
            link,
            icon: iconMap[type],
        };

        this.notificationsSignal.update(list => [notification, ...list]);
    }

    markAsRead(id: number) {
        this.notificationsSignal.update(list =>
            list.map(n => n.id === id ? { ...n, read: true } : n)
        );
    }

    markAllAsRead() {
        this.notificationsSignal.update(list =>
            list.map(n => ({ ...n, read: true }))
        );
    }

    remove(id: number) {
        this.notificationsSignal.update(list => list.filter(n => n.id !== id));
    }

    clearAll() {
        this.notificationsSignal.set([]);
    }

    /** Schedule a reminder notification for an event. */
    scheduleEventReminder(eventId: number, eventTitle: string, startDate: string, minutesBefore = 30) {
        const start = new Date(startDate).getTime();
        const reminderTime = start - minutesBefore * 60 * 1000;
        const now = Date.now();

        if (reminderTime > now) {
            const timeout = setTimeout(() => {
                this.add(
                    'Event Starting Soon',
                    `"${eventTitle}" starts in ${minutesBefore} minutes!`,
                    'event_reminder',
                    `/events/${eventId}`
                );
                this.reminderTimeouts.delete(eventId);
            }, reminderTime - now);

            this.reminderTimeouts.set(eventId, timeout);
        }

        // Also check if event is happening now
        if (start <= now && start + 60000 > now) {
            this.add(
                'Event Starting Now!',
                `"${eventTitle}" is starting right now!`,
                'event_starting',
                `/events/${eventId}`
            );
        }
    }

    /** Notify that the user registered for an event. */
    notifyRegistration(eventTitle: string, eventId: number) {
        this.add(
            'Registration Confirmed',
            `You've successfully registered for "${eventTitle}".`,
            'registration_confirmed',
            `/events/${eventId}`
        );
    }

    /** Notify that a reward was earned. */
    notifyRewardEarned(rewardValue: string, eventTitle: string) {
        this.add(
            'Reward Earned!',
            `You earned "${rewardValue}" from "${eventTitle}".`,
            'reward_earned'
        );
    }

    getTimeAgo(date: Date): string {
        const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
        if (seconds < 60) return 'Just now';
        if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
        if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
        return `${Math.floor(seconds / 86400)}d ago`;
    }
}
