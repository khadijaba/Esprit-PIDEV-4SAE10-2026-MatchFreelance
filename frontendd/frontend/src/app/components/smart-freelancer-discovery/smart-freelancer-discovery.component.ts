import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TeamAiService } from '../../services/team-ai.service';
import { TalentMatchingService } from '../../services/talent-matching.service';
import { ToastService } from '../../services/toast.service';
import { MatchingFreelancer } from '../../models/matching.model';
import { Project } from '../../models/project.model';
import { extractRequiredSkillsFromDescription } from '../../utils/project-skill.util';

const DISCOVERY_LIMIT = 20;

@Component({
  selector: 'app-smart-freelancer-discovery',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './smart-freelancer-discovery.component.html',
})
export class SmartFreelancerDiscoveryComponent {
  description = '';
  extractedSkills: string[] = [];
  freelancers: MatchingFreelancer[] = [];
  loading = false;
  searched = false;

  constructor(
    private teamAi: TeamAiService,
    private matching: TalentMatchingService,
    private toast: ToastService
  ) {}

  discover(): void {
    const text = (this.description && this.description.trim()) || '';
    if (!text) {
      this.toast.show('Décrivez votre projet pour lancer la recherche.');
      return;
    }

    this.loading = true;
    this.searched = true;
    this.extractedSkills = [];
    this.freelancers = [];

    const firstLine = text.split(/\n/)[0];
    const title = (firstLine && firstLine.trim().slice(0, 100)) || 'Recherche';

    this.teamAi.analyzeProject({ title, description: text }).subscribe({
      next: (res) => {
        let skills = (res.requiredSkills || []).map((s) => String(s).trim()).filter(Boolean);
        if (skills.length === 0) {
          skills = extractRequiredSkillsFromDescription(text);
        }
        this.extractedSkills = [...new Set(skills)];
        this.runMatching(skills);
      },
      error: () => {
        this.extractedSkills = extractRequiredSkillsFromDescription(text);
        this.runMatching(this.extractedSkills);
      },
    });
  }

  private runMatching(requiredSkills: string[]): void {
    if (requiredSkills.length === 0) {
      this.loading = false;
      this.toast.show('Aucune compétence détectée. Précisez des technologies (ex. Spring Boot, Docker, Java).');
      return;
    }

    const project: Project = {
      id: 0,
      title: 'Discovery',
      description: this.description,
      budget: 0,
      duration: 0,
      createdAt: '',
      status: 'OPEN',
      projectOwnerId: 0,
      requiredSkills,
    };

    this.matching.getTopMatchingFreelancers(project, DISCOVERY_LIMIT).subscribe({
      next: (list) => {
        this.freelancers = list;
        this.loading = false;
        if (list.length === 0) {
          this.toast.show('Aucun freelancer ne correspond aux compétences extraites pour l\'instant.');
        } else {
          this.toast.success(list.length + ' freelancer(s) trouvé(s).');
        }
      },
      error: () => {
        this.loading = false;
        this.toast.error('Erreur lors de la recherche des freelancers.');
      },
    });
  }
}
