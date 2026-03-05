import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CareerPathService } from '../../services/career-path.service';
import { CareerPathRecommendation } from '../../models/career-path.model';

@Component({
  selector: 'app-career-path',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './career-path.component.html',
})
export class CareerPathComponent implements OnInit {
  loading = true;
  error = '';
  recommendation: CareerPathRecommendation | null = null;

  constructor(
    private auth: AuthService,
    private careerPath: CareerPathService
  ) {}

  ngOnInit() {
    const user = this.auth.getCurrentUser();
    const userId = user?.id != null ? Number(user.id) : null;
    if (!userId) {
      this.error = 'Session invalide. Veuillez vous reconnecter.';
      this.loading = false;
      return;
    }
    this.careerPath.getRecommendations(userId).subscribe({
      next: (rec) => {
        this.recommendation = rec;
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les recommandations. Vérifiez que les services Projets et Formations sont accessibles.';
        this.loading = false;
      },
    });
  }
}
