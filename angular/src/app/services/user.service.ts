import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable, of } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly api = '/api/users';
  private cache = new Map<number, Observable<User>>();

  constructor(private http: HttpClient) {}

  getById(id: number): Observable<User> {
    if (!this.cache.has(id)) {
      this.cache.set(
        id,
        this.http.get<User>(`${this.api}/${id}`).pipe(shareReplay(1))
      );
    }
    return this.cache.get(id)!;
  }

  getDisplayName(id: number): Observable<string> {
    return this.getById(id).pipe(
      map((u) => u.fullName?.trim() || u.email || `#${id}`)
    );
  }

  /** Load display names for multiple user ids. Returns a map id -> fullName or email. */
  getDisplayNamesMap(ids: number[]): Observable<Record<number, string>> {
    const unique = [...new Set(ids)].filter((id) => id != null);
    if (unique.length === 0) return of({});
    return forkJoin(
      unique.map((id) =>
        this.getById(id).pipe(
          map((u) => ({ id, name: u.fullName?.trim() || u.email || `#${id}` }))
        )
      )
    ).pipe(
      map((pairs) => {
        const record: Record<number, string> = {};
        pairs.forEach(({ id, name }) => (record[id] = name));
        return record;
      })
    );
  }
}
