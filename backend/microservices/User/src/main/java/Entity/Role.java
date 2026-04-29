package Entity;

import java.util.Locale;

/**
 * Valeurs stockées dans la colonne {@code role} de la table {@code users}.
 * Le porteur de projet est uniquement {@link #PROJECT_OWNER} (plus de synonyme {@code CLIENT}).
 */
public enum Role {
    ADMIN,
    PROJECT_OWNER,
    FREELANCER,
    /**
     * Valeur encore présente en base sur d’anciens comptes porteurs ; côté API on l’expose comme « CLIENT ».
     */
    CLIENT;

    public boolean isProjectOwner() {
        return this == PROJECT_OWNER;
    }

    /**
     * Inscription / API : accepte encore la chaîne {@code CLIENT} pour compatibilité et la mappe vers {@link #PROJECT_OWNER}.
     */
    public static Role fromSignupString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Rôle requis");
        }
        String r = raw.trim().toUpperCase(Locale.ROOT);
        if ("CLIENT".equals(r)) {
            return PROJECT_OWNER;
        }
        return Role.valueOf(r);
    }
}
