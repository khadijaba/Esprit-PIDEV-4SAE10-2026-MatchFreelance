import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ContractService } from '../../services/contract.service';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../services/toast.service';
import { Contract, ContractStatus } from '../../models/contract.model';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './contract-list.component.html',
})
export class ContractListComponent implements OnInit {
  contracts: Contract[] = [];
  freelancerNames: Record<number, string> = {};
  loading = true;

  constructor(
    private contractService: ContractService,
    private userService: UserService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.contractService.getAll().subscribe({
      next: (data) => {
        this.contracts = data;
        this.loading = false;
        const ids = data.map((c) => c.freelancerId);
        this.userService.getDisplayNamesMap(ids).subscribe({
          next: (map: Record<number, string>) => (this.freelancerNames = { ...this.freelancerNames, ...map }),
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
