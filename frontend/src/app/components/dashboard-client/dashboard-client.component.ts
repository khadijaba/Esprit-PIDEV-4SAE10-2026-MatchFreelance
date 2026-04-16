import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ExamenService } from '../../services/examen.service';
import { User } from '../../models/auth.model';
import { Certificat } from '../../models/examen.model';

@Component({
  selector: 'app-dashboard-client',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-client.component.html',
})
export class DashboardClientComponent implements OnInit {
  freelancers: User[] = [];
  freelancerCertificats: Map<number, Certificat[]> = new Map();
  loading = true;
  error: string | null = null;

  constructor(
    public auth: AuthService,
    private examenService: ExamenService
  ) {}

  ngOnInit() {
    if (!this.auth.isLoggedIn() || !this.auth.isProjectOwner()) {
      this.error = 'Connectez-vous en tant que Client pour accéder à ce tableau de bord.';
      this.loading = false;
      return;
    }
    this.auth.getUsers().subscribe({
      next: (users) => {
        this.freelancers = (users ?? []).filter((u) => u.role === 'FREELANCER');
        this.freelancers.forEach((f) => {
          this.examenService.getCertificatsByFreelancer(f.id).subscribe({
            next: (c) => this.freelancerCertificats.set(f.id, c),
            error: () => {},
          });
        });
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  getCertificatsFor(freelancerId: number): Certificat[] {
    return this.freelancerCertificats.get(freelancerId) ?? [];
  }
}
