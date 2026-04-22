/**
 * Tout /api passe par l'API Gateway (8086).
 * La gateway route /api/ai/** vers Team AI (Python), voir ApiGatewayApplication team.ai.url (défaut :5000).
 *
 * Évite d'appeler :5000 depuis le serveur de dev Angular (souvent la cause de « service indisponible »
 * si seul la stack Java + gateway est démarrée, ou si le pare-feu bloque 5000 côté Node).
 */
module.exports = [
  {
    context: ['/api'],
    target: 'http://localhost:8086',
    secure: false,
    changeOrigin: true,
  },
];
