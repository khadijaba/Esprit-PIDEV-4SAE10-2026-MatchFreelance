import { Injectable } from '@angular/core';
import { SkillEvolutionEntry } from '../models/skill-evolution.model';

const STORAGE_KEY = 'matchfreelance_skill_evolution';

@Injectable({ providedIn: 'root' })
export class SkillEvolutionService {
  private loadAll(): SkillEvolutionEntry[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return [];
      const arr = JSON.parse(raw);
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  }

  private saveAll(entries: SkillEvolutionEntry[]): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
  }

  /**
   * Enregistre une évolution : ajout de compétence ou changement de niveau.
   */
  record(
    freelancerId: number,
    skillId: number,
    skillName: string,
    newLevel: string,
    previousLevel?: string,
    category?: string,
    source: 'manual' | 'project' = 'manual'
  ): void {
    const entries = this.loadAll();
    const entry: SkillEvolutionEntry = {
      id: `ev_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
      freelancerId,
      skillId,
      skillName,
      category,
      previousLevel: previousLevel || undefined,
      newLevel: newLevel?.trim() || 'BEGINNER',
      date: new Date().toISOString(),
      source,
    };
    entries.push(entry);
    this.saveAll(entries);
  }

  /** Historique des évolutions pour un freelancer, du plus récent au plus ancien. */
  getHistory(freelancerId: number): SkillEvolutionEntry[] {
    const entries = this.loadAll().filter((e) => e.freelancerId === freelancerId);
    return entries.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }

  /** Nombre de compétences ajoutées (sans niveau précédent) sur la période. */
  getAddedCount(freelancerId: number, sinceDays = 30): number {
    const since = new Date();
    since.setDate(since.getDate() - sinceDays);
    return this.getHistory(freelancerId).filter(
      (e) => !e.previousLevel && new Date(e.date) >= since
    ).length;
  }

  /** Nombre de progressions (changement de niveau) sur la période. */
  getUpgradedCount(freelancerId: number, sinceDays = 30): number {
    const since = new Date();
    since.setDate(since.getDate() - sinceDays);
    return this.getHistory(freelancerId).filter(
      (e) => e.previousLevel && new Date(e.date) >= since
    ).length;
  }
}
