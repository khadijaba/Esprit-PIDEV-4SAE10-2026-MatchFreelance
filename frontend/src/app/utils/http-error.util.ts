import { HttpErrorResponse } from '@angular/common/http';

function isNonEmptyText(v: unknown): v is string {
  return typeof v === 'string' && v.trim().length > 0 && v.trim() !== 'null' && v.trim() !== 'undefined';
}

/** Réponses Spring Boot « whitelabel » peu utiles seules pour l’utilisateur */
function isGenericWhitelabelText(s: string): boolean {
  const t = s.trim().toLowerCase();
  return (
    t === 'not found' ||
    t === 'bad request' ||
    t === 'unauthorized' ||
    t === 'forbidden' ||
    t === 'internal server error' ||
    t === 'service unavailable'
  );
}

function springPathHint(o: Record<string, unknown>): string | null {
  const p = o['path'];
  if (typeof p === 'string' && p.trim()) {
    return p.trim();
  }
  return null;
}

function pickFromObject(o: Record<string, unknown>): string | null {
  for (const key of ['message', 'detail', 'title']) {
    const v = o[key];
    if (isNonEmptyText(v)) {
      const t = v.trim();
      if (!isGenericWhitelabelText(t)) {
        return t;
      }
    }
    if (v !== null && typeof v === 'object' && !Array.isArray(v)) {
      const nested = pickFromObject(v as Record<string, unknown>);
      if (nested) {
        return nested;
      }
    }
  }
  const errField = o['error'];
  if (isNonEmptyText(errField)) {
    const t = errField.trim();
    if (!isGenericWhitelabelText(t)) {
      return t;
    }
  }
  const errors = o['errors'];
  if (Array.isArray(errors) && errors.length > 0) {
    const first = errors[0];
    if (isNonEmptyText(first)) {
      return first.trim();
    }
    if (first !== null && typeof first === 'object' && !Array.isArray(first)) {
      const fe = first as Record<string, unknown>;
      for (const k of ['defaultMessage', 'message']) {
        const x = fe[k];
        if (isNonEmptyText(x)) {
          return x.trim();
        }
      }
    }
  }
  return null;
}

function hintForStatus(status: number, bodyObj: Record<string, unknown> | null, fallback: string): string {
  const path = bodyObj ? springPathHint(bodyObj) : null;
  if (status === 404) {
    const pathLower = (path ?? '').toLowerCase();
    let backendHint =
      'Vérifiez que le microservice ciblé est démarré, enregistré sur Eureka, et que la route API Gateway est bien configurée.';
    if (pathLower.startsWith('/projects')) {
      backendHint =
        'Vérifiez que le microservice Project est redémarré avec les nouveaux endpoints de supervision et que la Gateway route /api/projects/** vers PROJECT.';
    } else if (pathLower.startsWith('/candidatures')) {
      backendHint =
        'Vérifiez que le microservice Candidature est démarré, enregistré sur Eureka, et que l’API Gateway inclut les routes /api/candidatures.';
    } else if (pathLower.startsWith('/interviews')) {
      backendHint =
        'Vérifiez que le microservice Interview est démarré, enregistré sur Eureka, et que l’API Gateway inclut les routes /api/interviews.';
    } else if (pathLower.startsWith('/users')) {
      backendHint =
        'Vérifiez que le microservice User (8085) et l’API Gateway (8086) sont démarrés, enregistrés sur Eureka, et que MySQL contient la base « user ».';
    } else if (pathLower.includes('/api/ai/') || pathLower.includes('analyze-description') || pathLower.includes('description-coach')) {
      backendHint =
        'Team AI (Python) : vérifiez uvicorn sur le port 5000, la propriété team.ai.url sur la gateway, et que le code expose POST /api/analyze-description (ou /api/description-coach). Les routes Eureka ne s’appliquent pas à ce service.';
    }
    return (
      `${fallback} — Ressource introuvable (404).` +
      (path ? ` Chemin : ${path}.` : '') +
      ` ${backendHint}`
    );
  }
  if (status === 503) {
    return `${fallback} — Service indisponible (503). Aucune instance Eureka pour ce microservice.`;
  }
  return `${fallback} (HTTP ${status})`;
}

/**
 * Extrait un message lisible depuis une erreur HTTP ou une Error (évite « null » et « Not Found » seuls).
 */
export function httpErrorMessage(err: unknown, fallback: string): string {
  if (!(err instanceof HttpErrorResponse)) {
    if (err instanceof Error) {
      const m = err.message?.trim();
      if (m && m !== 'null' && m !== 'undefined' && !isGenericWhitelabelText(m)) {
        return `${fallback} — ${m}`;
      }
      return fallback;
    }
    return fallback;
  }

  if (err.status === 0) {
    return 'Impossible de joindre le serveur (réseau, proxy ou CORS).';
  }

  const body = err.error;
  let bodyObj: Record<string, unknown> | null = null;

  if (body !== null && typeof body === 'object' && !Array.isArray(body)) {
    bodyObj = body as Record<string, unknown>;
  } else if (typeof body === 'string') {
    const raw = body.trim();
    if (isNonEmptyText(raw) && !isGenericWhitelabelText(raw)) {
      return raw;
    }
    try {
      const parsed = JSON.parse(raw) as unknown;
      if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) {
        bodyObj = parsed as Record<string, unknown>;
      }
    } catch {
      /* ignore */
    }
  }

  if (bodyObj) {
    const p = pickFromObject(bodyObj);
    if (p && !isGenericWhitelabelText(p)) {
      return p;
    }
  }

  if (err.status === 404 || err.status === 503) {
    return hintForStatus(err.status, bodyObj, fallback);
  }

  if (body == null || body === '') {
    const st = err.statusText?.trim();
    if (st && st !== 'Unknown Error' && !isGenericWhitelabelText(st)) {
      return `${fallback} (${err.status} ${st})`;
    }
  }

  if (typeof err.message === 'string' && err.message.trim()) {
    return `${fallback} — ${err.message.trim()}`;
  }

  return `${fallback} (HTTP ${err.status})`;
}

/** Alias pour les composants qui utilisaient ce nom. */
export const getBackendErrorMessage = httpErrorMessage;
