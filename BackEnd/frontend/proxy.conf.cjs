/**
 * Proxy dev — ordre du tableau = priorité (premier contexte qui matche gagne).
 * - /api/examens + /api/certificats → Evaluation (8083) : évite 404 si Gateway/Eureka ne résout pas lb://EVALUATION
 * - /api → Gateway (8050) pour le reste
 *
 * Si Vite affiche « ECONNREFUSED » sur /api/examens : démarrer le microservice Evaluation (port 8083),
 * ou vérifier le port dans application.properties. Cibles en 127.0.0.1 pour éviter localhost→IPv6 sous Windows.
 */
const opts = { secure: false, changeOrigin: true, logLevel: 'warn', ws: false };

function rewriteProjects(path) {
  return path.replace(/^\/api\/projects/, '/projects');
}

module.exports = [
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
    context: ['/api/projects', '/api/projects/**'],
    target: 'http://127.0.0.1:8084',
    pathRewrite: rewriteProjects,
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
  {
    context: ['/api'],
    target: 'http://127.0.0.1:8050',
    ...opts,
  },
];
