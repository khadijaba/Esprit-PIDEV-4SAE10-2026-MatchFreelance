import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProjectService } from '../../services/project.service';
import { SkillService } from '../../services/skill.service';
import { ExamenService } from '../../services/examen.service';
import { Project, ProjectStatus } from '../../models/project.model';
import { User } from '../../models/auth.model';
import { Skill } from '../../models/skill.model';
import { Certificat } from '../../models/examen.model';

@Component({
  selector: 'app-dashboard-client',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-client.component.html',
})
export class DashboardClientComponent implements OnInit {
  projects: Project[] = [];
  freelancers: User[] = [];
  freelancerSkills: Map<number, Skill[]> = new Map();
  freelancerCertificats: Map<number, Certificat[]> = new Map();
  loading = true;
  error: string | null = null;

  stats = { total: 0, open: 0, inProgress: 0, completed: 0 };

  constructor(
    public auth: AuthService,
    private projectService: ProjectService,
    private skillService: SkillService,
    private examenService: ExamenService
  ) {}

  ngOnInit() {
    if (!this.auth.isLoggedIn() || !this.auth.isProjectOwner()) {
      this.error = 'Connectez-vous en tant que porteur de projet (Project Owner) pour accéder à ce tableau de bord.';
      this.loading = false;
      return;
    }
    this.loadProjects();
    this.auth.getUsers().subscribe({
      next: (users) => {
        this.freelancers = (users ?? []).filter((u) => u.role === 'FREELANCER');
        this.freelancers.forEach((f) => {
          this.skillService.getByFreelancer(f.id).subscribe({
            next: (s) => this.freelancerSkills.set(f.id, s),
            error: () => {},
          });
          this.examenService.getCertificatsByFreelancer(f.id).subscribe({
            next: (c) => this.freelancerCertificats.set(f.id, c),
            error: () => {},
          });
        });
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  loadProjects() {
    const userId = this.auth.getStoredUser()?.userId;
    const obs = userId ? this.projectService.getByOwner(userId) : this.projectService.getAll();
    obs.subscribe({
      next: (data) => {
        this.projects = data ?? [];
        this.stats.total = this.projects.length;
        this.stats.open = this.projects.filter((p) => p.status === 'OPEN').length;
        this.stats.inProgress = this.projects.filter((p) => p.status === 'IN_PROGRESS').length;
        this.stats.completed = this.projects.filter((p) => p.status === 'COMPLETED').length;
      },
      error: () => {},
    });
  }

  getSkillsFor(freelancerId: number): Skill[] {
    return this.freelancerSkills.get(freelancerId) ?? [];
  }

  getCertificatsFor(freelancerId: number): Certificat[] {
    return this.freelancerCertificats.get(freelancerId) ?? [];
  }

  statusClass(s: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      DRAFT: 'bg-slate-100 text-slate-700',
      OPEN: 'bg-emerald-100 text-emerald-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[s] ?? 'bg-gray-100 text-gray-700';
  }
}
