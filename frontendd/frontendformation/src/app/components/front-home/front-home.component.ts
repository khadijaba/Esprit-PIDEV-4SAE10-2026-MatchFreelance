import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { FormationService } from '../../services/formation.service';
import { Project } from '../../models/project.model';
import { Formation } from '../../models/formation.model';

@Component({
  selector: 'app-front-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './front-home.component.html',
  styles: [
    `.hero-pattern {
      background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
      inset: 0;
      position: absolute;
    }`,
  ],
})
export class FrontHomeComponent implements OnInit {
  openProjects: Project[] = [];
  loading = true;
  openFormations: Formation[] = [];
  formationsLoading = true;

  constructor(
    private projectService: ProjectService,
    private formationService: FormationService
  ) {}

  ngOnInit() {
    this.projectService.getByStatus('OPEN').subscribe({
      next: (data) => {
        this.openProjects = data.slice(0, 6);
        this.loading = false;
      },
      error: () => (this.loading = false),
    });

    this.formationService.getOuvertes().subscribe({
      next: (data) => {
        this.openFormations = data.slice(0, 6);
        this.formationsLoading = false;
      },
      error: () => (this.formationsLoading = false),
    });
  }
}
