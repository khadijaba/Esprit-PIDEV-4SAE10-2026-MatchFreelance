import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmService } from '../../services/confirm.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (confirmService.message(); as msg) {
      <div class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <div class="mx-4 w-full max-w-md rounded-xl bg-gray-900 px-6 py-5 shadow-xl">
          <p id="confirm-title" class="mb-5 text-white">{{ msg }}</p>
          <div class="flex justify-end gap-3">
            <button
              type="button"
              (click)="confirmService.choose(false)"
              class="rounded-lg px-4 py-2 font-medium text-white bg-gray-700 hover:bg-gray-600"
            >
              Annuler
            </button>
            <button
              type="button"
              (click)="confirmService.choose(true)"
              class="rounded-lg px-4 py-2 font-medium text-gray-900 bg-emerald-400 hover:bg-emerald-300"
            >
              OK
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ConfirmDialogComponent {
  constructor(public confirmService: ConfirmService) {}
}
