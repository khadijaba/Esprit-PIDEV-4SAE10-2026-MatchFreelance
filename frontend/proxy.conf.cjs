/**
 * Proxy dev — ordre du tableau = priorité (premier contexte qui matche gagne).
 * - /api/users → microservice USER (server.port dans User/application.properties, ex. 8064)
 * - /api/auth → même USER : signin/signup PIDEV, reset mot de passe, vérif email, etc.
 * - /api/admin → même USER : admin PIDEV + /api/admin/v2 (stats, recherche)
 * - /uploads → USER (avatars) en direct si la Gateway n’est pas lancée
 * - /api/formations + /api/inscriptions → Formation (8081) — sans Gateway
 * - /api/examens + /api/certificats → Evaluation (8083)
 * - /api/projects → MS Project (8084) en direct ; sinon Gateway (8050) pour skills, etc.
 *
 * ECONNREFUSED 8050 : lancer la Gateway, ou utiliser les routes directes ci-dessus (USER, Formation, …).
 */
const opts = { secure: false, changeOrigin: true, logLevel: 'warn', ws: false };

module.exports = [
  {
    context: ['/api/users', '/api/users/**'],
    target: 'http://127.0.0.1:8064',
    ...opts,
  },
  {
    context: ['/api/auth', '/api/auth/**'],
    target: 'http://127.0.0.1:8064',
    ...opts,
  },
  {
    context: ['/api/admin', '/api/admin/**'],
    target: 'http://127.0.0.1:8064',
    ...opts,
  },
  {
    context: ['/api/test', '/api/test/**'],
    target: 'http://127.0.0.1:8064',
    ...opts,
  },
  {
    context: ['/oauth2', '/oauth2/**', '/login/oauth2', '/login/oauth2/**'],
    target: 'http://127.0.0.1:8064',
    ...opts,
  },
  {
    context: ['/uploads', '/uploads/**'],
    target: 'http://127.0.0.1:8064',
    ...opts,
  },
  {
    context: ['/api/evaluation-reports', '/api/evaluation-reports/**'],
    target: 'http://127.0.0.1:8090',
    ...opts,
  },
  {
    context: ['/api/modules', '/api/modules/**'],
    target: 'http://127.0.0.1:8081',
    ...opts,
  },
  {
    context: ['/api/formations', '/api/formations/**'],
    target: 'http://127.0.0.1:8081',
    ...opts,
  },
  {
    context: ['/api/inscriptions', '/api/inscriptions/**'],
    target: 'http://127.0.0.1:8081',
    ...opts,
  },
  {
    context: ['/api/examens', '/api/examens/**'],
    target: 'http://127.0.0.1:8083',
    ...opts,
  },
  {
    context: ['/api/certificats', '/api/certificats/**'],
    target: 'http://127.0.0.1:8083',
    ...opts,
  },
  // Team AI (FastAPI) — avant le catch-all /api → Gateway (appel direct en dev si besoin)
  {
    context: ['/api/team-ai', '/api/team-ai/**'],
    target: 'http://127.0.0.1:5000',
    pathRewrite: { '^/api/team-ai': '/api' },
    ...opts,
  },
  // MS Project (8084) : chemins contrôleurs = /projects/** pas /api/projects — retire le préfixe /api.
  {
    context: ['/api/projects', '/api/projects/**'],
    target: 'http://127.0.0.1:8084',
    pathRewrite: { '^/api': '' },
    ...opts,
  },
  {
    context: ['/api'],
    target: 'http://127.0.0.1:8050',
    ...opts,
  },
];
