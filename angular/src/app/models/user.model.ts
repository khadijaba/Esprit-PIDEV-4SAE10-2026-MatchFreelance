export enum Role {
    ADMIN = 'ADMIN',
    PROJECT_OWNER = 'PROJECT_OWNER',
    FREELANCER = 'FREELANCER',
    USER = 'USER'
}

export interface User {
    id: number;
    firstName: string;
    lastName: string;
    address: string;
    email: string;
    birthDate: string;
    role: Role;
    enabled: boolean;
}

export interface UserStatsResponse {
    totalUsers: number;
    activeUsers: number;
    inactiveUsers: number;
    adminCount: number;
    projectOwnerCount: number;
    freelancerCount: number;
    newUsersThisWeek: number;
}

export interface UserFilterRequest {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
    searchTerm?: string;
    role?: Role;
    enabled?: boolean;
}

export interface PageResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
}
