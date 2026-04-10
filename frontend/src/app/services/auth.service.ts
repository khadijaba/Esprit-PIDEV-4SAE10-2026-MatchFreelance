import { Injectable, signal, computed } from '@angular/core';

export type UserRole = 'FREELANCER' | 'CLIENT' | 'ADMIN';

export interface User {
    id: number;
    name: string;
    email: string;
    role: UserRole;
    points: number;
    avatarUrl?: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    // Mock initial user - default to Freelancer for the "Front Office" experience
    private currentUserSignal = signal<User | null>({
        id: 1,
        name: 'Alex Freelancer',
        email: 'alex@freelancehub.io',
        role: 'FREELANCER',
        points: 1250,
        avatarUrl: 'https://ui-avatars.com/api/?name=Alex+Freelancer&background=6366f1&color=fff'
    });

    currentUser = computed(() => this.currentUserSignal());
    isAuthenticated = computed(() => !!this.currentUserSignal());

    constructor() { }

    loginAs(role: UserRole) {
        const mockUser: User = {
            id: role === 'CLIENT' ? 2 : (role === 'ADMIN' ? 3 : 1),
            name: role === 'CLIENT' ? 'Sarah Client' : (role === 'ADMIN' ? 'Admin User' : 'Alex Freelancer'),
            email: `${role.toLowerCase()}@freelancehub.io`,
            role: role,
            points: role === 'FREELANCER' ? 1250 : 0, // Only freelancers usually see points
            avatarUrl: `https://ui-avatars.com/api/?name=${role}&background=random&color=fff`
        };
        this.currentUserSignal.set(mockUser);
    }

    logout() {
        this.currentUserSignal.set(null);
    }

    updatePoints(newPoints: number) {
        this.currentUserSignal.update(user => user ? { ...user, points: newPoints } : null);
    }
}
