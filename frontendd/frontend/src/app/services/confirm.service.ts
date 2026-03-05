import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  /** Message à afficher ; null = dialog masquée */
  message = signal<string | null>(null);
  private resolve: ((value: boolean) => void) | null = null;

  /**
   * Affiche une boîte de confirmation (sans titre navigateur type "localhost:4200 indique").
   * @returns Promise<true> si OK, Promise<false> si Annuler
   */
  confirm(message: string): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.resolve = resolve;
      this.message.set(message);
    });
  }

  choose(ok: boolean): void {
    if (this.resolve) {
      this.resolve(ok);
      this.resolve = null;
    }
    this.message.set(null);
  }
}
