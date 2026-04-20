package esprit.tn.blog.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class ModerationService {

    // Hardened Java Regex pattern for common inappropriate words
    private static final String BANNED_PATTERN_STR = "\\b(fuck|shit|asshole|bitch|bastard|dick|pussy|slut|whore|crap|damn|hell)\\b";
    private static final Pattern BANNED_PATTERN = Pattern.compile(BANNED_PATTERN_STR, Pattern.CASE_INSENSITIVE);

    /**
     * Checks if the content is inappropriate.
     * @param content The text to check
     * @return true if the content is toxic or contained banned words
     */
    public boolean isToxic(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        // --- LEVEL 1: Check against banned words list ---
        if (BANNED_PATTERN.matcher(content).find()) {
            return true;
        }

        // --- LEVEL 2: Optional AI check could be added here in the future ---
        // For now, this acts as the "Server Source of Truth"

        return false;
    }
}
