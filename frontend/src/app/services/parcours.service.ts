import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { FormationProposeeDto, ParcoursIntelligentResponse, SkillDto } from '../models/parcours.model';
import { Skill } from '../models/skill.model';
import { TypeFormation } from '../models/formation.model';
import { SkillService } from './skill.service';
import { FormationService } from './formation.service';

/** Domaines de formation utilisés pour les gaps (alignés sur {@link TypeFormation}). */
const DOMAINES_FORMATION: TypeFormation[] = [
  'WEB_DEVELOPMENT',
  'DEVOPS',
  'CYBERSECURITY',
  'DESIGN',
  'MOBILE_DEVELOPMENT',
  'AI',
  'DATA_SCIENCE',
];

function toSkillDto(s: Skill): SkillDto {
  return {
    id: s.id,
    name: s.name,
    category: String(s.category),
    freelancerId: s.freelancerId,
    level: s.level,
    yearsOfExperience: s.yearsOfExperience,
  };
}

/** Domaines couverts par les catégories de compétences enregistrées (normalisation uppercase). */
function domainesCouvertsParCategories(categories: string[]): Set<string> {
  const covered = new Set<string>();
  for (const c of categories) {
    const u = String(c).trim().toUpperCase();
    if (!u) continue;
    covered.add(u);
    if (u === 'DATA_SCIENCE' || u === 'AI') {
      covered.add('DATA_SCIENCE');
      covered.add('AI');
    }
  }
  return covered;
}

function gapsDepuisCategories(categories: string[]): string[] {
  const covered = domainesCouvertsParCategories(categories);
  return DOMAINES_FORMATION.filter((d) => !covered.has(d));
}

@Injectable({ providedIn: 'root' })
export class ParcoursService {
  constructor(
    private skillService: SkillService,
    private formationService: FormationService
  ) {}

  /**
   * Charge les compétences du freelancer (microservice Skill via /api/skills/freelancer/:id),
   * calcule les gaps par rapport aux domaines de formation, puis propose des formations ouvertes.
   * Si le service Skill est indisponible, retombe sur le mode catalogue (analyseCompetencesDisponible: false).
   */
  getParcoursIntelligent(freelancerId: number): Observable<ParcoursIntelligentResponse> {
    return this.skillService.getByFreelancerId(freelancerId).pipe(
      map((skills) => ({ ok: true as const, skills })),
      catchError(() => of({ ok: false as const, skills: [] as Skill[] })),
      switchMap(({ ok, skills }) => {
        if (!ok) {
          return of(this.reponseModeCatalogue(freelancerId));
        }
        const competencesActuelles = skills.map(toSkillDto);
        const categoriesActuelles = [...new Set(skills.map((s) => String(s.category).trim()).filter(Boolean))];
        const gapsDetectes = gapsDepuisCategories(categoriesActuelles);
        if (gapsDetectes.length === 0) {
          return of({
            freelancerId,
            competencesActuelles,
            categoriesActuelles,
            gapsDetectes: [],
            formationsProposees: [],
            analyseCompetencesDisponible: true,
          });
        }
        return this.formationService.getOuvertes().pipe(
          map((formations) => {
            const gapSet = new Set(gapsDetectes);
            const proposees: FormationProposeeDto[] = formations
              .filter((f) => f.statut === 'OUVERTE' && f.typeFormation && gapSet.has(f.typeFormation as TypeFormation))
              .slice(0, 16)
              .map((f) => ({
                id: f.id,
                titre: f.titre,
                typeFormation: f.typeFormation ?? '',
                description: f.description ?? undefined,
                dureeHeures: f.dureeHeures,
                statut: f.statut,
              }));
            return {
              freelancerId,
              competencesActuelles,
              categoriesActuelles,
              gapsDetectes,
              formationsProposees: proposees,
              analyseCompetencesDisponible: true,
            };
          }),
          catchError(() =>
            of({
              freelancerId,
              competencesActuelles,
              categoriesActuelles,
              gapsDetectes,
              formationsProposees: [],
              analyseCompetencesDisponible: true,
            })
          )
        );
      })
    );
  }

  private reponseModeCatalogue(freelancerId: number): ParcoursIntelligentResponse {
    return {
      freelancerId,
      competencesActuelles: [],
      categoriesActuelles: [],
      gapsDetectes: [],
      formationsProposees: [],
      analyseCompetencesDisponible: false,
    };
  }
}
