import { Injectable } from '@angular/core';
import { Skill } from '../models/skill.model';
import { Project } from '../models/project.model';
import { SkillGapResult } from '../models/skill-gap.model';
import { normalizeProjectRequiredSkills, normalizeRequiredSkills, skillMatchesRequired } from '../utils/project-skill.util';

@Injectable({ providedIn: 'root' })
export class SkillGapService {
  /**
   * Calcule la compatibilité = compétences de l'espace freelancer vs compétences requises du projet.
   * - project : compétences requises (champ requiredSkills ou extraites de la description).
   * - freelancerSkills : compétences du profil freelancer (espace freelancer).
   * Si aucune compétence requise, compatibilité 100 %.
   */
  analyze(project: Project, freelancerSkills: Skill[] | unknown): SkillGapResult {
    const normalized = normalizeProjectRequiredSkills(project);
    const required = normalizeRequiredSkills(normalized.requiredSkills);
    const skillsArray = this.normalizeSkillsArray(freelancerSkills);
    const freelancerSet = this.buildFreelancerSkillSet(skillsArray);

    if (!required.length) {
      return { compatibility: 100, matchedSkills: [], missingSkills: [] };
    }

    const matchedSkills: string[] = [];
    const missingSkills: string[] = [];

    for (const r of required) {
      const trimmed = r.trim();
      if (!trimmed) continue;
      const norm = trimmed.toLowerCase().replace(/_/g, ' ');
      const hasIt = skillMatchesRequired(norm, freelancerSet.allNormalized);
      if (hasIt) {
        matchedSkills.push(trimmed);
      } else {
        missingSkills.push(trimmed);
      }
    }

    const compatibility = Math.round((matchedSkills.length / required.length) * 100);

    return {
      compatibility,
      matchedSkills,
      missingSkills,
    };
  }

  /** Le backend peut renvoyer un tableau ou un objet { skills/content/data: [...] } */
  private normalizeSkillsArray(data: Skill[] | unknown): Skill[] {
    if (Array.isArray(data)) return data;
    if (data && typeof data === 'object') {
      const obj = data as Record<string, unknown>;
      let arr = obj['skills'] ?? obj['content'] ?? obj['data'] ?? obj['result'] ?? obj['skillList'];
      if (!Array.isArray(arr) && obj['_embedded'] && typeof obj['_embedded'] === 'object') {
        arr = (obj['_embedded'] as Record<string, unknown>)['skills'];
      }
      return Array.isArray(arr) ? arr : [];
    }
    return [];
  }

  private buildFreelancerSkillSet(skills: Skill[]): { names: Set<string>; categories: Set<string>; allNormalized: string[] } {
    const names = new Set<string>();
    const categories = new Set<string>();
    const allNormalized: string[] = [];
    for (const s of skills) {
      const rec = s as unknown as Record<string, unknown>;
      const rawName = s.name ?? rec['skillName'] ?? rec['skill_name'] ?? rec['name'];
      const name = (typeof rawName === 'string' ? rawName : rawName != null ? String(rawName) : '').trim();
      if (name && name.length >= 2) {
        const n = name.toLowerCase();
        const nSpace = n.replace(/_/g, ' ');
        names.add(n);
        names.add(nSpace);
        allNormalized.push(n);
        allNormalized.push(nSpace);
      }
      const rawCat = s.category ?? rec['category'];
      const cat =
        typeof rawCat === 'object' && rawCat != null && 'name' in (rawCat as object)
          ? String((rawCat as Record<string, unknown>)['name'])
          : (typeof rawCat === 'string' ? rawCat : rawCat != null ? String(rawCat) : '');
      const catTrimmed = cat.trim();
      if (catTrimmed.length >= 2 && !catTrimmed.startsWith('[')) {
        const c = catTrimmed.toLowerCase().replace(/_/g, ' ');
        categories.add(c);
        allNormalized.push(c);
      }
    }
    return { names, categories, allNormalized };
  }

}
