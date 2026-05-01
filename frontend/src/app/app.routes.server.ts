import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * Configuration des routes pour le rendu serveur (SSR).
 * Tout est configuré en mode 'Server' pour un rendu dynamique à chaque requête,
 * évitant ainsi le pré-rendu statique au build (conformément à la demande utilisateur).
 */
export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    renderMode: RenderMode.Server
  }
];
