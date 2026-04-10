import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event, EventRequest, EventStatus, EventType, Participation } from '../models/event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
    private readonly api = '/api/events';

    constructor(private http: HttpClient) { }

    getAll(status?: EventStatus, type?: EventType): Observable<Event[]> {
        let params = new HttpParams();
        if (status) params = params.set('status', status);
        if (type) params = params.set('type', type);
        return this.http.get<Event[]>(this.api, { params });
    }

    getById(id: number): Observable<Event> {
        return this.http.get<Event>(`${this.api}/${id}`);
    }

    create(event: EventRequest): Observable<Event> {
        return this.http.post<Event>(this.api, event);
    }

    update(id: number, event: EventRequest): Observable<Event> {
        return this.http.put<Event>(`${this.api}/${id}`, event);
    }

    delete(id: number): Observable<void> {
        return this.http.delete<void>(`${this.api}/${id}`);
    }

    register(eventId: number, userId: number): Observable<Participation> {
        return this.http.post<Participation>(`${this.api}/${eventId}/register?userId=${userId}`, {});
    }

    getParticipants(eventId: number): Observable<Participation[]> {
        return this.http.get<Participation[]>(`${this.api}/${eventId}/participants`);
    }

    getLeaderboard(eventId: number): Observable<Participation[]> {
        return this.http.get<Participation[]>(`${this.api}/${eventId}/leaderboard`);
    }

    // ── AI Features ──

    generateSummary(id: number): Observable<string> {
        return this.http.get(`${this.api}/${id}/summary`, { responseType: 'text' });
    }

    askQuestion(id: number, question: string): Observable<string> {
        return this.http.get(`${this.api}/${id}/qa`, {
            params: { question },
            responseType: 'text'
        });
    }

    searchByNaturalLanguage(query: string): Observable<Event[]> {
        return this.http.get<Event[]>(`${this.api}/search`, {
            params: { query }
        });
    }
}

