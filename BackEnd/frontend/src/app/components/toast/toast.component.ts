import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      @for (t of toastService.toasts(); track t.id) {
        <div
          role="alert"
          class="flex min-w-[280px] items-center gap-3 rounded-lg border px-4 py-3 shadow-lg"
          [ngClass]="{
            'border-emerald-200 bg-emerald-50 text-emerald-800': t.type === 'success',
            'border-red-200 bg-red-50 text-red-800': t.type === 'error',
            'border-blue-200 bg-blue-50 text-blue-800': t.type === 'info'
          }"
        >
          @if (t.type === 'success') {
            <svg class="h-5 w-5 shrink-0 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          }
          @if (t.type === 'error') {
            <svg class="h-5 w-5 shrink-0 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
          <span class="flex-1 text-sm font-medium">{{ t.message }}</span>
          <button
            (click)="toastService.dismiss(t.id)"
            class="ml-2 rounded p-1 text-current opacity-70 hover:opacity-100"
            aria-label="Dismiss"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastComponent {
  constructor(public toastService: ToastService) {}
}
