import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  toasts = signal<Toast[]>([]);
  private nextId = 0;
  private hideTimeout = 4000;

  show(message: string, type: ToastType = 'info') {
    const id = ++this.nextId;
    const toast: Toast = { id, message, type };
    this.toasts.update((t) => [...t, toast]);
    setTimeout(() => this.dismiss(id), this.hideTimeout);
  }

  success(message: string) {
    this.show(message, 'success');
  }

  error(message: string) {
    this.show(message, 'error');
  }

  dismiss(id: number) {
    this.toasts.update((t) => t.filter((x) => x.id !== id));
  }
}
