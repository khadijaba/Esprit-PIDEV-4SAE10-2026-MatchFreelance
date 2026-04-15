import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Skill, SkillRequest } from '../models/skill.model';

@Injectable({ providedIn: 'root' })
export class SkillService {
  private readonly api = '/api/skills';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Skill[]> {
    return this.http.get<Skill[]>(this.api);
  }

  getById(id: number): Observable<Skill> {
    return this.http.get<Skill>(`${this.api}/${id}`);
  }

  getByFreelancer(freelancerId: number): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  create(skill: SkillRequest): Observable<Skill> {
    return this.http.post<Skill>(this.api, skill);
  }

  update(id: number, skill: SkillRequest): Observable<Skill> {
    return this.http.put<Skill>(`${this.api}/${id}`, skill);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
