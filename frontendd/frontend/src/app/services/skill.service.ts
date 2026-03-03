import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Skill, SkillRequest, SkillCategory, FreelancerProfile } from '../models/skill.model';

@Injectable({ providedIn: 'root' })
export class SkillService {
  private readonly api = '/api/skills';
  private readonly profilesApi = '/api/profiles';

  constructor(private http: HttpClient) {}

  // Skills
  getAll(): Observable<Skill[]> {
    return this.http.get<Skill[]>(this.api);
  }

  getById(id: number): Observable<Skill> {
    return this.http.get<Skill>(`${this.api}/${id}`);
  }

  getByFreelancerId(freelancerId: number): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  getByCategory(category: SkillCategory): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.api}/category/${category}`);
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

  // CV Management
  uploadCV(freelancerId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`/api/cv/upload/${freelancerId}`, formData);
  }

  getCV(freelancerId: number): Observable<any> {
    return this.http.get<any>(`/api/cv/freelancer/${freelancerId}`);
  }

  deleteCV(freelancerId: number): Observable<void> {
    return this.http.delete<void>(`/api/cv/freelancer/${freelancerId}`);
  }

  // Portfolio Management (multiple per freelancer) – JSON pour passer la gateway
  addPortfolio(freelancerId: number, portfolioUrl: string, portfolioDescription?: string): Observable<any> {
    const body = { portfolioUrl, portfolioDescription: portfolioDescription || '' };
    return this.http.post<any>(`/api/portfolio/${freelancerId}`, body, {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
    });
  }

  getPortfolios(freelancerId: number): Observable<any[]> {
    return this.http.get<any[]>(`/api/portfolio/freelancer/${freelancerId}`);
  }

  updatePortfolio(id: number, portfolioUrl: string, portfolioDescription?: string): Observable<any> {
    const body = { portfolioUrl, portfolioDescription: portfolioDescription || '' };
    return this.http.put<any>(`/api/portfolio/${id}`, body, {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
    });
  }

  deletePortfolio(id: number): Observable<void> {
    return this.http.delete<void>(`/api/portfolio/${id}`);
  }

  // Admin Supervision
  getAllCVs(): Observable<any[]> {
    return this.http.get<any[]>(`/api/cv/all`);
  }

  getAllPortfolios(): Observable<any[]> {
    return this.http.get<any[]>(`/api/portfolio/all`);
  }

  // Professional summary (Bio) – no file upload
  getBio(freelancerId: number): Observable<{ id?: number; freelancerId: number; bio: string }> {
    return this.http.get<{ id?: number; freelancerId: number; bio: string }>(`/api/bio/freelancer/${freelancerId}`);
  }
  saveBio(freelancerId: number, bio: string): Observable<{ id?: number; freelancerId: number; bio: string }> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.put<{ id?: number; freelancerId: number; bio: string }>(
      `/api/bio/freelancer/${freelancerId}`,
      { bio },
      { headers }
    );
  }
  deleteBio(freelancerId: number): Observable<void> {
    return this.http.delete<void>(`/api/bio/freelancer/${freelancerId}`);
  }
}
