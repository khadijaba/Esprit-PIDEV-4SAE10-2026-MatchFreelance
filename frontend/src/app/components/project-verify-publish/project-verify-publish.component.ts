import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';
import { ProjectEffortEstimate } from '../../models/project-effort.model';
import { ProjectMlRisk } from '../../models/project-ml-risk.model';

@Component({
  selector: 'app-project-verify-publish',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-verify-publish.component.html',
})
export class ProjectVerifyPublishComponent implements OnInit {
  projectId!: number;
  project: Project | null = null;
  effort: ProjectEffortEstimate | null = null;
  risk: ProjectMlRisk | null = null;
  loading = true;
  refreshing = false;
  publishing = false;
  confirmed = false;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/projets']);
      return;
    }
    this.projectId = Number(id);
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.error = null;
    this.projectService.getById(this.projectId).subscribe({
      next: (p) => {
        this.project = p;
        this.refreshIndicators();
        this.loading = false;
      },
      error: () => {
        this.error = 'Projet introuvable ou serveur indisponible.';
        this.loading = false;
      },
    });
  }

  refreshIndicators(): void {
    if (!this.projectId) return;
    this.refreshing = true;
    let n = 0;
    const done = () => {
      if (++n >= 2) this.refreshing = false;
    };
    this.projectService.getEffortEstimate(this.projectId).subscribe({
      next: (e) => (this.effort = e),
      error: () => (this.effort = null),
      complete: done,
    });
    this.projectService.getMlRisk(this.projectId).subscribe({
      next: (r) => (this.risk = r),
      error: () => (this.risk = null),
      complete: done,
    });
  }

  publish(): void {
    if (!this.project || !this.confirmed) return;
    this.publishing = true;
    this.error = null;
    this.projectService
      .update(this.project.id, {
        title: this.project.title,
        description: this.project.description,
        budget: this.project.budget,
        duration: this.project.duration,
        projectOwnerId: this.project.projectOwnerId,
        status: 'OPEN',
        requiredSkills: this.project.requiredSkills ?? [],
      })
      .subscribe({
        next: () => {
          this.publishing = false;
          this.router.navigate(['/projets', this.project!.id]);
        },
        error: (err) => {
          this.publishing = false;
          this.error = err?.error?.message ?? 'Publication impossible.';
        },
      });
  }
}
