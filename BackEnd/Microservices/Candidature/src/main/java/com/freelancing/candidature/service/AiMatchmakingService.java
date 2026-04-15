package com.freelancing.candidature.service;

import com.freelancing.candidature.client.ProjectClient;
import com.freelancing.candidature.entity.Candidature;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiMatchmakingService {

    // Simulating a knowledge base of technical ecosystems
    private static final Map<String, List<String>> TECH_ECOSYSTEMS = Map.of(
            "frontend", Arrays.asList("react", "angular", "vue", "javascript", "typescript", "html", "css", "tailwind", "ui", "ux"),
            "backend", Arrays.asList("java", "spring", "node", "express", "python", "django", "c#", "dotnet", "php", "laravel", "microservices", "api"),
            "database", Arrays.asList("sql", "mysql", "postgresql", "mongodb", "nosql", "redis", "oracle"),
            "devops", Arrays.asList("docker", "kubernetes", "aws", "azure", "gcp", "ci/cd", "jenkins", "terraform")
    );

    // Professional tone markers
    private static final List<String> PROFESSIONAL_MARKERS = Arrays.asList(
            "experience", "expert", "senior", "successfully", "delivered", "architecture", "scalable", "confident", "quality", "testing", "best practices"
    );

    public void analyzeCandidature(Candidature candidature, ProjectClient.ProjectResponse project) {
        if (project == null || candidature == null) {
            candidature.setAiMatchScore(0.0);
            candidature.setAiInsights("Unable to analyze due to missing data.");
            return;
        }

        String projectText = (project.getTitle() + " " + project.getDescription()).toLowerCase();
        String freelancerText = (candidature.getMessage() == null ? "" : candidature.getMessage()).toLowerCase();

        // 1. Advanced Tech Stack Extraction & Matching (Semantic NLP Simulation)
        double semanticScore = calculateDeepSemanticMatch(freelancerText, projectText);

        // 2. Budget Alignment Calculus
        double budgetScore = calculateBudgetAlignment(candidature.getProposedBudget(), project.getMinBudget(), project.getMaxBudget());

        // 3. Professional Tone & Experience Analysis
        double toneScore = analyzeProfessionalTone(freelancerText);

        // 4. Duration/Timeline Commitment
        double durationScore = analyzeDurationCommitment(freelancerText, project.getDuration());

        // Weighted Aggregation
        // 50% technical match, 30% budget, 10% tone/professionalism, 10% timeline alignment
        double matchScore = (semanticScore * 0.50) + (budgetScore * 0.30) + (toneScore * 0.10) + (durationScore * 0.10);
        
        matchScore = Math.min(100.0, Math.max(0.0, matchScore));
        matchScore = Math.round(matchScore * 10.0) / 10.0;

        candidature.setAiMatchScore(matchScore);
        candidature.setAiInsights(generateDynamicInsights(matchScore, semanticScore, budgetScore, toneScore));
    }

    private double calculateDeepSemanticMatch(String message, String projectDetails) {
        if (message.isBlank()) return 0.0;

        Set<String> projectKeywords = extractTechKeywords(projectDetails);
        Set<String> freelancerKeywords = extractTechKeywords(message);

        if (projectKeywords.isEmpty()) {
            return Math.min(60.0, message.length() * 0.2); // Fallback: reward detailed messages if project is vague
        }

        // Calculate intersection (skills mentioned in both)
        Set<String> matchedSkills = new HashSet<>(projectKeywords);
        matchedSkills.retainAll(freelancerKeywords);

        // Score based on coverage of project's required skills
        double termCoverageScore = ((double) matchedSkills.size() / projectKeywords.size()) * 100.0;

        // Bonus if candidate mentions related ecosystem tools (e.g., Project needs Java, Candidate says Java + Spring + Docker)
        long ecosystemBonus = freelancerKeywords.stream()
                .filter(k -> !projectKeywords.contains(k))
                .count() * 2; // +2 points for every complementary tech skill mentioned

        return Math.min(100.0, termCoverageScore + ecosystemBonus);
    }

    private Set<String> extractTechKeywords(String text) {
        return TECH_ECOSYSTEMS.values().stream()
                .flatMap(List::stream)
                .filter(text::contains)
                .collect(Collectors.toSet());
    }

    private double analyzeProfessionalTone(String message) {
        if (message.isBlank()) return 0.0;
        
        long markerCount = PROFESSIONAL_MARKERS.stream().filter(message::contains).count();
        int wordCount = message.split("\\s+").length;
        
        double score = (markerCount * 15.0); // 15 points per professional buzzword
        
        // Reward well-articulated, adequately sized proposals (between 50 and 300 words)
        if (wordCount >= 30 && wordCount <= 300) {
            score += 40.0; 
        } else if (wordCount > 300) {
            score += 20.0; // Slightly penalize rambling
        } else {
            score -= 10.0; // Penalize extremely short messages
        }
        
        return Math.min(100.0, Math.max(0.0, score));
    }

    private double analyzeDurationCommitment(String message, Integer projectDurationDays) {
        if (projectDurationDays == null || projectDurationDays <= 0) return 50.0; // Neutral if no timeline specified by client
        
        // Use RegEx to find numbers associated with time in the message
        Pattern timePattern = Pattern.compile("(\\d+)\\s*(days|weeks|months)");
        Matcher matcher = timePattern.matcher(message);
        
        if (matcher.find()) {
            int proposedValue = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            int proposedDays = proposedValue;
            if (unit.contains("week")) proposedDays *= 7;
            if (unit.contains("month")) proposedDays *= 30;
            
            // If they can do it faster or on time
            if (proposedDays <= projectDurationDays) {
                return 100.0;
            } else {
                // Deduct points for taking longer than the client's deadline
                double overflowRatio = (double) (proposedDays - projectDurationDays) / projectDurationDays;
                return Math.max(0.0, 100.0 - (overflowRatio * 100)); 
            }
        }
        
        return 60.0; // Minor baseline points if they just don't mention time
    }

    private double calculateBudgetAlignment(Double proposedBudget, Double minBudget, Double maxBudget) {
        if (proposedBudget == null || minBudget == null || maxBudget == null) return 50.0;
        
        if (proposedBudget >= minBudget && proposedBudget <= maxBudget) {
            double range = maxBudget - minBudget;
            if (range == 0) return 100.0;
            double position = 1.0 - ((proposedBudget - minBudget) / range);
            return 80.0 + (position * 20.0);
        }
        
        if (proposedBudget > maxBudget) {
            double overagePercentage = (proposedBudget - maxBudget) / maxBudget;
            return Math.max(0.0, 100.0 - (overagePercentage * 150.0));
        }
        
        return 70.0; // Below budget
    }

    private String generateDynamicInsights(double finalScore, double semanticScore, double budgetScore, double toneScore) {
        List<String> insights = new ArrayList<>();
        
        if (finalScore >= 85) {
            insights.add("🌟 Top Candidate: Excellent cross-dimensional fit.");
        } else if (finalScore >= 65) {
            insights.add("✅ Strong Contender: Meets primary project requirements.");
        } else {
            insights.add("⚠️ High Risk: Candidate may lack strictly required parameters.");
        }
        
        if (semanticScore > 80) {
            insights.add("Deep technical alignment with required tech stack.");
        } else if (semanticScore < 40) {
            insights.add("Missing several core technical keywords mentioned in project details.");
        }
        
        if (toneScore > 75) {
            insights.add("Proposal exhibits high professionalism and clear communication.");
        }
        
        if (budgetScore < 50) {
            insights.add("Red Flag: Budget significantly misaligned with project constraints.");
        }

        return String.join(" ", insights);
    }
}
