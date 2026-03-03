import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Project } from '../models/project.model';
import { Skill } from '../models/skill.model';
import { User } from '../models/user.model';
import { MatchingFreelancer } from '../models/matching.model';
import { normalizeProjectRequiredSkills, normalizeRequiredSkills, skillMatchesRequired } from '../utils/project-skill.util';
import { ProjectService } from './project.service';
import { SkillService } from './skill.service';
import { UserService } from './user.service';

const WEIGHTS = {
  skillMatch: 0.5,
  experience: 0.2,
  userRating: 0.2,
  completedProjects: 0.1,
};

/** Expérience max pour normaliser à 100 % */
const MAX_YEARS_EXPERIENCE = 10;
/** Note max (étoiles) pour normaliser */
const MAX_RATING = 5;
/** Projets complétés max pour normaliser à 100 % */
const MAX_COMPLETED_PROJECTS = 50;

@Injectable({ providedIn: 'root' })
export class TalentMatchingService {
  constructor(
    private projectService: ProjectService,
    private skillService: SkillService,
    private userService: UserService
  ) {}

  /**
   * Retourne le Top 5 des freelancers les plus compatibles avec le projet.
   * Utilise SKILL (compétences) et USER (réputation) pour calculer le MatchScore.
   */
  getTopMatchingFreelancers(project: Project): Observable<MatchingFreelancer[]> {
    const normalized = normalizeProjectRequiredSkills(project);
    const required = normalizeRequiredSkills(normalized.requiredSkills);
    if (!required.length) return of([]);
    return this.skillService.getAll().pipe(
      switchMap((skills) =>
        this.userService.getAll('FREELANCER').pipe(
          map((users) => this.computeMatching(required, skills, users))
        )
      )
    );
  }

  /**
   * Par projet ID : charge le projet puis retourne le top 5.
   * Normalise requiredSkills (backend peut renvoyer string ou array).
   */
  getTopMatchingByProjectId(projectId: number): Observable<MatchingFreelancer[]> {
    return this.projectService.getById(projectId).pipe(
      switchMap((project) => {
        const normalized = normalizeProjectRequiredSkills(project);
        const required = normalizeRequiredSkills(normalized.requiredSkills);
        if (!required.length) return of([]);
        return this.skillService.getAll().pipe(
          switchMap((skills) =>
            this.userService.getAll('FREELANCER').pipe(
              map((users) => this.computeMatching(required, skills, users))
            )
          )
        );
      })
    );
  }

  private computeMatching(required: string[], allSkills: Skill[], users: User[]): MatchingFreelancer[] {
    const requiredNorm = required.map((s) => s.toLowerCase().trim());
    const byFreelancer = new Map<number, Skill[]>();
    for (const s of allSkills) {
      if (!byFreelancer.has(s.freelancerId)) byFreelancer.set(s.freelancerId, []);
      byFreelancer.get(s.freelancerId)!.push(s);
    }

    const freelancerIds = new Set(byFreelancer.keys());
    const usersById = new Map(users.map((u) => [u.id, u]));

    const results: MatchingFreelancer[] = [];
    for (const fid of freelancerIds) {
      const user = usersById.get(fid);
      if (!user) continue;
      const skills = byFreelancer.get(fid) || [];
      const skillMatch = this.computeSkillMatch(requiredNorm, skills);
      const experience = this.computeExperience(skills);
      const userRating = this.getUserRating(user);
      const completedProjects = this.getCompletedProjects(user);

      const scoreNorm =
        (skillMatch / 100) * WEIGHTS.skillMatch +
        (experience / 100) * WEIGHTS.experience +
        (userRating / MAX_RATING) * WEIGHTS.userRating +
        (completedProjects / MAX_COMPLETED_PROJECTS) * WEIGHTS.completedProjects;
      const score = Math.round(Math.min(1, scoreNorm) * 100);

      results.push({
        freelancerId: fid,
        fullName: (user.fullName ?? user.username ?? user.email ?? `Freelancer #${fid}`).toString().trim() || `Freelancer #${fid}`,
        email: user.email,
        score,
        skillMatch: Math.round(skillMatch),
        experience: Math.round(experience),
        userRating,
        completedProjects,
      });
    }

    results.sort((a, b) => b.score - a.score);
    return results.slice(0, 5);
  }

  private computeSkillMatch(requiredNorm: string[], skills: Skill[]): number {
    if (requiredNorm.length === 0) return 100;
    const allNormalized: string[] = [];
    for (const s of skills) {
      const n = (s.name ?? '').toString().toLowerCase().trim().replace(/_/g, ' ');
      if (n) allNormalized.push(n);
      const c = String(s.category ?? '').toLowerCase().replace(/_/g, ' ');
      if (c) allNormalized.push(c);
    }
    let matched = 0;
    for (const r of requiredNorm) {
      const norm = r.replace(/_/g, ' ');
      if (skillMatchesRequired(norm, allNormalized)) matched++;
    }
    return (matched / requiredNorm.length) * 100;
  }

  private computeExperience(skills: Skill[]): number {
    if (skills.length === 0) return 0;
    const totalYears = skills.reduce((sum, s) => sum + (s.yearsOfExperience ?? 0), 0);
    const avg = totalYears / skills.length;
    return Math.min(100, (avg / MAX_YEARS_EXPERIENCE) * 100);
  }

  private getUserRating(user: User): number {
    const rating = (user as User & { rating?: number }).rating;
    if (typeof rating === 'number' && rating >= 0 && rating <= MAX_RATING) return rating;
    return 2.5; // valeur par défaut neutre
  }

  private getCompletedProjects(user: User): number {
    const count = (user as User & { completedProjects?: number }).completedProjects;
    if (typeof count === 'number' && count >= 0) return Math.min(MAX_COMPLETED_PROJECTS, count);
    return 0;
  }
}
