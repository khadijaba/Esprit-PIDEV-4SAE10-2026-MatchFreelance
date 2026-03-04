import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { PageResponse, User, UserFilterRequest, UserStatsResponse } from '../models/user.model';

@Injectable({
    providedIn: 'root'
})
export class UserService {
    private apiUrl = `${environment.apiUrl}/admin/v2`;
    private legacyUrl = `${environment.apiUrl}/admin`;

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
}
