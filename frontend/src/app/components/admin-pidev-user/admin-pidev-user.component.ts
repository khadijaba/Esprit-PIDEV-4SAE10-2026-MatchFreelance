import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PidevUserService, PidevUserRow, UserStats } from '../../services/pidev-user.service';

@Component({
  selector: 'app-admin-pidev-user',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-pidev-user.component.html',
})
export class AdminPidevUserComponent implements OnInit {
  stats: UserStats | null = null;
  statsError = '';
  searchTerm = '';
  searchResults: PidevUserRow[] = [];
  searchLoading = false;
  searchError = '';

  constructor(private pidevUser: PidevUserService) {}

  ngOnInit(): void {
    this.pidevUser.getStatistics().subscribe({
      next: (s) => {
        this.stats = s;
        this.statsError = '';
      },
      error: () => {
        this.stats = null;
        this.statsError = 'Impossible de charger les statistiques. Vérifiez que le microservice User est démarré et que vous êtes admin.';
      },
    });
  }

  runSearch(): void {
    const t = this.searchTerm.trim();
    if (t.length < 2) {
      this.searchError = 'Saisissez au moins 2 caractères.';
      this.searchResults = [];
      return;
    }
    this.searchError = '';
    this.searchLoading = true;
    this.pidevUser.searchSimple(t).subscribe({
      next: (rows) => {
        this.searchResults = rows ?? [];
        this.searchLoading = false;
      },
      error: () => {
        this.searchLoading = false;
        this.searchResults = [];
        this.searchError = 'Recherche impossible (réseau ou droits).';
      },
    });
  }
}
