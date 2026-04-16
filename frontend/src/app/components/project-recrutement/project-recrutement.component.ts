import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService } from '../../services/contract.service';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../models/project.model';
import { CandidatureResponse } from '../../models/candidature.model';
import { ContractSummary } from '../../models/contract.model';

@Component({
  selector: 'app-project-recrutement',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './project-recrutement.component.html',
})
export class ProjectRecrutementComponent implements OnInit {
  projectId!: number;
  project: Project | null = null;
  candidatures: CandidatureResponse[] = [];
  contracts: ContractSummary[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private contractService: ContractService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.router.navigate(['/projets']);
      return;
    }
    this.projectId = id;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        const me = this.auth.getStoredUser();
        if (!me?.userId || p.projectOwnerId !== me.userId) {
          this.error = 'Réservé au porteur du projet.';
          this.loading = false;
          return;
        }
        this.loadLists();
        this.loading = false;
      },
      error: () => {
        this.error = 'Projet introuvable.';
        this.loading = false;
      },
    });
  }

  private loadLists(): void {
    this.candidatureService.listByProject(this.projectId).subscribe({
      next: (c) => (this.candidatures = c ?? []),
      error: () => (this.candidatures = []),
    });
    this.contractService.listByProject(this.projectId).subscribe({
      next: (c) => (this.contracts = c ?? []),
      error: () => (this.contracts = []),
    });
  }
}
