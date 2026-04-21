import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ContractService } from '../../services/contract.service';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Contract } from '../../models/contract.model';
import { Project } from '../../models/project.model';
import { ContractChatPanelComponent } from '../contract-chat-panel/contract-chat-panel.component';
import { analyzePitch, matchfreelanceProjectToPitchJob, PitchAnalysisResult } from '../../services/pitch-analyzer.service';

@Component({
  selector: 'app-freelancer-contract-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ContractChatPanelComponent],
  templateUrl: './freelancer-contract-detail.component.html',
})
export class FreelancerContractDetailComponent implements OnInit, OnDestroy {
  contract: Contract | null = null;
  project: Project | null = null;
  loading = true;
  forbidden = false;

  progressDraft = 0;
  savingProgress = false;

  termsOpen = false;
  amendOpen = false;
  amendTerms = '';
  amendBudget: number | null = null;
  amendStart = '';
  amendEnd = '';
  savingAmend = false;

  extraAmount: number | null = null;
  extraReason = '';
  proposingExtra = false;
  extraAiResult: PitchAnalysisResult | null = null;

  cancelling = false;
  private routeSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private contractService: ContractService,
    private projectService: ProjectService,
    private auth: AuthService,
    private toast: ToastService
  ) {}

  get freelancerId(): number | null {
    const u = this.auth.getStoredUser();
    return u?.role === 'FREELANCER' ? u.userId : null;
  }

  get userId(): number | null {
    return this.auth.getStoredUser()?.userId ?? null;
  }

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe((pm) => {
      const id = Number(pm.get('contractId'));
      if (!Number.isFinite(id)) {
        this.router.navigate(['/mes-contrats']);
        return;
      }
      this.extraAiResult = null;
      this.load(id);
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  private load(id: number): void {
    const fid = this.freelancerId;
    if (!fid) {
      this.forbidden = true;
      this.loading = false;
      return;
    }
    this.loading = true;
    this.contractService.getById(id).subscribe({
      next: (c) => {
        if (c.freelancerId == null || c.freelancerId !== fid) {
          this.forbidden = true;
          this.contract = null;
          this.loading = false;
          return;
        }
        this.contract = c;
        this.progressDraft = this.progressValue(c);
        this.patchAmendForm(c);
        this.loading = false;
        this.loadProject(c.projectId);
      },
      error: () => {
        this.loading = false;
        this.toast.error('Contrat introuvable.');
        this.router.navigate(['/mes-contrats']);
      },
    });
  }

  private loadProject(projectId: number): void {
    this.projectService.getById(projectId).subscribe({
      next: (p) => (this.project = p),
      error: () => (this.project = null),
    });
  }

  private patchAmendForm(c: Contract): void {
    this.amendTerms = c.terms ?? '';
    this.amendBudget = c.proposedBudget ?? null;
    this.amendStart = this.toDateInput(c.startDate);
    this.amendEnd = this.toDateInput(c.endDate);
  }

  private toDateInput(iso?: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return d.toISOString().slice(0, 10);
  }

  private parseDateInput(s: string): string | undefined {
    if (!s?.trim()) return undefined;
    const d = new Date(s + 'T12:00:00');
    if (Number.isNaN(d.getTime())) return undefined;
    return d.toISOString();
  }

  progressValue(c: Contract): number {
    const p = c.progressPercent;
    return p != null ? Math.max(0, Math.min(100, p)) : 0;
  }

  statusClass(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'bg-amber-100 text-amber-900';
    if (s === 'COMPLETED') return 'bg-emerald-100 text-emerald-800';
    if (s === 'CANCELLED') return 'bg-slate-200 text-slate-700';
    return 'bg-slate-100 text-slate-600';
  }

  dateRangeLabel(c: Contract): string {
    const fmt = (s?: string) => {
      if (!s) return null;
      const d = new Date(s);
      if (Number.isNaN(d.getTime())) return null;
      return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short' }).format(d);
    };
    const a = fmt(c.startDate);
    const b = fmt(c.endDate);
    if (a && b) return `${a} — ${b}`;
    if (a) return a;
    if (b) return b;
    return '—';
  }

  skillChips(): string[] {
    const skills = this.project?.requiredSkills;
    return skills?.length ? skills.slice(0, 6) : [];
  }

  saveProgress(): void {
    const c = this.contract;
    const fid = this.freelancerId;
    if (!c || fid == null || c.status !== 'ACTIVE') return;
    this.savingProgress = true;
    this.contractService.updateProgress(c.id, this.progressDraft, fid).subscribe({
      next: (updated) => {
        this.contract = updated;
        this.savingProgress = false;
        this.toast.success('Progression enregistrée.');
      },
      error: () => {
        this.savingProgress = false;
        this.toast.error('Enregistrement impossible.');
      },
    });
  }

  saveAmend(): void {
    const c = this.contract;
    if (!c || c.clientId == null || c.freelancerId == null) return;
    this.savingAmend = true;
    const body = {
      projectId: c.projectId,
      freelancerId: c.freelancerId,
      clientId: c.clientId,
      terms: this.amendTerms.trim() || undefined,
      proposedBudget: this.amendBudget != null ? this.amendBudget : undefined,
      startDate: this.parseDateInput(this.amendStart),
      endDate: this.parseDateInput(this.amendEnd),
    };
    this.contractService.update(c.id, body).subscribe({
      next: (updated) => {
        this.contract = updated;
        this.savingAmend = false;
        this.amendOpen = false;
        this.toast.success('Contrat mis à jour.');
      },
      error: () => {
        this.savingAmend = false;
        this.toast.error('Modification refusée ou invalide.');
      },
    });
  }

  cancelContract(): void {
    const c = this.contract;
    if (!c || c.status !== 'ACTIVE') return;
    if (!confirm('Annuler ce contrat ? Cette action est réservée aux contrats actifs.')) return;
    this.cancelling = true;
    this.contractService.cancel(c.id).subscribe({
      next: (updated) => {
        this.contract = updated;
        this.cancelling = false;
        this.toast.success('Contrat annulé.');
      },
      error: () => {
        this.cancelling = false;
        this.toast.error('Annulation impossible.');
      },
    });
  }

  downloadPdf(): void {
    const c = this.contract;
    if (!c) return;
    window.open(`/api/contracts/${c.id}/pdf`, '_blank');
  }

  reviewExtraWithAi(): void {
    const amount = this.extraAmount;
    const reason = this.extraReason.trim();
    if (amount == null || amount <= 0) {
      this.toast.error('Indiquez un montant valide.');
      return;
    }
    const proj = this.project;
    const job = proj
      ? matchfreelanceProjectToPitchJob(proj)
      : {
          id: this.contract?.projectId ?? 0,
          title: 'Mission freelance',
          company: this.contract?.clientName ?? 'Client',
          budget: { min: this.contract?.proposedBudget ?? 0, max: this.contract?.proposedBudget ?? 0 },
          duration: 30,
          skills: [],
          level: 'mid',
          description: this.contract?.terms ?? '',
        };
    const pitch = `Demande de budget supplémentaire de ${amount} TND. ${reason || 'Pas de justification détaillée.'}`;
    this.extraAiResult = analyzePitch(job, pitch);
    this.toast.success('Analyse locale prête (aperçu ci-dessous).');
  }

  proposeExtra(): void {
    const c = this.contract;
    const fid = this.freelancerId;
    const amount = this.extraAmount;
    if (!c || fid == null || c.status !== 'ACTIVE') return;
    if (amount == null || amount <= 0) {
      this.toast.error('Montant invalide.');
      return;
    }
    if (c.pendingExtraAmount != null) {
      this.toast.error('Une demande est déjà en attente de réponse client.');
      return;
    }
    this.proposingExtra = true;
    this.contractService.proposeExtraBudget(c.id, amount, this.extraReason, fid).subscribe({
      next: (updated) => {
        this.contract = updated;
        this.proposingExtra = false;
        this.extraAmount = null;
        this.extraReason = '';
        this.toast.success('Proposition envoyée au client.');
      },
      error: () => {
        this.proposingExtra = false;
        this.toast.error('Envoi impossible (contrat ou montant).');
      },
    });
  }

  peerName(): string {
    return this.contract?.clientName?.trim() || 'Client';
  }
}
