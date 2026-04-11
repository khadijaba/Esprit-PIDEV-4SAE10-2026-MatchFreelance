import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Sponsor, CreateSponsorRequest, SponsorType } from '../models/sponsor.model';

@Injectable({ providedIn: 'root' })
export class SponsorService {
  private readonly api = '/api/sponsors';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Sponsor[]> {
    return this.http.get<Sponsor[]>(this.api);
  }

  getById(id: number): Observable<Sponsor> {
    return this.http.get<Sponsor>(`${this.api}/${id}`);
  }

  getByType(type: SponsorType): Observable<Sponsor[]> {
    return this.http.get<Sponsor[]>(`${this.api}/type/${type}`);
  }

  getActive(): Observable<Sponsor[]> {
    return this.http.get<Sponsor[]>(`${this.api}/active`);
  }

  getByEvent(eventId: number): Observable<Sponsor[]> {
    return this.http.get<Sponsor[]>(`${this.api}/event/${eventId}`);
  }

  search(name: string): Observable<Sponsor[]> {
    return this.http.get<Sponsor[]>(`${this.api}/search`, { params: { name } });
  }

  create(req: CreateSponsorRequest): Observable<Sponsor> {
    return this.http.post<Sponsor>(this.api, req);
  }

  update(id: number, req: CreateSponsorRequest): Observable<Sponsor> {
    return this.http.put<Sponsor>(`${this.api}/${id}`, req);
  }

  toggleActive(id: number): Observable<Sponsor> {
    return this.http.patch<Sponsor>(`${this.api}/${id}/toggle-active`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
