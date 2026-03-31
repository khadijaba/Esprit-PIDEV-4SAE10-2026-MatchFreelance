import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { SkillService } from '../../services/skill.service';
import { UserService } from '../../services/user.service';
import { Project, ProjectStatus } from '../../models/project.model';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  projects: Project[] = [];
  users: User[] = [];
  usersLoadError = false;
  stats = {
    total: 0,
    open: 0,
    inProgress: 0,
    completed: 0,
    cancelled: 0,
    totalBudget: 0,
    totalSkills: 0,
    totalCVs: 0,
    totalPortfolios: 0
  };

  constructor(
    private projectService: ProjectService,
    private skillService: SkillService,
    private userService: UserService
  ) {}

  ngOnInit() {
    this.loadProjects();
    this.loadSkillsStats();
    this.loadCVsStats();
    this.loadPortfoliosStats();
    this.loadUsers();
  }

  loadUsers() {
    this.usersLoadError = false;
    this.userService.getAll().subscribe({
      next: (data) => (this.users = data ?? []),
      error: () => {
        this.usersLoadError = true;
        this.users = [];
      },
    });
  }

  displayName(u: User): string {
    return (u.fullName ?? u.username ?? u.email ?? `#${u.id}`).toString().trim() || `Profil #${u.id}`;
  }

  roleLabel(role: string | undefined): string {
    if (!role) return '—';
    const map: Record<string, string> = { FREELANCER: 'Freelancer', CLIENT: 'Project Owner', PROJECT_OWNER: 'Project Owner', ADMIN: 'Admin' };
    return map[role.toUpperCase()] ?? role;
  }

  loadProjects() {
    this.projectService.getAll().subscribe((data) => {
      this.projects = data;
      this.stats.total = data.length;
      this.stats.open = data.filter((p) => p.status === 'OPEN').length;
      this.stats.inProgress = data.filter((p) => p.status === 'IN_PROGRESS').length;
      this.stats.completed = data.filter((p) => p.status === 'COMPLETED').length;
      this.stats.cancelled = data.filter((p) => p.status === 'CANCELLED').length;
      this.stats.totalBudget = data.reduce((sum, p) => sum + p.budget, 0);
    });
  }

  loadSkillsStats() {
    this.skillService.getAll().subscribe({
      next: (skills) => {
        this.stats.totalSkills = skills.length;
      },
      error: () => {}
    });
  }

  loadCVsStats() {
    this.skillService.getAllCVs().subscribe({
      next: (cvs) => {
        this.stats.totalCVs = cvs.length;
      },
      error: () => {}
    });
  }

  loadPortfoliosStats() {
    this.skillService.getAllPortfolios().subscribe({
      next: (portfolios) => {
        this.stats.totalPortfolios = portfolios.length;
      },
      error: () => {}
    });
  }

  statusClass(status: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      OPEN: 'bg-emerald-100 text-emerald-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  formatStatus(status: ProjectStatus): string {
    return status.replace('_', ' ');
  }
}

