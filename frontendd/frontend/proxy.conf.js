/**
 * Ordre : chemins les plus spécifiques en premier.
 * - /api/ai → service Python Team AI (port 5000)
 * - /api → API Gateway sur le host : 8065 (compose : 8065:8086). Si tu lances le gateway en local sans Docker sur 8086, mets la cible à http://localhost:8086.
 */
module.exports = [
  {
    context: ['/api/ai'],
    target: 'http://localhost:5000',
    secure: false,
    changeOrigin: true,
    pathRewrite: { '^/api/ai': '/api' },
  },
  {
    context: ['/api'],
    target: 'http://localhost:8065',
    secure: false,
    changeOrigin: true,
  },
];
