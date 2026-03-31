import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserRequest } from '../models/user.model';

/**
 * Service pour le microservice User.
 * Les appels passent par le proxy (/api) puis la Gateway (8082) qui route vers le service USER.
 * Adapter les méthodes selon les vrais endpoints du microservice User (collègue).
 */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly api = '/api/users';

  constructor(private http: HttpClient) {}

  /** Liste tous les utilisateurs. Optionnel : ?role=FREELANCER|CLIENT|ADMIN */
  getAll(role?: string): Observable<User[]> {
    if (role) {
      return this.http.get<User[]>(this.api, { params: { role } });
    }
    return this.http.get<User[]>(this.api);
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${this.api}/${id}`);
  }

  create(user: UserRequest): Observable<User> {
    return this.http.post<User>(this.api, user);
  }

  update(id: number, user: UserRequest): Observable<User> {
    return this.http.put<User>(`${this.api}/${id}`, user);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
