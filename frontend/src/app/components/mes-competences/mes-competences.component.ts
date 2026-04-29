import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/** Le microservice Skill a été retiré : page d’information uniquement. */
@Component({
  selector: 'app-mes-competences',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mes-competences.component.html',
})
export class MesCompetencesComponent {
  constructor(public auth: AuthService) {}
}
