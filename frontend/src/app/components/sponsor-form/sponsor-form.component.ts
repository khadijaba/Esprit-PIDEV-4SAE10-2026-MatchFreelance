import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SponsorService } from '../../services/sponsor.service';
import { ToastService } from '../../services/toast.service';
import { CreateSponsorRequest } from '../../models/sponsor.model';

@Component({
  selector: 'app-sponsor-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './sponsor-form.component.html',
})
export class SponsorFormComponent implements OnInit {
  form: CreateSponsorRequest = {
    name: '',
    description: '',
    website: '',
    email: '',
    phone: '',
    type: 'COMPANY',
    totalBudget: 0,
  };

  isEdit = false;
  sponsorId?: number;
  loading = false;

  constructor(
    private sponsorService: SponsorService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEdit = true;
      this.sponsorId = +id;
      this.sponsorService.getById(this.sponsorId).subscribe({
        next: (s) => {
          this.form = {
            name: s.name,
            description: s.description,
            website: s.website || '',
            email: s.email || '',
            phone: s.phone || '',
            type: s.type,
            totalBudget: s.totalBudget,
          };
        },
        error: () => {
          this.toast.error('Sponsor not found');
          this.router.navigate(['/admin/sponsors']);
        },
      });
    }
  }

  onSubmit() {
    if (!this.form.name?.trim()) {
      this.toast.error('Name is required');
      return;
    }

    this.loading = true;
    const obs = this.isEdit
      ? this.sponsorService.update(this.sponsorId!, this.form)
      : this.sponsorService.create(this.form);

    obs.subscribe({
      next: (s) => {
        this.toast.success(this.isEdit ? 'Sponsor updated' : 'Sponsor created');
        this.router.navigate(['/admin/sponsors', s.id]);
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err.error?.message || 'Failed to save sponsor');
      },
    });
  }
}
