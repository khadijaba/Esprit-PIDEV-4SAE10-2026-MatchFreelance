import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../models/project.model';
import { FreelancerFitDto } from '../../models/freelancer-fit.model';
import { User } from '../../models/auth.model';

@Component({
  selector: 'app-project-owner-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './project-owner-detail.component.html',
})
export class ProjectOwnerDetailComponent implements OnInit {
  project: Project | null = null;
  topFreelancers: { user: User; fit: FreelancerFitDto }[] = [];
  loading = true;
  fitLoading = false;
  promoting = false;
  error: string | null = null;
  /** Erreur chargement scores (liste users ou microservice Project). */
  fitError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.router.navigate(['/projets']);
      return;
    }
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        const me = this.auth.getStoredUser();
        if (me?.userId && p.projectOwnerId !== me.userId) {
          this.error = 'Ce détail « propriétaire » est réservé au porteur du projet.';
          this.loading = false;
          return;
        }
        this.loadFit(id);
        this.loading = false;
      },
      error: () => {
        this.error = 'Projet introuvable.';
        this.loading = false;
      },
    });
  }

  /** Passe OPEN → IN_PROGRESS pour débloquer l’écran Supervision (workflow simplifié). */
  markInProgress(): void {
    if (!this.project) return;
    this.promoting = true;
    this.projectService
      .update(this.project.id, {
        title: this.project.title,
        description: this.project.description,
        budget: this.project.budget,
        duration: this.project.duration,
        projectOwnerId: this.project.projectOwnerId,
        status: 'IN_PROGRESS',
        requiredSkills: this.project.requiredSkills ?? [],
      })
      .subscribe({
        next: (p) => {
          this.project = p;
          this.promoting = false;
        },
        error: () => (this.promoting = false),
      });
  }

  private loadFit(projectId: number): void {
    this.fitLoading = true;
    this.fitError = null;
    this.auth.getFreelancersForProjectMatching().subscribe({
      next: (freelancers) => {
        const ids = [
          ...new Set(freelancers.map((u) => u.id).filter((n) => Number.isFinite(n) && n > 0)),
        ].slice(0, 40);
        if (ids.length === 0) {
          this.fitLoading = false;
          this.fitError =
            'Aucun compte FREELANCER trouvé (vérifiez GET /api/users/all?role=FREELANCER et la base User).';
          return;
        }
        this.projectService.getFreelancerFit(projectId, ids).subscribe({
          next: (batch) => {
            const byId = new Map<number, User>(freelancers.map((u) => [u.id, u]));
            const rows = (batch.freelancers ?? [])
              .map((fit) => {
                const fid = Number(fit.freelancerId);
                if (!Number.isFinite(fid)) {
                  return null;
                }
                const user =
                  byId.get(fid) ??
                  ({
                    id: fid,
                    email: '',
                    fullName: `Freelancer #${fid}`,
                    role: 'FREELANCER',
                  } as User);
                return { user, fit };
              })
              .filter((x): x is { user: User; fit: FreelancerFitDto } => x != null);
            rows.sort(
              (a, b) =>
                (b.fit.weightedMatchPercent ?? b.fit.successScore) -
                (a.fit.weightedMatchPercent ?? a.fit.successScore)
            );
            this.topFreelancers = rows.slice(0, 5);
            this.fitLoading = false;
            if (this.topFreelancers.length === 0) {
              this.fitError =
                'Le microservice Project n’a renvoyé aucun score (paramètre freelancerIds ou erreur silencieuse côté Candidature).';
            }
          },
          error: (err) => {
            this.fitLoading = false;
            this.fitError =
              err?.error?.message ??
              err?.message ??
              'Impossible de charger /api/projects/…/freelancer-fit (Gateway + microservice Project).';
          },
        });
      },
      error: (err) => {
        this.fitLoading = false;
        this.fitError =
          err?.error?.message ??
          err?.message ??
          'Impossible de charger la liste des freelancers (User).';
      },
    });
  }

  displayMatchPercent(fit: FreelancerFitDto): number {
    return fit.weightedMatchPercent ?? fit.successScore;
  }

  /** Initiale affichée sur l’avatar (une lettre, comme la maquette). */
  initials(u: { fullName: string | null; email: string }): string {
    const n = (u.fullName || u.email || '?').trim();
    const letter = n.charAt(0);
    return letter ? letter.toUpperCase() : '?';
  }

  contactFreelancer(u: { email: string; fullName: string | null }): void {
    const sub = encodeURIComponent('MatchFreelance — intérêt pour votre profil');
    window.location.href = `mailto:${u.email}?subject=${sub}`;
  }
}
