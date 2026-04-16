import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-matchfreelance-projects',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './matchfreelance-projects.component.html',
})
export class MatchfreelanceProjectsComponent implements OnInit {
  openProjects: Project[] = [];
  myProjects: Project[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    private projectService: ProjectService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.projectService.getByStatus('OPEN').subscribe({
      next: (list) => {
        this.openProjects = list ?? [];
        this.loading = false;
      },
      error: () => {
        this.error =
          'Impossible de charger les projets. Vérifiez la Gateway (8050) et le microservice Project (8084).';
        this.loading = false;
      },
    });
    const u = this.auth.getStoredUser();
    if (this.auth.isProjectOwner() && u?.userId) {
      this.projectService.getByOwnerId(u.userId).subscribe({
        next: (list) => (this.myProjects = list ?? []),
        error: () => {},
      });
    }
  }
}
