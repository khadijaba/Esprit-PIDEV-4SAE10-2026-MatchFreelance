import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ContractService } from '../../services/contract.service';
import { ToastService } from '../../services/toast.service';
import { Contract, ContractStatus } from '../../models/contract.model';

@Component({
  selector: 'app-contract-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './contract-details.component.html',
})
export class ContractDetailsComponent implements OnInit {
  contract?: Contract;
  loading = true;

  constructor(
    private contractService: ContractService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.contractService.getById(id).subscribe({
      next: (c) => {
        this.contract = c;
        this.loading = false;
      },
      error: () => {
        this.toast.error('Contract not found');
        this.router.navigate(['/admin/contracts']);
      },
    });
  }

  onDelete() {
    if (!this.contract || !confirm('Delete this contract?')) return;
    this.contractService.delete(this.contract.id).subscribe({
      next: () => {
        this.toast.success('Contract deleted');
        this.router.navigate(['/admin/contracts']);
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
