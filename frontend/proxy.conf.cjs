/**
 * Proxy dev (ordre : chemins les plus spécifiques d'abord) :
 * - /api/evaluation-reports → microservice Python (8090), évite 404 si la Gateway n’a pas encore la route
 * - /api/modules → Formation (8081)
 * - /api/projects → Project (8084), rewrite /api/projects -> /projects
 * - reste /api → Gateway (8050)
 */
const opts = { secure: false, changeOrigin: true, logLevel: 'debug', ws: false };

function rewriteProjects(path) {
  return path.replace(/^\/api\/projects/, '/projects');
}

module.exports = {
  '/api/evaluation-reports': {
    target: 'http://localhost:8090',
    ...opts,
  },
  '/api/modules': {
    target: 'http://localhost:8081',
    ...opts,
    rewrite: (path) => path,
  },
  '/api/projects': {
    target: 'http://localhost:8084',
    ...opts,
    rewrite: rewriteProjects,
  },
  '/api': { target: 'http://localhost:8050', ...opts },
};
