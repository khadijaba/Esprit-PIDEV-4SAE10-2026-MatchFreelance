import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { SkillService } from '../../services/skill.service';
import { ExamenService } from '../../services/examen.service';
import { UserProfile } from '../../models/auth.model';
import { Skill } from '../../models/skill.model';
import { Certificat } from '../../models/examen.model';
import { SKILL_CATEGORY_LABELS } from '../../models/skill.model';

export interface BadgeData {
  level: string;
  score_completion_pct: number;
  badges: string[];
  badge_labels: Record<string, string>;
}

export interface BadgesResponse {
  by_freelancer: Record<string, BadgeData>;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  profile: UserProfile | null = null;
  skills: Skill[] = [];
  certificats: Certificat[] = [];
  badgeData: BadgeData | null = null;
  loading = true;
  error: string | null = null;
  deleting = false;
  deleteError: string | null = null;

  constructor(
    private auth: AuthService,
    private router: Router,
    private http: HttpClient,
    private skillService: SkillService,
    private examenService: ExamenService
  ) {}

  get isAdminRoute(): boolean {
    return this.router.url.startsWith('/admin');
  }

  ngOnInit() {
    if (!this.auth.isLoggedIn()) {
      this.error = 'Connectez-vous pour voir votre profil.';
      this.loading = false;
      return;
    }
    this.auth.getProfile().subscribe({
      next: (data) => {
        this.profile = data;
        this.loading = false;
        if (data.role === 'FREELANCER' && data.userId) {
          this.skillService.getByFreelancer(data.userId).subscribe({ next: (s) => (this.skills = s), error: () => {} });
          this.examenService.getCertificatsByFreelancer(data.userId).subscribe({ next: (c) => (this.certificats = c), error: () => {} });
          this.http.get<BadgesResponse>('/reports/badges.json').subscribe({
            next: (res) => {
              const b = res?.by_freelancer?.[String(data.userId)];
              if (b) this.badgeData = b;
            },
            error: () => {},
          });
        }
      },
      error: () => {
        this.error = 'Impossible de charger le profil.';
        this.loading = false;
      },
    });
  }

  categoryLabel(cat: string): string {
    return (SKILL_CATEGORY_LABELS as Record<string, string>)[cat] ?? cat?.replace(/_/g, ' ') ?? '';
  }

  roleLabel(role: string): string {
    const labels: Record<string, string> = {
      ADMIN: 'Administrateur',
      FREELANCER: 'Freelancer',
      CLIENT: 'Client',
    };
    return labels[role] ?? role;
  }

  deleteAccount(): void {
    if (
      !window.confirm(
        'Supprimer définitivement votre compte ? Cette action est irréversible.'
      )
    ) {
      return;
    }
    this.deleteError = null;
    this.deleting = true;
    this.auth.deleteMyAccount().subscribe({
      next: () => {
        this.auth.clearSession();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.deleting = false;
        this.deleteError = 'Impossible de supprimer le compte. Réessayez ou contactez le support.';
      },
    });
  }
}
