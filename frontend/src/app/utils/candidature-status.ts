import { CandidatureResponse } from '../models/candidature.model';

/** Compare le statut renvoyé par l’API (casse / type) de façon tolérante. */
export function isCandidaturePending(status: string | null | undefined): boolean {
  if (status == null) return false;
  return String(status).trim().toUpperCase() === 'PENDING';
}

/** Acceptation côté client : backend exige eligibleForAcceptance (entretien COMPLETED). */
export function canOwnerAcceptCandidature(c: CandidatureResponse): boolean {
  return isCandidaturePending(c.status) && c.eligibleForAcceptance === true;
}

export function canOwnerRejectCandidature(c: CandidatureResponse): boolean {
  return isCandidaturePending(c.status);
}
