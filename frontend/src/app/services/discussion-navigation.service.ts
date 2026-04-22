import { Injectable } from '@angular/core';

/** Permet de passer le nom de l'interlocuteur à la page discussion (évite "Utilisateur #4"). */
@Injectable({ providedIn: 'root' })
export class DiscussionNavigationService {
  private pendingName: string | null = null;

  setPendingName(name: string): void {
    this.pendingName = name;
  }

  getAndClearPendingName(): string | null {
    const name = this.pendingName;
    this.pendingName = null;
    return name;
  }
}
