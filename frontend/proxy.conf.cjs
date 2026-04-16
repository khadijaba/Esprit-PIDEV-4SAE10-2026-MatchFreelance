/**
 * Proxy dev — tous les endpoints applicatifs passent par la Gateway (8050).
 * Objectif: éviter les appels directs aux microservices depuis le frontend.
 *
 * Exception conservée en local:
 * - /api/team-ai → FastAPI direct (5000) pour les scripts IA.
 */
const opts = { secure: false, changeOrigin: true, logLevel: 'warn', ws: false };

module.exports = [
  // Team AI (FastAPI) — exception locale, avant le catch-all /api -> Gateway.
  {
    context: ['/api/team-ai', '/api/team-ai/**'],
    target: 'http://127.0.0.1:5000',
    pathRewrite: { '^/api/team-ai': '/api' },
    ...opts,
  },
  // OAuth2 + callbacks via Gateway.
  {
    context: ['/oauth2', '/oauth2/**', '/login/oauth2', '/login/oauth2/**'],
    target: 'http://127.0.0.1:8050',
    ...opts,
  },
  // Avatars/fichiers via Gateway.
  {
    context: ['/uploads', '/uploads/**'],
    target: 'http://127.0.0.1:8050',
    ...opts,
  },
  // Toutes les APIs backend via Gateway.
  {
    context: ['/api', '/api/**'],
    target: 'http://127.0.0.1:8050',
    ...opts,
  },
];
