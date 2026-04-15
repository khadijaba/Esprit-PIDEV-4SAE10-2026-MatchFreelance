import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ContractService } from '../../../services/contract.service';
import { ProjectService } from '../../../services/project.service';
import { ToastService } from '../../../services/toast.service';
import { Contract, ContractStatus } from '../../../models/contract.model';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './contract-list.component.html',
})
export class ContractListComponent implements OnInit {
  contracts: Contract[] = [];
  projectTitles: Record<number, string> = {};
  loading = true;

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
    this.contractService.getAll().subscribe({
      next: (data) => {
        // Filter out cancelled contracts
        this.contracts = data.filter((c) => c.status !== 'CANCELLED');
        const projectIds = [...new Set(this.contracts.map((c) => c.projectId))];
        if (projectIds.length === 0) {
          this.loading = false;
          return;
        }
        forkJoin(projectIds.map((id) => this.projectService.getById(id))).subscribe({
          next: (projects) => {
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

  onDelete(id: number) {
    if (!confirm('Delete this contract?')) return;
    this.contractService.delete(id).subscribe({
      next: () => {
        this.toast.success('Contract deleted');
        this.load();
      },
      error: () => this.toast.error('Failed to delete'),
    });
  }

  statusClass(status: ContractStatus): string {
    const map: Record<ContractStatus, string> = {
      DRAFT: 'bg-gray-100 text-gray-700',
      ACTIVE: 'bg-emerald-100 text-emerald-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  formatStatus(status: ContractStatus): string {
    return status.replace('_', ' ');
  }
}
