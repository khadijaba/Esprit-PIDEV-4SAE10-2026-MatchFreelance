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

/**
 * Score = (Compétences × 0,7) + (Expérience × 0,3). Note et projets réalisés non utilisés.
 */
const WEIGHTS = {
  skillMatch: 0.7,
  experience: 0.3,
};

/** Expérience max pour normaliser à 100 % */
const MAX_YEARS_EXPERIENCE = 10;

@Injectable({ providedIn: 'root' })
export class TalentMatchingService {
  constructor(
    private projectService: ProjectService,
    private skillService: SkillService,
    private userService: UserService
  ) {}

  /**
   * Retourne les freelancers les plus compatibles avec le projet (par défaut Top 5).
   * Utilise SKILL (compétences) et USER (réputation) pour calculer le MatchScore.
   * @param limit nombre max de résultats (défaut 5, ex. 20 pour Smart Discovery).
   */
  getTopMatchingFreelancers(project: Project, limit = 5): Observable<MatchingFreelancer[]> {
    const normalized = normalizeProjectRequiredSkills(project);
    const required = normalizeRequiredSkills(normalized.requiredSkills);
    if (!required.length) return of([]);
    return this.skillService.getAll().pipe(
      switchMap((skills) =>
        this.userService.getAll('FREELANCER').pipe(
          map((users) => this.computeMatching(required, skills, users, limit))
        )
      )
    );
  }

  /**
   * Par projet ID : charge le projet puis retourne le top 5.
   * Normalise requiredSkills (backend peut renvoyer string ou array).
   */
  getTopMatchingByProjectId(projectId: number, limit = 5): Observable<MatchingFreelancer[]> {
    return this.projectService.getById(projectId).pipe(
      switchMap((project) => {
        const normalized = normalizeProjectRequiredSkills(project);
        const required = normalizeRequiredSkills(normalized.requiredSkills);
        if (!required.length) return of([]);
        return this.skillService.getAll().pipe(
          switchMap((skills) =>
            this.userService.getAll('FREELANCER').pipe(
              map((users) => this.computeMatching(required, skills, users, limit))
            )
          )
        );
      })
    );
  }

  private computeMatching(required: string[], allSkills: Skill[], users: User[], limit = 5): MatchingFreelancer[] {
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

      const score = Math.round((skillMatch / 100) * WEIGHTS.skillMatch * 100 + (experience / 100) * WEIGHTS.experience * 100);
      const scoreClamped = Math.min(100, Math.max(0, score));

      results.push({
        freelancerId: fid,
        fullName: (user.fullName ?? user.username ?? user.email ?? `Freelancer #${fid}`).toString().trim() || `Freelancer #${fid}`,
        email: user.email,
        score: scoreClamped,
        skillMatch: Math.round(skillMatch),
        experience: Math.round(experience),
        userRating: 0,
        completedProjects: 0,
      });
    }

    results.sort((a, b) => b.score - a.score);
    return results.slice(0, limit);
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

}
