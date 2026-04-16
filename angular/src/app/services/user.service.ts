import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { PageResponse, User, UserFilterRequest, UserStatsResponse } from '../models/user.model';

@Injectable({
    providedIn: 'root'
})
export class UserService {
    // User microservice endpoints (port 9090)
    private userMicroserviceUrl = 'http://localhost:9090/api';
    private apiUrl = `${this.userMicroserviceUrl}/admin/v2`;
    private legacyUrl = `${this.userMicroserviceUrl}/admin`;
    private baseApiUrl = `${this.userMicroserviceUrl}/users`;

    constructor(private http: HttpClient) { }

    // Search users with filters and pagination
    searchUsers(filter: UserFilterRequest): Observable<PageResponse<User>> {
        return this.http.post<PageResponse<User>>(`${this.apiUrl}/search`, filter);
    }

    // Get User Statistics
    getUserStatistics(): Observable<UserStatsResponse> {
        return this.http.get<UserStatsResponse>(`${this.apiUrl}/statistics`);
    }

    // Delete User
    deleteUser(id: number): Observable<any> {
        return this.http.delete(`${this.apiUrl}/users/${id}`, { responseType: 'text' });
    }

    // Simple search (by name or email)
    simpleSearch(term: string): Observable<User[]> {
        const params = new HttpParams().set('term', term);
        return this.http.get<User[]>(`${this.apiUrl}/search/simple`, { params });
    }

    // Get all users
    getAllUsers(): Observable<User[]> {
        return this.http.get<User[]>(`${this.legacyUrl}/users`);
    }

    // Update user status (enable/disable)
    updateUserStatus(userId: number, enabled: boolean): Observable<any> {
        return this.http.patch(`${this.legacyUrl}/users/${userId}/status`, { enabled }, { responseType: 'text' });
    }

    // Get current logged in user
    getCurrentUser(): Observable<User> {
        return this.http.get<User>(`${this.baseApiUrl}/me`);
    }

    // Upload new profile avatar
    uploadAvatar(file: File): Observable<{ url: string }> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<{ url: string }>(`${this.baseApiUrl}/me/avatar`, formData);
    }

    // Update profile data
    updateProfile(user: User): Observable<User> {
        return this.http.put<User>(`${this.baseApiUrl}/me`, user);
    }
}
