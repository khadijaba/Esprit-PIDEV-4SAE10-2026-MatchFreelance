import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ExamenService } from '../../services/examen.service';
import { InscriptionService } from '../../services/inscription.service';
import { AuthService } from '../../services/auth.service';
import { Inscription } from '../../models/inscription.model';
import { User } from '../../models/auth.model';
import { FreelancerRanking } from '../../models/examen.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  stats = { examens: 0, inscriptionsEnAttente: 0 };
  examensCount = 0;
  inscriptionsEnAttente: Inscription[] = [];
  inscriptionsLoading = false;
  allUsers: User[] = [];
  allUsersLoading = false;
  allUsersError: string | null = null;
  roleFilter: 'ALL' | 'ADMIN' | 'FREELANCER' | 'CLIENT' = 'ALL';

  rankingPreview: FreelancerRanking[] = [];
  rankingPreviewLoading = false;
  rankingPreviewError: string | null = null;

  constructor(
    private examenService: ExamenService,
    private inscriptionService: InscriptionService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.rankingPreviewLoading = true;
    this.examenService.getGlobalRanking().subscribe({
      next: (rows) => {
        this.rankingPreviewLoading = false;
        this.rankingPreview = (rows ?? []).slice(0, 5);
        this.rankingPreviewError = null;
      },
      error: (err) => {
        this.rankingPreviewLoading = false;
        this.rankingPreview = [];
        this.rankingPreviewError =
          err?.error?.message ??
          `Classement indisponible (HTTP ${err?.status ?? '—'}). Vérifiez le microservice Evaluation.`;
      },
    });
    this.examenService.getAll().subscribe({
      next: (data) => {
        this.examensCount = data.length;
        this.stats.examens = data.length;
      },
      error: () => {},
    });
    this.inscriptionsLoading = true;
    this.inscriptionService.getEnAttente().subscribe({
      next: (data) => {
        this.inscriptionsEnAttente = data;
        this.stats.inscriptionsEnAttente = data.length;
        this.inscriptionsLoading = false;
      },
      error: () => (this.inscriptionsLoading = false),
    });
    this.allUsersLoading = true;
    this.allUsersError = null;
    this.authService.getUsers().subscribe({
      next: (data) => {
        this.allUsers = Array.isArray(data) ? data : [];
        this.allUsersLoading = false;
      },
      error: (err) => {
        this.allUsersLoading = false;
        this.allUsersError =
          err?.status === 401
            ? 'Connectez-vous pour afficher la liste.'
            : 'Impossible de charger les utilisateurs. Démarrez le microservice USER, relancez ng serve, ou utilisez la Gateway sur 8050.';
      },
    });
  }

  get filteredUsers(): User[] {
    if (this.roleFilter === 'ALL') return this.allUsers;
    return this.allUsers.filter((u) => u.role === this.roleFilter);
  }

  get countAdmin(): number {
    return this.allUsers.filter((u) => u.role === 'ADMIN').length;
  }

  get countFreelancer(): number {
    return this.allUsers.filter((u) => u.role === 'FREELANCER').length;
  }

  get countClient(): number {
    return this.allUsers.filter((u) => u.role === 'CLIENT').length;
  }

  roleLabel(role: string): string {
    const labels: Record<string, string> = {
      ADMIN: 'Administrateur',
      FREELANCER: 'Freelancer',
      CLIENT: 'Client',
    };
    return labels[role] ?? role;
  }

  roleBadgeClass(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'bg-purple-100 text-purple-700',
      FREELANCER: 'bg-indigo-100 text-indigo-700',
      CLIENT: 'bg-slate-100 text-slate-700',
    };
    return map[role] ?? 'bg-gray-100 text-gray-700';
  }
}
