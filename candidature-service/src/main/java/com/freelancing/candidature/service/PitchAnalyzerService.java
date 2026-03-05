package com.freelancing.candidature.service;

import com.freelancing.candidature.client.ProjectClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based pitch analyzer aligned with the frontend pitch-analyzer.service.ts.
 * Computes a 0–100 score for how well a freelancer's application message (pitch)
 * matches the project (skills, problem understanding, experience, communication, value).
 */
@Service
public class PitchAnalyzerService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was",
            "one", "our", "out", "has", "his", "how", "its", "may", "new", "now", "old", "see",
            "way", "who", "did", "get", "got", "let", "put", "say", "she", "too", "use",
            "your", "this", "that", "with", "from", "have", "been", "will", "would", "could",
            "should", "them", "they", "what", "when", "where", "which", "while", "than", "then",
            "just", "only", "also", "more", "some", "very", "into", "over", "such");

    private static final Pattern TECH_PATTERN = Pattern.compile(
            "\\b(react|angular|vue|node|node\\.?js|java|python|typescript|javascript|php|django|spring|sql|mysql|postgresql|mongodb|redis|aws|docker|kubernetes|api|rest|graphql|frontend|backend|fullstack|full.?stack|mobile|flutter|react.?native|redux|zustand|next\\.?js|express|nest|tailwind|sass|html|css|figma|swagger|websocket|stripe|firebase|terraform|ci/cd|jenkins|github|gitlab|azure|gcp|nosql|elasticsearch|microservices|agile|scrum|jira)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> EXP_MARKERS = Arrays.asList(
            "experience", "years", "built", "shipped", "worked", "developed", "implemented",
            "led", "senior", "expert", "production", "previous", "portfolio", "client",
            "architected", "launched", "delivered", "team", "certified", "professional", "extensive",
            "proven", "track record", "successfully", "completed", "handled", "managed", "designed",
            "engineered", "deployed", "maintained", "scaled", "optimized", "integrated", "automated",
            "freelance", "freelancer", "contractor", "consultant", "agency", "startup", "enterprise", "b2b", "b2c");

    private static final List<String> VALUE_MARKERS = Arrays.asList(
            "fit", "right choice", "best", "why i", "perfect", "ideal", "trust", "reliable",
            "deliver", "commit", "dedicated", "match", "align", "excited", "passionate", "interested",
            "available", "ready", "can start", "understand", "familiar", "experienced in", "specialize",
            "focus", "expertise", "strong fit", "great fit", "looking forward", "happy to", "glad to",
            "would love", "ideal candidate", "suitable", "well-suited");

    private static final Pattern EXP_NUMBERS = Pattern.compile(
            "\\d+(\\s*(\\+)?\\s*(years?|yr|projects?|clients?|apps?|sites?))?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXP_OUTCOME = Pattern.compile(
            "\\b(successfully|delivered|launched|shipped|reduced|increased|improved|saved|built)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Returns a 0–100 score for how well the pitch (application message) matches the project.
     * Uses the same dimensions as the frontend: technical relevance (30%), problem understanding (25%),
     * experience signals (20%), communication quality (15%), value proposition (10%).
     */
    public double computePitchScore(ProjectClient.ProjectResponse project, String pitch) {
        if (project == null || pitch == null || pitch.isBlank()) {
            return 50.0;
        }
        String pitchLower = pitch.toLowerCase().trim();
        List<String> pitchWords = tokenize(pitch);
        int pitchWordCount = pitchWords.size();
        String desc = project.getDescription() != null ? project.getDescription() : "";
        String title = project.getTitle() != null ? project.getTitle() : "";
        String descAndTitle = desc + " " + title;

        List<String> jobSkills = extractSkillsFromDescription(descAndTitle);
        List<String> jobKeywords = new java.util.ArrayList<>(new LinkedHashSet<>(jobSkills));
        jobKeywords.addAll(extractKeywords(descAndTitle, 4).stream().limit(35).toList());
        jobKeywords = jobKeywords.stream().distinct().toList();

        List<String> mentionedSkills = jobSkills.stream().filter(pitchLower::contains).toList();
        long mentionedKeywordsCount = jobKeywords.stream().filter(pitchLower::contains).count();
        double techRatio = jobSkills.isEmpty() ? mentionedKeywordsCount / (double) Math.max(1, jobKeywords.size()) : mentionedSkills.size() / (double) Math.max(1, jobSkills.size());
        double keywordRatio = jobKeywords.isEmpty() ? 0 : mentionedKeywordsCount / (double) jobKeywords.size();
        int techScore = (int) Math.min(100, Math.round(techRatio * 55 + keywordRatio * 45));

        List<String> descTerms = extractKeywords(descAndTitle, 4).stream().limit(30).toList();
        long matchedTerms = descTerms.stream().filter(pitchLower::contains).count();
        int problemScore = descTerms.isEmpty() ? 50 : (int) Math.min(100, Math.round((matchedTerms / (double) descTerms.size()) * 100));

        long expCount = EXP_MARKERS.stream().filter(pitchLower::contains).count();
        boolean hasNumbers = EXP_NUMBERS.matcher(pitch).find();
        boolean hasOutcome = EXP_OUTCOME.matcher(pitch).find();
        int expScore = (int) Math.min(100, Math.round(
                expCount * 8 + (hasNumbers ? 18 : 0) + (hasOutcome ? 12 : 0)
                + (pitchWordCount >= 80 ? 14 : pitchWordCount >= 50 ? 8 : pitchWordCount >= 30 ? 4 : 0)));

        long paragraphBreaks = pitch.split("\n\n", -1).length - 1;
        boolean hasParagraphs = paragraphBreaks >= 1;
        long exclamations = pitch.chars().filter(c -> c == '!').count();
        long questionMarks = pitch.chars().filter(c -> c == '?').count();
        String noSpaces = pitch.replaceAll("\\s", "");
        double allCapsRatio = noSpaces.isEmpty() ? 0 : (double) noSpaces.chars().filter(Character::isUpperCase).count() / noSpaces.length();
        int commScore = (int) Math.max(0, Math.min(100, 28
                + (pitchWordCount >= 100 ? 28 : pitchWordCount >= 70 ? 22 : pitchWordCount >= 50 ? 15 : pitchWordCount >= 30 ? 8 : 0)
                + (hasParagraphs ? 14 : 0)
                + (exclamations <= 2 ? 14 : exclamations <= 5 ? 4 : -12)
                + (questionMarks <= 1 ? 8 : -5)
                + (allCapsRatio < 0.2 ? 12 : -15)));

        long valueCount = VALUE_MARKERS.stream().filter(pitchLower::contains).count();
        int valueScore = (int) Math.min(100, 35 + valueCount * 12 + (pitchWordCount >= 60 ? 18 : pitchWordCount >= 40 ? 10 : 0));

        double overall = (techScore * 30 + problemScore * 25 + expScore * 20 + commScore * 15 + valueScore * 10) / 100.0;
        return Math.max(0.0, Math.min(100.0, Math.round(overall * 10) / 10.0));
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase().replaceAll("[^\\w\\s]", " ").split("\\s+"))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> extractKeywords(String text, int minLen) {
        List<String> words = tokenize(text);
        return words.stream()
                .filter(w -> w.length() >= minLen && !STOP_WORDS.contains(w))
                .distinct()
                .toList();
    }

    private List<String> extractSkillsFromDescription(String desc) {
        if (desc == null || desc.isBlank()) return List.of("project", "requirements");
        Set<String> tech = new LinkedHashSet<>();
        Matcher m = TECH_PATTERN.matcher(desc.toLowerCase());
        while (m.find()) tech.add(m.group(1).toLowerCase().replace(".", ""));
        if (!tech.isEmpty()) return List.copyOf(tech);
        List<String> kw = extractKeywords(desc, 4).stream()
                .filter(w -> w.length() <= 20)
                .limit(16)
                .distinct()
                .toList();
        return kw.isEmpty() ? List.of("project", "requirements") : kw;
    }
}
