import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SponsorService } from '../../services/sponsor.service';
import { SponsorshipService } from '../../services/sponsorship.service';
import { ToastService } from '../../services/toast.service';
import { Sponsor, Sponsorship, SponsorshipStatus, CreateSponsorshipRequest } from '../../models/sponsor.model';
import { EventService } from '../../services/event.service';
import { Event } from '../../models/event.model';

@Component({
  selector: 'app-sponsor-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './sponsor-detail.component.html',
})
export class SponsorDetailComponent implements OnInit {
  sponsor?: Sponsor;
  events: Event[] = [];
  loading = true;
  showSponsorshipForm = false;

  newSponsorship: CreateSponsorshipRequest = {
    sponsorId: 0,
    eventId: 0,
    contributionType: 'MONETARY',
    amount: 0,
    description: '',
    status: 'PENDING',
  };

  constructor(
    private sponsorService: SponsorService,
    private sponsorshipService: SponsorshipService,
    private eventService: EventService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.loadSponsor(id);
    this.eventService.getAll().subscribe({
      next: (data) => (this.events = data),
    });
  }

  loadSponsor(id: number) {
    this.loading = true;
    this.sponsorService.getById(id).subscribe({
      next: (s) => {
        this.sponsor = s;
        this.loading = false;
      },
      error: () => {
        this.toast.error('Sponsor not found');
        this.router.navigate(['/admin/sponsors']);
      },
    });
  }

  statusClass(status: SponsorshipStatus): string {
    const map: Record<SponsorshipStatus, string> = {
      PENDING: 'bg-amber-100 text-amber-700',
      CONFIRMED: 'bg-blue-100 text-blue-700',
      ACTIVE: 'bg-emerald-100 text-emerald-700',
      COMPLETED: 'bg-gray-100 text-gray-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  openSponsorshipForm() {
    this.newSponsorship = {
      sponsorId: this.sponsor!.id,
      eventId: 0,
      contributionType: 'MONETARY',
      amount: 0,
      description: '',
      status: 'PENDING',
    };
    this.showSponsorshipForm = true;
  }

  closeSponsorshipForm() {
    this.showSponsorshipForm = false;
  }

  saveSponsorship() {
    if (!this.newSponsorship.eventId) {
      this.toast.error('Please select an event');
      return;
    }
    this.sponsorshipService.create(this.newSponsorship).subscribe({
      next: () => {
        this.toast.success('Sponsorship created');
        this.closeSponsorshipForm();
        this.loadSponsor(this.sponsor!.id);
      },
      error: () => this.toast.error('Failed to create sponsorship'),
    });
  }

  confirmSponsorship(id: number) {
    this.sponsorshipService.confirm(id).subscribe({
      next: () => {
        this.toast.success('Sponsorship confirmed');
        this.loadSponsor(this.sponsor!.id);
      },
      error: () => this.toast.error('Failed to confirm'),
    });
  }

  deleteSponsorship(id: number) {
    if (!confirm('Remove this sponsorship?')) return;
    this.sponsorshipService.delete(id).subscribe({
      next: () => {
        this.toast.success('Sponsorship removed');
        this.loadSponsor(this.sponsor!.id);
      },
      error: () => this.toast.error('Failed to delete'),
    });
  }

  deleteSponsor() {
    if (!confirm('Delete this sponsor?')) return;
    this.sponsorService.delete(this.sponsor!.id).subscribe({
      next: () => {
        this.toast.success('Sponsor deleted');
        this.router.navigate(['/admin/sponsors']);
      },
      error: () => this.toast.error('Failed to delete'),
    });
  }

  budgetRemaining(): number {
    if (!this.sponsor) return 0;
    return this.sponsor.totalBudget - this.sponsor.totalSpent;
  }

  budgetPercent(): number {
    if (!this.sponsor || this.sponsor.totalBudget === 0) return 0;
    return Math.round((this.sponsor.totalSpent / this.sponsor.totalBudget) * 100);
  }
}
