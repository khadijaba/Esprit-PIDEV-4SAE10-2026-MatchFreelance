/**
 * Configuration front plateforme MatchFreelance (validation).
 * L’app « blog / forum » (projet Esprit-PIDEV…-MatchFreelance-blog) tourne en général sur un autre port.
 */
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
