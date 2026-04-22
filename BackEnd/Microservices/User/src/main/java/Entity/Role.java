package Entity;

import java.util.Locale;

/**
 * Valeurs alignées sur la colonne {@code user_role} en base.
 * Le porteur de projet est uniquement {@link #PROJECT_OWNER} (plus de synonyme {@code CLIENT}).
 */
public enum Role {
    ADMIN,
    PROJECT_OWNER,
    FREELANCER;

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
