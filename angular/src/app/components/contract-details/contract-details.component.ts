import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ContractService } from '../../services/contract.service';
import { ProjectService } from '../../services/project.service';
import { ChatService } from '../../services/chat.service';
import { ToastService } from '../../services/toast.service';
import { Contract, ContractStatus } from '../../models/contract.model';
import { FinancialSummary, ContractHealth } from '../../models/contract-advanced.model';
import { Message } from '../../models/message.model';

@Component({
  selector: 'app-contract-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './contract-details.component.html',
})
export class ContractDetailsComponent implements OnInit {
  contract?: Contract;
  projectTitle = '';
  messages: Message[] = [];
  loading = true;
  messagesLoading = false;
  financialSummary: FinancialSummary | null = null;
  contractHealth: ContractHealth | null = null;
  loadingFinancial = false;
  loadingHealth = false;
  showFinancial = false;
  showHealth = false;

  constructor(
    private contractService: ContractService,
    private projectService: ProjectService,
    private chatService: ChatService,
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
        this.loadMessages(c.id);
        this.projectService.getById(c.projectId).subscribe({
          next: (p) => (this.projectTitle = p.title),
          error: () => (this.projectTitle = ''),
        });
      },
      error: () => {
        this.toast.error('Contract not found');
        this.router.navigate(['/admin/contracts']);
      },
    });
  }

  loadMessages(contractId: number) {
    this.messagesLoading = true;
    this.chatService.getMessages(contractId).subscribe({
      next: (data) => {
        this.messages = data ?? [];
        this.messagesLoading = false;
      },
      error: (err) => {
        this.messagesLoading = false;
        this.toast.error('Failed to load messages');
      },
    });
  }

  getSenderLabel(msg: Message & { sender_id?: number }): string {
    if (!this.contract) return 'Unknown';
    const senderId = msg.senderId ?? msg.sender_id;
    return senderId === this.contract.clientId
      ? (this.contract.clientName ?? `Client #${senderId}`)
      : (this.contract.freelancerName ?? `Freelancer #${senderId}`);
  }

  isFromClient(msg: Message & { sender_id?: number }): boolean {
    if (!this.contract) return false;
    const senderId = msg.senderId ?? msg.sender_id;
    return senderId === this.contract.clientId;
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

  loadFinancialSummary() {
    if (!this.contract) return;
    this.showFinancial = !this.showFinancial;
    if (this.showFinancial && !this.financialSummary) {
      this.loadingFinancial = true;
      this.contractService.getFinancialSummary(this.contract.id).subscribe({
        next: (data) => {
          this.financialSummary = data;
          this.loadingFinancial = false;
        },
        error: () => {
          this.loadingFinancial = false;
        },
      });
    }
  }

  loadContractHealth() {
    if (!this.contract) return;
    this.showHealth = !this.showHealth;
    if (this.showHealth && !this.contractHealth) {
      this.loadingHealth = true;
      this.contractService.getContractHealth(this.contract.id).subscribe({
        next: (data) => {
          this.contractHealth = data;
          this.loadingHealth = false;
        },
        error: () => {
          this.loadingHealth = false;
        },
      });
    }
  }
}
