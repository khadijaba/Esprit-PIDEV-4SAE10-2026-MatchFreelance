import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { WorkspaceService } from '../../services/workspace.service';
import { ContractService } from '../../services/contract.service';
import { ToastService } from '../../services/toast.service';
import { CreateWorkspaceRequest, WorkspaceAccessLevel } from '../../models/workspace.model';
import { Contract } from '../../models/contract.model';

@Component({
  selector: 'app-workspace-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './workspace-form.component.html',
})
export class WorkspaceFormComponent implements OnInit {
  form: CreateWorkspaceRequest = {
    name: '',
    description: '',
    accessLevel: 'PRIVATE',
    contractId: 0,
    ownerId: 1,
  };

  contracts: Contract[] = [];
  isEdit = false;
  workspaceId?: number;
  loading = false;

  constructor(
    private workspaceService: WorkspaceService,
    private contractService: ContractService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.contractService.getAll().subscribe({
      next: (data) => (this.contracts = data),
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEdit = true;
      this.workspaceId = +id;
      this.workspaceService.getById(this.workspaceId).subscribe({
        next: (w) => {
          this.form = {
            name: w.name,
            description: w.description,
            accessLevel: w.accessLevel,
            contractId: w.contractId,
            ownerId: w.ownerId,
          };
        },
        error: () => {
          this.toast.error('Workspace not found');
          this.router.navigate(['/admin/workspaces']);
        },
      });
    }
  }

  onSubmit() {
    if (!this.form.name.trim()) {
      this.toast.error('Name is required');
      return;
    }
    if (!this.form.contractId) {
      this.toast.error('Please select a contract');
      return;
    }

    this.loading = true;
    const obs = this.isEdit
      ? this.workspaceService.update(this.workspaceId!, this.form)
      : this.workspaceService.create(this.form);

    obs.subscribe({
      next: (w) => {
        this.toast.success(this.isEdit ? 'Workspace updated' : 'Workspace created');
        this.router.navigate(['/admin/workspaces', w.id]);
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err.error?.message || 'Failed to save workspace');
      },
    });
  }
}
