import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Statistiques renvoyées par GET /api/admin/v2/statistics (microservice User PIDEV). */
export interface UserStats {
  totalUsers: number;
  activeFreelancers: number;
  activeProjectOwners: number;
  activeAccounts: number;
  inactiveAccounts: number;
  totalAdmins: number;
}

/** Ligne utilisateur renvoyée par la recherche admin (entité sérialisée côté Java). */
export interface PidevUserRow {
  id?: number;
  email?: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  enabled?: boolean;
  profilePictureUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class PidevUserService {
  private readonly adminV2 = '/api/admin/v2';

  constructor(private http: HttpClient) {}

  getStatistics(): Observable<UserStats> {
    return this.http.get<UserStats>(`${this.adminV2}/statistics`);
  }

  searchSimple(term: string): Observable<PidevUserRow[]> {
    return this.http.get<PidevUserRow[]>(`${this.adminV2}/search/simple`, {
      params: { term },
    });
  }
}
