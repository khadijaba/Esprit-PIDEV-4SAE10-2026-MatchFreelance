import { AnalyzeProjectResponse } from '../services/team-ai.service';

export interface DescriptionCoachingResult {
  /** Titre court pour le bandeau (ex. règles vs LLM). */
  headline: string;
  pointsToReinforce: string[];
  checklist: string[];
  clarificationQuestions: string[];
  /** Texte du brouillon éditable (aperçu). */
  draftDescription: string;
  analysisSource: 'rules' | 'llm' | 'unknown';
}

const CHECKLIST_ITEMS = [
  'Objectif métier clair (pour qui, quel problème).',
  'Liste des livrables attendus (code, maquettes, doc, scripts…).',
  'Critères d’acceptation (« c’est fini quand… »).',
  'Contraintes techniques (stack, hébergement, RGPD, perf…).',
  'Jalons ou fenêtre de livraison approximative.',
];

function hasAny(text: string, patterns: RegExp[]): boolean {
  return patterns.some((re) => re.test(text));
}

/**
 * Construit le retour « coaching » affiché après l’analyse Team AI (complément côté front, règles lisibles).
 */
export function buildDescriptionCoaching(
  title: string,
  description: string,
  analysis: AnalyzeProjectResponse | null,
  budgetTnd: number,
  durationDays: number
): DescriptionCoachingResult {
  const t = (title || '').trim();
  const d = (description || '').trim();
  const lower = `${t}\n${d}`.toLowerCase();

  const points: string[] = [];
  if (d.length < 80) {
    points.push('La description est très courte : détaillez le périmètre et les livrables.');
  }
  if (!hasAny(lower, [/\blivrable/, /deliverable/, /code source/, /mockup|maquette/, /documentation/])) {
    points.push(
      'Précisez les livrables concrets (ex. dépôt Git, maquettes Figma, manuel utilisateur, scripts Docker).'
    );
  }
  if (!hasAny(lower, [/critère|acceptation|definition of done|c’est fini|cest fini|done when/i])) {
    points.push('Ajoutez des critères d’acceptation (« c’est terminé lorsque… »).');
  }
  if (!hasAny(lower, [/périmètre|hors scope|exclu|in scope|scope/])) {
    points.push('Indiquez les frontières du projet (ce qui est inclus / exclu).');
  }
  if (!hasAny(lower, [/valid|validation|client|product owner|recette/])) {
    points.push('Précisez qui valide les livrables et comment (recette, démo, UAT).');
  }
  if (!hasAny(lower, [/prod|production|staging|recette|environnement|dev\b/])) {
    points.push('Mentionnez les environnements cibles (dev, recette, production) si pertinent.');
  }

  const questions: string[] = [];
  if (t) {
    questions.push(`Quels livrables exacts attendez-vous pour « ${t.slice(0, 80)}${t.length > 80 ? '…' : ''} » ?`);
  }
  questions.push('Quelles contraintes techniques ou légales (RGPD, sécurité, SLA) doivent être respectées ?');
  questions.push('Quelle est la fenêtre de livraison réaliste (date butoir ou jalons) ?');

  const src = analysis?.analysisSource === 'llm' ? 'llm' : analysis?.analysisSource === 'rules' ? 'rules' : 'unknown';
  const n = points.length;
  const headline =
    src === 'llm'
      ? `${n} point(s) à renforcer détecté(s). Brouillon enrichi via LLM (éditable).`
      : `${n} point(s) à renforcer détecté(s). Brouillon généré par règles (éditable).`;

  const draft = buildDraftParagraph(t, d, analysis, budgetTnd, durationDays);

  return {
    headline,
    pointsToReinforce: points,
    checklist: [...CHECKLIST_ITEMS],
    clarificationQuestions: questions,
    draftDescription: draft,
    analysisSource: src,
  };
}

function buildDraftParagraph(
  title: string,
  description: string,
  analysis: AnalyzeProjectResponse | null,
  budgetTnd: number,
  durationDays: number
): string {
  const roles = analysis?.roles?.length ? analysis.roles.join(', ') : 'un profil technique adapté au périmètre';
  const skills = analysis?.requiredSkills?.length ? analysis.requiredSkills.join(', ') : 'les compétences listées dans l’annonce';
  const summary = (analysis?.summary || '').trim();
  const complexity = analysis?.complexity || 'medium';
  const leader = analysis?.technicalLeaderRole ? `Rôle de référence : ${analysis.technicalLeaderRole}.` : '';
  const be = analysis?.budgetEstimate;
  let budgetLine = '';
  if (be && typeof be.minAmount === 'number' && typeof be.maxAmount === 'number') {
    const cur = (be.currency || 'TND').toUpperCase();
    budgetLine = `Fourchette budgétaire estimée par l’analyse : ${Math.round(be.minAmount)} – ${Math.round(be.maxAmount)} ${cur} (à valider). `;
  } else if (budgetTnd > 0) {
    budgetLine = `Budget indicatif porteur : ${budgetTnd} TND. `;
  }
  const dur = durationDays > 0 ? durationDays : analysis?.durationEstimateDays ?? 30;

  const intro = title
    ? `Projet : ${title}.\n\n`
    : '';
  const ctx = description
    ? `Contexte actuel (à compléter) :\n${description.slice(0, 1200)}${description.length > 1200 ? '\n…' : ''}\n\n`
    : '';

  return (
    `${intro}${ctx}` +
    `Nous recherchons ${roles} pour réaliser ce projet (complexité estimée : ${complexity}). ` +
    `${leader}\n\n` +
    `Compétences techniques attendues : ${skills}.\n` +
    `${budgetLine}` +
    `Durée indicative : ${dur} jours calendaires.\n\n` +
    (summary ? `Synthèse analyse : ${summary}\n\n` : '') +
    `Livrables à préciser : code source versionné, documentation minimale, plan de déploiement ou conteneurisation si applicable.\n` +
    `Critères d’acceptation : à définir avec le porteur (démo, recette, jeux d’essai).\n` +
    `Environnements : préciser dev / recette / production et les accès nécessaires.`
  );
}
