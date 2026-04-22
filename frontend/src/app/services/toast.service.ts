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

  /** Normalise le texte pour éviter d'afficher "null" / chaîne vide dans l'UI. */
  private static normalizeMessage(message: string | null | undefined, type: ToastType): string {
    const raw = message == null ? '' : String(message).trim();
    if (raw === '' || raw === 'null' || raw === 'undefined') {
      return type === 'error' ? 'Une erreur est survenue.' : type === 'success' ? 'OK' : 'Information';
    }
    return raw;
  }

  show(message: string | null | undefined, type: ToastType = 'info') {
    const id = ++this.nextId;
    const text = ToastService.normalizeMessage(message, type);
    const toast: Toast = { id, message: text, type };
    this.toasts.update((t) => [...t, toast]);
    setTimeout(() => this.dismiss(id), this.hideTimeout);
  }

  success(message: string | null | undefined) {
    this.show(message, 'success');
  }

  error(message: string | null | undefined) {
    this.show(message, 'error');
  }

  dismiss(id: number) {
    this.toasts.update((t) => t.filter((x) => x.id !== id));
  }
}
