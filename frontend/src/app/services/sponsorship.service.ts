import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Sponsorship, CreateSponsorshipRequest, SponsorshipStatus } from '../models/sponsor.model';

@Injectable({ providedIn: 'root' })
export class SponsorshipService {
  private readonly api = '/api/sponsorships';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Sponsorship[]> {
    return this.http.get<Sponsorship[]>(this.api);
  }

  getById(id: number): Observable<Sponsorship> {
    return this.http.get<Sponsorship>(`${this.api}/${id}`);
  }

  getBySponsor(sponsorId: number): Observable<Sponsorship[]> {
    return this.http.get<Sponsorship[]>(`${this.api}/sponsor/${sponsorId}`);
  }

  getByEvent(eventId: number): Observable<Sponsorship[]> {
    return this.http.get<Sponsorship[]>(`${this.api}/event/${eventId}`);
  }

  getTotalFunding(eventId: number): Observable<number> {
    return this.http.get<number>(`${this.api}/event/${eventId}/total`);
  }

  getByStatus(status: SponsorshipStatus): Observable<Sponsorship[]> {
    return this.http.get<Sponsorship[]>(`${this.api}/status/${status}`);
  }

  create(req: CreateSponsorshipRequest): Observable<Sponsorship> {
    return this.http.post<Sponsorship>(this.api, req);
  }

  update(id: number, req: CreateSponsorshipRequest): Observable<Sponsorship> {
    return this.http.put<Sponsorship>(`${this.api}/${id}`, req);
  }

  confirm(id: number): Observable<Sponsorship> {
    return this.http.patch<Sponsorship>(`${this.api}/${id}/confirm`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
