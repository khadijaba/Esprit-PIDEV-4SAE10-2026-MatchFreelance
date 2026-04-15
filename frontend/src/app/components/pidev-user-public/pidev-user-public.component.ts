import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pidev-user-public',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pidev-user-public.component.html',
})
export class PidevUserPublicComponent {
  readonly legacyEndpoints = [
    { method: 'POST', path: '/api/users/auth/login', note: 'Connexion MatchFreelance (JSON email, password)' },
    { method: 'POST', path: '/api/users/auth/register', note: 'Inscription simplifiée (JSON)' },
    { method: 'GET', path: '/api/users', note: 'Liste des comptes (option ?role=CLIENT|FREELANCER)' },
    { method: 'GET', path: '/api/users/me/profile', note: 'Profil JWT courant (format front)' },
    { method: 'DELETE', path: '/api/users/me', note: 'Suppression du compte connecté' },
  ];

  readonly pidevAuthEndpoints = [
    { method: 'POST', path: '/api/auth/signup', note: 'Inscription PIDEV complète (multipart + fichier optionnel)' },
    { method: 'POST', path: '/api/auth/signin', note: 'Connexion PIDEV (JSON SignInRequest)' },
    { method: 'POST', path: '/api/auth/signin-face', note: 'Connexion par reconnaissance faciale' },
    { method: 'POST', path: '/api/auth/reset-password/request', note: 'Demande reset mot de passe (email)' },
    { method: 'POST', path: '/api/auth/reset-password/confirm', note: 'Confirmation avec token' },
    { method: 'POST', path: '/api/auth/verify-email', note: 'Vérification email' },
    { method: 'POST', path: '/api/auth/resend-verification', note: 'Renvoyer email de vérification' },
    { method: 'POST', path: '/api/auth/change-password-with-code', note: 'Changement mot de passe avec code' },
  ];

  readonly pidevUserEndpoints = [
    { method: 'GET', path: '/api/users/welcome', note: 'Message welcome (config)' },
    { method: 'GET', path: '/api/users/me', note: 'Utilisateur JWT (entité User, mot de passe masqué)' },
    { method: 'PUT', path: '/api/users/me', note: 'Mise à jour profil' },
    { method: 'POST', path: '/api/users/me/avatar', note: 'Upload avatar (multipart file)' },
  ];

  readonly pidevAdminEndpoints = [
    { method: 'GET', path: '/api/admin/users', note: 'Liste utilisateurs (rôle ADMIN)' },
    { method: 'DELETE', path: '/api/admin/users/{id}', note: 'Supprimer un utilisateur' },
    { method: 'PATCH', path: '/api/admin/users/{id}/status', note: 'Activer / désactiver (body { enabled })' },
    { method: 'GET', path: '/api/admin/v2/statistics', note: 'Statistiques agrégées' },
    { method: 'GET', path: '/api/admin/v2/statistics/range', note: 'Stats sur plage de dates' },
    { method: 'POST', path: '/api/admin/v2/search', note: 'Recherche paginée (filtres)' },
    { method: 'GET', path: '/api/admin/v2/search/simple', note: 'Recherche rapide ?term=' },
    { method: 'GET', path: '/api/admin/v2/users/role/{role}', note: 'Par rôle enum' },
    { method: 'GET', path: '/api/admin/v2/users/active', note: 'Comptes actifs' },
    { method: 'GET', path: '/api/admin/v2/users/inactive', note: 'Comptes inactifs' },
    { method: 'GET', path: '/api/admin/v2/users', note: 'Liste paginée (query page, size, sortBy, sortDir)' },
    { method: 'DELETE', path: '/api/admin/v2/users/{id}', note: 'Supprimer (v2)' },
  ];

  readonly other = [
    { path: '/uploads/avatars/...', note: 'Fichiers statiques servis par le microservice User' },
    { path: '/api/test/sendgrid, /api/test/send-test-email', note: 'Tests SendGrid (dev)' },
    { path: 'OAuth2', note: 'Optionnel : matchfreelance.oauth2.enabled dans application.properties' },
  ];
}
