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

  /** Supervision : bloquer ou débloquer une compétence (admin). Utilise PUT avec les données complètes + blocked. */
  setBlocked(skill: Skill, blocked: boolean): Observable<Skill> {
    const body: SkillRequest & { blocked?: boolean } = {
      name: skill.name,
      category: skill.category,
      freelancerId: skill.freelancerId,
      level: skill.level,
      yearsOfExperience: skill.yearsOfExperience,
      blocked,
    };
    return this.http.put<Skill>(`${this.api}/${skill.id}`, body);
  }

  private static readonly STORAGE_KEY_BLOCKED = 'matchfreelance_skill_blocked';

  /** Fallback si le backend ne gère pas encore "blocked" : état bloqué stocké en local (par id de skill). */
  getBlockedSkillIds(): number[] {
    try {
      const raw = localStorage.getItem(SkillService.STORAGE_KEY_BLOCKED);
      if (!raw) return [];
      const arr = JSON.parse(raw);
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  }

  setBlockedSkillIdLocal(id: number, blocked: boolean): void {
    const ids = new Set(this.getBlockedSkillIds());
    if (blocked) ids.add(id); else ids.delete(id);
    localStorage.setItem(SkillService.STORAGE_KEY_BLOCKED, JSON.stringify([...ids]));
  }

  /** Fusionne l'état bloqué du backend avec le fallback local (pour l'admin). */
  mergeBlockedState(skills: Skill[]): Skill[] {
    const blockedIds = new Set(this.getBlockedSkillIds());
    return skills.map((s) => ({ ...s, blocked: s.blocked ?? blockedIds.has(s.id) }));
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

  /** Supervision : bloquer/débloquer le CV (sans supprimer). Fallback localStorage si l’API ne gère pas. */
  setCVBlocked(freelancerId: number, blocked: boolean): Observable<any> {
    return this.http.patch<any>(`/api/cv/freelancer/${freelancerId}`, { blocked });
  }

  private static readonly STORAGE_KEY_CV_BLOCKED = 'matchfreelance_cv_blocked';

  getBlockedCVFreelancerIds(): number[] {
    try {
      const raw = localStorage.getItem(SkillService.STORAGE_KEY_CV_BLOCKED);
      if (!raw) return [];
      const arr = JSON.parse(raw);
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  }

  setCVBlockedLocal(freelancerId: number, blocked: boolean): void {
    const ids = new Set(this.getBlockedCVFreelancerIds());
    if (blocked) ids.add(freelancerId); else ids.delete(freelancerId);
    localStorage.setItem(SkillService.STORAGE_KEY_CV_BLOCKED, JSON.stringify([...ids]));
  }

  isCVBlocked(cv: any, freelancerId: number): boolean {
    return cv?.blocked ?? this.getBlockedCVFreelancerIds().includes(freelancerId);
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

  updatePortfolio(id: number, portfolioUrl: string, portfolioDescription?: string, blocked?: boolean): Observable<any> {
    const body: any = { portfolioUrl, portfolioDescription: portfolioDescription || '' };
    if (blocked !== undefined) body.blocked = blocked;
    return this.http.put<any>(`/api/portfolio/${id}`, body, {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
    });
  }

  /** Supervision : bloquer/débloquer un portfolio (sans supprimer). */
  setPortfolioBlocked(portfolio: { id: number; portfolioUrl?: string; portfolioDescription?: string }, blocked: boolean): Observable<any> {
    return this.updatePortfolio(
      portfolio.id,
      portfolio.portfolioUrl ?? '',
      portfolio.portfolioDescription,
      blocked
    );
  }

  private static readonly STORAGE_KEY_PORTFOLIO_BLOCKED = 'matchfreelance_portfolio_blocked';

  getBlockedPortfolioIds(): number[] {
    try {
      const raw = localStorage.getItem(SkillService.STORAGE_KEY_PORTFOLIO_BLOCKED);
      if (!raw) return [];
      const arr = JSON.parse(raw);
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  }

  setPortfolioBlockedLocal(id: number, blocked: boolean): void {
    const ids = new Set(this.getBlockedPortfolioIds());
    if (blocked) ids.add(id); else ids.delete(id);
    localStorage.setItem(SkillService.STORAGE_KEY_PORTFOLIO_BLOCKED, JSON.stringify([...ids]));
  }

  mergePortfolioBlocked(portfolios: any[]): any[] {
    const blockedIds = new Set(this.getBlockedPortfolioIds());
    return portfolios.map((p) => ({ ...p, blocked: p.blocked ?? blockedIds.has(p.id) }));
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
  saveBio(freelancerId: number, bio: string, blocked?: boolean): Observable<{ id?: number; freelancerId: number; bio: string }> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body: any = { bio };
    if (blocked !== undefined) body.blocked = blocked;
    return this.http.put<{ id?: number; freelancerId: number; bio: string }>(
      `/api/bio/freelancer/${freelancerId}`,
      body,
      { headers }
    );
  }

  /** Supervision : bloquer/débloquer le résumé (sans supprimer). Passe le bio actuel pour ne pas l’écraser. */
  setBioBlocked(freelancerId: number, blocked: boolean, currentBio: string): Observable<any> {
    return this.saveBio(freelancerId, currentBio, blocked);
  }

  private static readonly STORAGE_KEY_BIO_BLOCKED = 'matchfreelance_bio_blocked';

  getBlockedBioFreelancerIds(): number[] {
    try {
      const raw = localStorage.getItem(SkillService.STORAGE_KEY_BIO_BLOCKED);
      if (!raw) return [];
      const arr = JSON.parse(raw);
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  }

  setBioBlockedLocal(freelancerId: number, blocked: boolean): void {
    const ids = new Set(this.getBlockedBioFreelancerIds());
    if (blocked) ids.add(freelancerId); else ids.delete(freelancerId);
    localStorage.setItem(SkillService.STORAGE_KEY_BIO_BLOCKED, JSON.stringify([...ids]));
  }

  isBioBlocked(bio: string, freelancerId: number): boolean {
    return this.getBlockedBioFreelancerIds().includes(freelancerId);
  }

  deleteBio(freelancerId: number): Observable<void> {
    return this.http.delete<void>(`/api/bio/freelancer/${freelancerId}`);
  }
}
