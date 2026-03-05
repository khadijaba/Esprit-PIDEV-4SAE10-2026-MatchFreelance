import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ContractService } from '../../services/contract.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Contract, ContractStatus } from '../../models/contract.model';

@Component({
  selector: 'app-freelancer-contract-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './freelancer-contract-list.component.html',
})
export class FreelancerContractListComponent implements OnInit {
  contracts: Contract[] = [];
  projectTitles: Record<number, string> = {};
  loading = true;
  auth = inject(AuthService);
  get freelancerId(): number {
    return this.auth.currentUserId() ?? 2;
  }

  constructor(
    private contractService: ContractService,
    private projectService: ProjectService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.contractService.getByFreelancerId(this.freelancerId).subscribe({
      next: (data) => {
        this.contracts = data;
        const projectIds = [...new Set(data.map((c) => c.projectId))];
        if (projectIds.length === 0) {
          this.loading = false;
          return;
        }
        forkJoin(projectIds.map((id) => this.projectService.getById(id))).subscribe({
          next: (projects) => {
            this.projectTitles = {};
            projects.forEach((p, i) => (this.projectTitles[projectIds[i]] = p.title));
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          },
        });
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load contracts');
      },
    });
  }

  statusClass(status: ContractStatus): string {
    const map: Record<ContractStatus, string> = {
      DRAFT: 'bg-gray-100 text-gray-700',
      ACTIVE: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  formatStatus(status: ContractStatus): string {
    return status.replace('_', ' ');
  }
}
