import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ContractService } from '../../services/contract.service';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Contract } from '../../models/contract.model';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-freelancer-contracts',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './freelancer-contracts.component.html',
})
export class FreelancerContractsComponent implements OnInit {
  @Input() embedMode = false;
  @Input() maxEmbedItems: number | null = null;

  contracts: Contract[] = [];
  projectById: Record<number, Project | null> = {};
  loading = true;
  cancellingId: number | null = null;

  constructor(
    private contractService: ContractService,
    private projectService: ProjectService,
    private auth: AuthService,
    private toast: ToastService
  ) {}

  get freelancerId(): number | null {
    const u = this.auth.getStoredUser();
    return u?.role === 'FREELANCER' ? u.userId : null;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const fid = this.freelancerId;
    if (!fid) {
      this.loading = false;
      this.contracts = [];
      return;
    }
    this.loading = true;
    this.contractService.listByFreelancer(fid).subscribe({
      next: (list) => {
        let rows = [...(list ?? [])].sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
        if (this.embedMode && this.maxEmbedItems != null && this.maxEmbedItems > 0) {
          rows = rows.slice(0, this.maxEmbedItems);
        }
        this.contracts = rows;
        this.loadProjects(rows);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Impossible de charger vos contrats.');
      },
    });
  }

  private loadProjects(contracts: Contract[]): void {
    const ids = [...new Set(contracts.map((c) => c.projectId).filter((x) => x != null))];
    if (ids.length === 0) return;
    forkJoin(
      ids.map((id) =>
        this.projectService.getById(id).pipe(
          catchError(() => of(null))
        )
      )
    ).subscribe((projects) => {
      const map: Record<number, Project | null> = { ...this.projectById };
      projects.forEach((p, i) => {
        map[ids[i]] = p;
      });
      this.projectById = map;
    });
  }

  projectTitle(c: Contract): string {
    return this.projectById[c.projectId]?.title ?? `Projet #${c.projectId}`;
  }

  skillChips(c: Contract): string[] {
    const skills = this.projectById[c.projectId]?.requiredSkills;
    return skills?.length ? skills.slice(0, 4) : [];
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

  statusClass(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'bg-amber-100 text-amber-900';
    if (s === 'COMPLETED') return 'bg-emerald-100 text-emerald-800';
    if (s === 'CANCELLED') return 'bg-slate-200 text-slate-700';
    if (s === 'DRAFT') return 'bg-slate-100 text-slate-600';
    return 'bg-slate-100 text-slate-700';
  }

  progress(c: Contract): number {
    const p = c.progressPercent;
    return p != null ? Math.max(0, Math.min(100, p)) : 0;
  }

  downloadPdf(id: number): void {
    window.open(`/api/contracts/${id}/pdf`, '_blank');
  }

  cancelContract(c: Contract): void {
    if ((c.status ?? '').toUpperCase() !== 'ACTIVE') {
      this.toast.error('Seuls les contrats actifs peuvent être annulés.');
      return;
    }
    if (!confirm('Annuler ce contrat ?')) return;
    this.cancellingId = c.id;
    this.contractService.cancel(c.id).subscribe({
      next: () => {
        this.cancellingId = null;
        this.toast.success('Contrat annulé.');
        this.load();
      },
      error: () => {
        this.cancellingId = null;
        this.toast.error('Annulation impossible.');
      },
    });
  }
}
