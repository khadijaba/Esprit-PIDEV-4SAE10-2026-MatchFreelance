import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkillService } from '../../services/skill.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmService } from '../../services/confirm.service';

@Component({
  selector: 'app-cv-supervision',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cv-supervision.component.html',
})
export class CvSupervisionComponent implements OnInit {
  cvs: any[] = [];
  loading = true;

  constructor(
    private skillService: SkillService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit() {
    this.loadCVs();
  }

  loadCVs() {
    this.loading = true;
    this.skillService.getAllCVs().subscribe({
      next: (cvs) => {
        this.cvs = cvs;
        this.loading = false;
      },
      error: () => {
        this.toast.error('Failed to load CVs');
        this.loading = false;
      },
    });
  }

  async deleteCV(freelancerId: number) {
    const ok = await this.confirmService.confirm('Supprimer ce CV ?');
    if (!ok) return;
    this.skillService.deleteCV(freelancerId).subscribe({
      next: () => {
        this.toast.success('CV supprimé');
        this.loadCVs();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
    });
  }
}
