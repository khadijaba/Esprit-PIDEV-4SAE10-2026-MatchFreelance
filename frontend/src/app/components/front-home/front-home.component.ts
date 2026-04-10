import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-front-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './front-home.component.html',
})
export class FrontHomeComponent implements OnInit {
  openProjects: Project[] = [];
  loading = true;

  constructor(private projectService: ProjectService) {}

  ngOnInit() {
    this.projectService.getByStatus('OPEN').subscribe({
      next: (data) => {
        this.openProjects = data.slice(0, 6);
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }
}
