import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extrait un message lisible depuis une erreur HTTP backend (500, 400, etc.).
 * Ne renvoie jamais l'URL (ex. localhost:4200) : on affiche un message générique à la place.
 */
const FALLBACK_500 = 'Erreur serveur (500). Vérifiez les logs du service User et que la base de données est démarrée.';

export function getBackendErrorMessage(err: unknown, fallback = 'Une erreur est survenue'): string {
  if (!err) return fallback;
  const e = err as HttpErrorResponse;
  if (e?.status === 0) {
    return 'Impossible de joindre le serveur. Vérifiez que la Gateway (port 8086) et le service User sont démarrés, et que vous lancez l\'app avec "ng serve".';
  }
  const body = e?.error;
  if (typeof body === 'string') return body || (e?.status === 500 ? FALLBACK_500 : fallback);
  if (body && typeof body === 'object') {
    const obj = body as Record<string, unknown>;
    if (typeof obj['message'] === 'string') return (obj['message'] as string).trim() || (e?.status === 500 ? FALLBACK_500 : fallback);
    if (typeof obj['error'] === 'string') return obj['error'] as string;
    if (typeof obj['error_description'] === 'string') return obj['error_description'] as string;
    if (Array.isArray(obj['errors']) && (obj['errors'] as unknown[]).length) {
      const first = (obj['errors'] as unknown[])[0];
      const f = first as Record<string, unknown> | string;
      const msg = typeof f === 'string' ? f : String((f as Record<string, unknown>)?.['defaultMessage'] ?? (f as Record<string, unknown>)?.['message'] ?? '');
      return msg.trim() || (e?.status === 500 ? FALLBACK_500 : fallback);
    }
  }
  if (e?.status === 500) return FALLBACK_500;
  const raw = e?.message || '';
  if (raw.includes('localhost') || raw.includes('4200') || /^https?:\/\//i.test(raw)) {
    return fallback;
  }
  return raw || fallback;
}
