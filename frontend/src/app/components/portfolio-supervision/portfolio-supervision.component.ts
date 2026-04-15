import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkillService } from '../../services/skill.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmService } from '../../services/confirm.service';

@Component({
  selector: 'app-portfolio-supervision',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portfolio-supervision.component.html',
})
export class PortfolioSupervisionComponent implements OnInit {
  portfolios: any[] = [];
  loading = true;

  constructor(
    private skillService: SkillService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit() {
    this.loadPortfolios();
  }

  loadPortfolios() {
    this.loading = true;
    this.skillService.getAllPortfolios().subscribe({
      next: (portfolios) => {
        this.portfolios = portfolios;
        this.loading = false;
      },
      error: () => {
        this.toast.error('Failed to load portfolios');
        this.loading = false;
      },
    });
  }

  async deletePortfolio(id: number) {
    const ok = await this.confirmService.confirm('Supprimer ce portfolio ?');
    if (!ok) return;
    this.skillService.deletePortfolio(id).subscribe({
      next: () => {
        this.toast.success('Portfolio supprimé');
        this.loadPortfolios();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
    });
  }
}
