/**
 * Configuration front plateforme MatchFreelance (validation).
 * L’app « blog / forum » (projet Esprit-PIDEV…-MatchFreelance-blog) tourne en général sur un autre port.
 */

/** Base des appels API hors localhost : Gateway Render (même valeur que dans vercel.json). */
const PROD_API_ORIGIN = 'https://matchfreelance-gateway.onrender.com';

/**
 * Préfixe absolu pour les URLs `/api/...` (intercepteur).
 * Vide sur localhost → proxy dev ou ng serve inchangé.
 */
export function apiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return '';
  }
  const w = window as Window & { __MF_API_ORIGIN__?: string };
  if (typeof w.__MF_API_ORIGIN__ === 'string' && w.__MF_API_ORIGIN__.trim().length > 0) {
    return w.__MF_API_ORIGIN__.trim().replace(/\/$/, '');
  }
  const h = w.location.hostname;
  if (h === 'localhost' || h === '127.0.0.1') {
    return '';
  }
  return PROD_API_ORIGIN;
}

/** Préfixe `/api/...` avec la Gateway en prod (Vercel évite ainsi le 405 du statique). */
export function publicApiUrl(path: string): string {
  if (!path.startsWith('/')) {
    return path;
  }
  const b = apiBaseUrl();
  return b ? b + path : path;
}

export const environment = {
  production: false,

  /**
   * Origine relative pour rester sur le meme host/port que validation (4200 en dev).
   * Le dev-server proxifie /blog/** vers le frontend blog (port 4201).
   */
  blogForumOrigin: '',

  /** Chemin du forum proxifie via Gateway. */
  blogForumPath: '/blog/threads',
};
