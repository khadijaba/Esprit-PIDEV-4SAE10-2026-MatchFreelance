import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SponsorService } from '../../services/sponsor.service';
import { ToastService } from '../../services/toast.service';
import { Sponsor, SponsorType } from '../../models/sponsor.model';

@Component({
  selector: 'app-sponsor-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './sponsor-list.component.html',
})
export class SponsorListComponent implements OnInit {
  sponsors: Sponsor[] = [];
  loading = true;

  constructor(
    private sponsorService: SponsorService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.sponsorService.getAll().subscribe({
      next: (data) => {
        this.sponsors = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load sponsors');
      },
    });
  }

  onDelete(id: number) {
    if (!confirm('Delete this sponsor and all their sponsorships?')) return;
    this.sponsorService.delete(id).subscribe({
      next: () => {
        this.toast.success('Sponsor deleted');
        this.load();
      },
      error: () => this.toast.error('Failed to delete'),
    });
  }

  onToggleActive(id: number) {
    this.sponsorService.toggleActive(id).subscribe({
      next: () => this.load(),
      error: () => this.toast.error('Failed to toggle status'),
    });
  }

  typeClass(type: SponsorType): string {
    const map: Record<SponsorType, string> = {
      COMPANY: 'bg-blue-100 text-blue-700',
      INDIVIDUAL: 'bg-purple-100 text-purple-700',
      ORGANIZATION: 'bg-emerald-100 text-emerald-700',
      STARTUP: 'bg-amber-100 text-amber-700',
    };
    return map[type] ?? 'bg-gray-100 text-gray-700';
  }

  typeIcon(type: SponsorType): string {
    const map: Record<SponsorType, string> = {
      COMPANY: '🏢',
      INDIVIDUAL: '👤',
      ORGANIZATION: '🏛️',
      STARTUP: '🚀',
    };
    return map[type] ?? '💼';
  }
}
