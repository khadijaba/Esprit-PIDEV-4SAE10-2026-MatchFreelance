import { QuestionDto } from '../models/examen.model';

/** Aligné sur le backend (ExamenMetierConstants.MIN_QUESTIONS_PAR_EXAMEN). */
export const MIN_QUESTIONS_PAR_EXAMEN = 7;

export type ParcoursPassage = 'STANDARD' | 'RENFORCEMENT';

/**
 * Même logique que QuestionParcoursFilter.matches côté Evaluation.
 */
export function questionMatchesParcours(
  parcoursInclusion: string | undefined,
  typeParcours: ParcoursPassage
): boolean {
  const inc = (parcoursInclusion ?? 'COMMUN').toString().trim().toUpperCase();
  if (inc === 'COMMUN') {
    return true;
  }
  if (typeParcours === 'STANDARD') {
    return inc === 'STANDARD';
  }
  return inc === 'RENFORCEMENT';
}

export function countQuestionsForParcours(
  questions: QuestionDto[],
  typeParcours: ParcoursPassage
): number {
  if (!questions?.length) {
    return 0;
  }
  return questions.filter((q) => questionMatchesParcours(q.parcoursInclusion as string, typeParcours)).length;
}

/** Nombre de questions « Commun » à ajouter pour que les deux parcours aient au moins le minimum. */
export function communQuestionsToAddForBothParcours(questions: QuestionDto[]): number {
  const cStd = countQuestionsForParcours(questions, 'STANDARD');
  const cRenf = countQuestionsForParcours(questions, 'RENFORCEMENT');
  const dStd = Math.max(0, MIN_QUESTIONS_PAR_EXAMEN - cStd);
  const dRenf = Math.max(0, MIN_QUESTIONS_PAR_EXAMEN - cRenf);
  return Math.max(dStd, dRenf);
}
