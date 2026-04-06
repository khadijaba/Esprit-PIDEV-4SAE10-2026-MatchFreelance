package esprit.user.dto;

import esprit.user.entities.UserRole;

/**
 * Choix du rôle métier après inscription Keycloak (FREELANCER ou CLIENT uniquement).
 */
public record InitialRoleRequest(UserRole role) {}
