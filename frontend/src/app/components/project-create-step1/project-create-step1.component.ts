import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { TeamAiService, AnalyzeProjectResponse } from '../../services/team-ai.service';
import { ProjectRequest } from '../../models/project.model';
import {
  buildDescriptionCoaching,
  DescriptionCoachingResult,
} from '../../utils/project-description-coaching';

@Component({
  selector: 'app-project-create-step1',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-create-step1.component.html',
})
export class ProjectCreateStep1Component {
  title = '';
  description = '';
  budget = 0;
  duration = 1;
  skillInput = '';
  requiredSkills: string[] = [];
  saving = false;
  nlpBusy = false;
  error: string | null = null;
  includeLlmDraft = false;
  /** Panneau coaching + brouillon (après analyse NLP). */
  nlpFeedback: DescriptionCoachingResult | null = null;

  constructor(
    private projectService: ProjectService,
    private auth: AuthService,
    private router: Router,
    private teamAi: TeamAiService,
    private toast: ToastService
  ) {}

  addSkill(): void {
    const s = this.skillInput.trim();
    if (!s) return;
    const parts = s.split(',').map((x) => x.trim()).filter(Boolean);
    for (const p of parts) {
      if (!this.requiredSkills.includes(p)) {
        this.requiredSkills.push(p);
      }
    }
    this.skillInput = '';
  }

  removeSkill(s: string): void {
    this.requiredSkills = this.requiredSkills.filter((x) => x !== s);
  }

  /** Analyse NLP via la Gateway → Team AI (`POST /api/team-ai/analyze-project`). */
  analyzeNlp(): void {
    const desc = this.description.trim();
    if (!desc) {
      this.toast.error('Renseignez une description pour l’analyse.');
      return;
    }
    const title = this.title.trim() || this.fallbackTitleFromDescription(desc);
    const payload = { title, description: desc, preferLlm: this.includeLlmDraft };
    this.nlpBusy = true;
    this.teamAi
      .analyzeProject(payload)
      .pipe(finalize(() => (this.nlpBusy = false)))
      .subscribe({
        next: (res) => {
          this.applyAnalyzeResult(res, { fillTitle: !this.title.trim() });
          this.nlpFeedback = buildDescriptionCoaching(
            this.title,
            this.description,
            res,
            Number(this.budget),
            Number(this.duration)
          );
          const parts: string[] = [];
          if (res.complexity) parts.push(`Complexité : ${res.complexity}`);
          if (res.roles?.length) parts.push(`${res.roles.length} rôle(s) détecté(s)`);
          this.toast.success(parts.length ? `Analyse terminée — ${parts.join(' · ')}` : 'Analyse terminée.');
          const cur = res.budgetEstimate?.currency;
          if (cur && cur.toUpperCase() !== 'TND') {
            this.toast.info(`Budget moyen recopié selon la devise renvoyée par l’API (${cur}) — convertissez ou ajustez en TND.`);
          }
        },
        error: (err) => this.toast.error(this.formatTeamAiError(err)),
      });
  }

  /** Reformulation : réutilise l’analyse et préfixe la description avec la synthèse IA. */
  reformulate(): void {
    const desc = this.description.trim();
    if (!desc) {
      this.toast.error('Renseignez une description à reformuler.');
      return;
    }
    const title = this.title.trim() || this.fallbackTitleFromDescription(desc);
    this.nlpBusy = true;
    this.teamAi
      .analyzeProject({ title, description: desc, preferLlm: this.includeLlmDraft })
      .pipe(finalize(() => (this.nlpBusy = false)))
      .subscribe({
               next: (res) => {
          this.applyAnalyzeResult(res, { fillTitle: !this.title.trim() });
          const summary = res.summary?.trim();
          if (!summary) {
            this.toast.info('Aucune synthèse courte — le panneau ci-dessous propose un brouillon structuré.');
          } else if (desc.startsWith(summary)) {
            this.toast.info('La description commence déjà par cette synthèse.');
          } else {
            this.description = `${summary}\n\n---\n\n${desc}`;
            this.toast.success('Description enrichie avec la synthèse proposée par l’IA.');
          }
          this.nlpFeedback = buildDescriptionCoaching(
            this.title,
            this.description,
            res,
            Number(this.budget),
            Number(this.duration)
          );
        },
        error: (err) => this.toast.error(this.formatTeamAiError(err)),
      });
  }

  private fallbackTitleFromDescription(desc: string): string {
    const line = desc
      .split(/\n/)
      .map((l) => l.trim())
      .find((l) => l.length > 0);
    if (line && line.length <= 120) return line;
    return 'Projet';
  }

  private applyAnalyzeResult(res: AnalyzeProjectResponse, opts: { fillTitle: boolean }): void {
    const skills = res.requiredSkills ?? [];
    for (const s of skills) {
      const t = String(s).trim();
      if (!t) continue;
      const exists = this.requiredSkills.some((x) => x.toLowerCase() === t.toLowerCase());
      if (!exists) this.requiredSkills.push(t);
    }

    const be = res.budgetEstimate;
    if (be && typeof be.minAmount === 'number' && typeof be.maxAmount === 'number') {
      this.budget = Math.max(0, Math.round((be.minAmount + be.maxAmount) / 2));
    }

    if (res.durationEstimateDays != null && res.durationEstimateDays >= 1) {
      this.duration = res.durationEstimateDays;
    }

    if (opts.fillTitle) {
      const line = this.description
        .split(/\n/)
        .map((l) => l.trim())
        .find((l) => l.length > 0);
      if (line && line.length <= 140) this.title = line.slice(0, 140);
    }
  }

  applyNlpDraft(): void {
    if (!this.nlpFeedback?.draftDescription?.trim()) return;
    this.description = this.nlpFeedback.draftDescription.trim();
    this.toast.success('Description remplacée par le brouillon. Vous pouvez encore l’éditer avant enregistrement.');
  }

  dismissNlpFeedback(): void {
    this.nlpFeedback = null;
  }

  private formatTeamAiError(err: unknown): string {
    const e = err as { error?: { detail?: unknown; message?: string }; message?: string };
    const d = e?.error?.detail;
    if (typeof d === 'string') return d;
    if (Array.isArray(d)) {
      return d
        .map((x: { msg?: string }) => x?.msg)
        .filter(Boolean)
        .join(' — ') || 'Erreur de validation Team AI.';
    }
    return (
      e?.error?.message ??
      e?.message ??
      'Impossible de joindre Team AI (Gateway8050 + service sur le port configuré).'
    );
  }

  saveDraft(): void {
    const u = this.auth.getStoredUser();
    if (!u?.userId) {
      this.error = 'Session invalide. Reconnectez-vous.';
      return;
    }
    if (!this.title.trim() || !this.description.trim()) {
      this.error = 'Titre et description sont obligatoires.';
      return;
    }
    const body: ProjectRequest = {
      title: this.title.trim(),
      description: this.description.trim(),
      budget: Number(this.budget),
      duration: Number(this.duration),
      projectOwnerId: u.userId,
      status: 'DRAFT',
      requiredSkills: [...this.requiredSkills],
    };
    this.saving = true;
    this.error = null;
    this.projectService.create(body).subscribe({
      next: (p) => {
        this.saving = false;
        this.router.navigate(['/projets/nouveau/verification', p.id]);
      },
      error: (err) => {
        this.saving = false;
        this.error =
          err?.error?.message ?? err?.message ?? 'Échec création brouillon. Vérifiez Project + MySQL + Gateway.';
      },
    });
  }
}
