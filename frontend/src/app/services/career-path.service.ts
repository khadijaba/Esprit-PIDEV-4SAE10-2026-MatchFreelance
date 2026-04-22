import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { Skill } from '../models/skill.model';
import { Project } from '../models/project.model';
import { Formation } from '../models/formation.model';
import { CareerPathRecommendation, SkillDemand } from '../models/career-path.model';
import { normalizeProjectRequiredSkills, normalizeRequiredSkills } from '../utils/project-skill.util';
import { ProjectService } from './project.service';
import { SkillService } from './skill.service';
import { FormationService } from './formation.service';

/** Normalise un nom de compétence pour comparaison (lowercase, espaces). */
function normalizeSkillName(s: string): string {
  return s.trim().toLowerCase().replace(/_/g, ' ');
}

/** Vérifie si le freelancer a déjà une compétence (match exact ou sous-chaîne). */
function freelancerHasSkill(freelancerNormalized: Set<string>, required: string): boolean {
  const r = normalizeSkillName(required);
  if (freelancerNormalized.has(r)) return true;
  for (const f of freelancerNormalized) {
    if (f.length >= 3 && (r.includes(f) || f.includes(r))) return true;
  }
  return false;
}

/** Extrait les noms de compétences d'une formation (titre, typeFormation, skills, description). */
function getFormationSkillNames(formation: Formation): string[] {
  const names: string[] = [];
  const add = (arr: string[] | undefined) => arr?.forEach((s) => names.push(String(s).trim()));
  add(formation.skills);
  add(formation.skillNames);
  add(formation.tags);
  const titre = (formation as { title?: string }).title ?? formation.titre;
  if (titre) names.push(titre);
  if (formation.typeFormation) names.push(formation.typeFormation);
  if (formation.description) names.push(formation.description);
  return [...new Set(names)].filter(Boolean);
}

/** Vérifie si une formation couvre au moins une des compétences cibles (match souple). */
function formationCoversSkills(formation: Formation, targetSkills: string[]): string[] {
  const covered: string[] = [];
  const formNames = getFormationSkillNames(formation).map(normalizeSkillName);
  for (const target of targetSkills) {
    const t = normalizeSkillName(target);
    if (formNames.some((f) => f.length >= 2 && (f.includes(t) || t.includes(f)))) covered.push(target);
  }
  return covered;
}

/** Exclut les libellés irréalistes (gg, g b, Patient), fragments, etc. */
function isRealisticSkillLabel(s: string): boolean {
  const t = s.trim();
  if (t.length < 3) return false;
  if (t.startsWith('(') || t.endsWith(')')) return false;
  const open = (t.match(/\(/g) ?? []).length;
  const close = (t.match(/\)/g) ?? []).length;
  if (open !== close) return false;
  if (/^[)\]}.,;:\s]+|[(\[{.,;:\s]+$/.test(t)) return false;
  if (/^[^a-zA-Z\u00C0-\u024F]+$/.test(t)) return false;
  const words = t.split(/\s+/).filter((w) => w.length > 0);
  if (words.length >= 2) {
    const minWordLen = Math.min(...words.map((w) => w.replace(/[^\p{L}]/gu, '').length));
    if (minWordLen < 2) return false;
  } else {
    const letters = (t.match(/[\p{L}]/gu) ?? []).length;
    if (letters < 3) return false;
    if (/(.)\1{2,}/.test(t)) return false;
  }
  return true;
}

@Injectable({ providedIn: 'root' })
export class CareerPathService {
  constructor(
    private projectService: ProjectService,
    private skillService: SkillService,
    private formationService: FormationService
  ) {}

  /**
   * Smart Career Path Generator : analyse les skills du freelancer, les projets publiés,
   * et recommande des compétences manquantes + formations disponibles sur la plateforme.
   */
  getRecommendations(freelancerUserId: number): Observable<CareerPathRecommendation> {
    return forkJoin({
      projects: this.projectService.getAll().pipe(catchError(() => of([]))),
      freelancerSkills: this.skillService.getByFreelancerId(freelancerUserId).pipe(catchError(() => of([]))),
      formations: this.formationService.getAll().pipe(catchError(() => of([]))),
    }).pipe(
      map(({ projects, freelancerSkills, formations }) =>
        this.computeRecommendation(projects, freelancerSkills, formations)
      )
    );
  }

  private buildFreelancerSkillSet(skills: Skill[]): Set<string> {
    const set = new Set<string>();
    for (const s of skills) {
      const rec = s as unknown as Record<string, unknown>;
      const name = (s.name ?? rec['skillName'] ?? rec['skill_name'] ?? rec['name'])?.toString?.() ?? '';
      if (name.trim()) {
        set.add(normalizeSkillName(name));
        set.add(normalizeSkillName(name).replace(/_/g, ' '));
      }
      const cat = s.category ?? rec['category'];
      const catStr = typeof cat === 'object' && cat != null && 'name' in (cat as object)
        ? String((cat as Record<string, unknown>)['name'])
        : (typeof cat === 'string' ? cat : cat != null ? String(cat) : '');
      if (catStr.trim()) set.add(normalizeSkillName(catStr));
    }
    return set;
  }

  private computeSkillDemand(projects: Project[]): Map<string, { count: number; totalBudget: number }> {
    const demand = new Map<string, { count: number; totalBudget: number }>();
    for (const p of projects) {
      const normalized = normalizeProjectRequiredSkills(p);
      const skills = normalizeRequiredSkills(normalized.requiredSkills);
      const budget = p.budget ?? 0;
      for (const skill of skills) {
        const key = skill.trim();
        if (!key || !isRealisticSkillLabel(key)) continue;
        const cur = demand.get(key) ?? { count: 0, totalBudget: 0 };
        cur.count += 1;
        cur.totalBudget += budget;
        demand.set(key, cur);
      }
    }
    return demand;
  }

  private computeRecommendation(
    projects: Project[],
    freelancerSkills: Skill[],
    formations: Formation[]
  ): CareerPathRecommendation {
    const freelancerSet = this.buildFreelancerSkillSet(freelancerSkills);
    const demand = this.computeSkillDemand(projects);
    const totalProjects = projects.length;

    const missing: SkillDemand[] = [];
    for (const [skill, data] of demand.entries()) {
      if (!isRealisticSkillLabel(skill) || freelancerHasSkill(freelancerSet, skill)) continue;
      const percent = totalProjects > 0 ? Math.round((data.count / totalProjects) * 100) : 0;
      const avgBudget = data.count > 0 ? Math.round(data.totalBudget / data.count) : 0;
      missing.push({
        skill,
        projectCount: data.count,
        percentOfProjects: percent,
        avgBudget,
        totalBudget: data.totalBudget,
      });
    }
    missing.sort((a, b) => b.percentOfProjects - a.percentOfProjects);
    const topMissing = missing.slice(0, 10);
    const topMissingNames = topMissing.map((m) => m.skill);

    const suggestedFormations: { formation: Formation; coversSkills: string[] }[] = [];
    const seenIds = new Set<number>();
    for (const formation of formations) {
      const covers = formationCoversSkills(formation, topMissingNames);
      if (covers.length > 0 && !seenIds.has(formation.id)) {
        seenIds.add(formation.id);
        suggestedFormations.push({ formation, coversSkills: covers });
      }
    }

    let impactMessage = '';
    if (topMissing.length > 0) {
      const names = topMissing.slice(0, 3).map((m) => m.skill).join(', ');
      const avgPercent = topMissing.length > 0
        ? Math.round(topMissing.reduce((s, m) => s + m.percentOfProjects, 0) / Math.min(3, topMissing.length))
        : 0;
      impactMessage = `Si vous ajoutez ${names} → plus d'opportunités (${avgPercent}% des projets les demandent).`;
      const avgBudgetGain = topMissing.slice(0, 3).reduce((s, m) => s + m.avgBudget, 0) / Math.min(3, topMissing.length);
      if (avgBudgetGain > 0) impactMessage += ' Budget moyen des projets concernés : ' + Math.round(avgBudgetGain) + '$.';
    } else {
      impactMessage = 'Vos compétences correspondent bien à la demande actuelle. Continuez à vous former pour rester compétitif.';
    }

    return {
      missingSkillsWithDemand: topMissing,
      suggestedFormations,
      impactMessage,
      totalProjectsAnalyzed: totalProjects,
    };
  }
}
