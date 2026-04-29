import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ContractService } from '../../services/contract.service';
import { ToastService } from '../../services/toast.service';
import { ContractMessage } from '../../models/contract-message.model';

@Component({
  selector: 'app-contract-chat-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contract-chat-panel.component.html',
})
export class ContractChatPanelComponent implements OnChanges {
  @Input({ required: true }) contractId!: number;
  @Input({ required: true }) currentUserId!: number;
  /** Nom affiché du client ou du freelancer (l’interlocuteur). */
  @Input({ required: true }) peerName!: string;

  messages: ContractMessage[] = [];
  loading = false;
  sending = false;
  draft = '';

  constructor(
    private contractService: ContractService,
    private toast: ToastService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['contractId'] && this.contractId != null) {
      this.load();
    }
  }

  load(): void {
    if (this.contractId == null) return;
    this.loading = true;
    this.contractService.getMessages(this.contractId).subscribe({
      next: (list) => {
        this.messages = list ?? [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Impossible de charger les messages.');
      },
    });
  }

  send(): void {
    const text = this.draft.trim();
    if (!text || this.sending) return;
    this.sending = true;
    this.contractService.sendMessage(this.contractId, this.currentUserId, text).subscribe({
      next: (msg) => {
        this.messages = [...this.messages, msg];
        this.draft = '';
        this.sending = false;
        if (msg.contractProgressPercent != null) {
          this.toast.success(`Progression mise à jour à ${msg.contractProgressPercent}% (détectée dans le message).`);
        }
      },
      error: () => {
        this.sending = false;
        this.toast.error('Envoi impossible.');
      },
    });
  }

  isMine(m: ContractMessage): boolean {
    return m.senderId === this.currentUserId;
  }

  formatTime(iso?: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' }).format(d);
  }
}
