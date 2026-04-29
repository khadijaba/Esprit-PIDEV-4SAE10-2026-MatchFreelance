package esprit.project.Service;

import esprit.project.Repositories.ProjectRepository;
import esprit.project.client.CandidatureClient;
import esprit.project.dto.ApplicationCountResponse;
import esprit.project.dto.ProjectInsightsDto;
import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProjectInsightsService {

    public static final String MODEL_VERSION = "heuristic-v1";

    private final ProjectRepository projectRepository;
    private final CandidatureClient candidatureClient;

    @Transactional(readOnly = true)
    public ProjectInsightsDto computeInsights(Long projectId) {
        Project p = projectRepository.findById(projectId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
        Long ownerId = p.getProjectOwnerId();

        long ownerTotal = ownerId != null ? projectRepository.countByProjectOwnerId(ownerId) : 0;
        long ownerCompleted = ownerId != null ? projectRepository.countByProjectOwnerIdAndStatus(ownerId, ProjectStatus.COMPLETED) : 0;
        long ownerOpen = ownerId != null ? projectRepository.countByProjectOwnerIdAndStatus(ownerId, ProjectStatus.OPEN) : 0;

        double completionRate = ownerTotal > 0 ? (double) ownerCompleted / ownerTotal : 0.55;

        int skillCount = Optional.ofNullable(p.getRequiredSkills()).map(List::size).orElse(0);
        String niche = nicheDifficulty(skillCount);

        double daily = p.getBudget() != null && p.getDuration() != null && p.getDuration() > 0
                ? p.getBudget() / p.getDuration()
                : 0;

        List<String> flags = new ArrayList<>();
        int titleLen = Optional.ofNullable(p.getTitle()).map(String::length).orElse(0);
        int wordCount = wordCount(p.getDescription());
        if (titleLen < 12) {
            flags.add("TITRE_TROP_COURT");
        }
        if (wordCount < 35) {
            flags.add("DESCRIPTION_INSUFFISANTE");
        }
        if (skillCount == 0) {
            flags.add("AUCUNE_COMPETENCE_REQUISE");
        }
        if (skillCount > 8) {
            flags.add("PROFIL_ELARGI_DIFFICILE_A_REMPLIR");
        }
        if (daily > 0 && daily < 12) {
            flags.add("TAUX_JOURNALIER_FAIBLE");
        }
        if (daily > 280) {
            flags.add("TAUX_JOURNALIER_ELEVE");
        }

        int listingQuality = listingQualityScore(titleLen, wordCount, skillCount, daily);
        int marketAttraction = marketAttractivenessScore(daily);

        int composite = clamp(Math.round(listingQuality * 0.52f + marketAttraction * 0.48f), 0, 100);

        Integer applicationCount = null;
        if (ownerId != null) {
            try {
                ApplicationCountResponse ac = candidatureClient.countApplications(projectId, ownerId);
                if (ac != null) {
                    applicationCount = (int) Math.min(Integer.MAX_VALUE, ac.getCount());
                }
            } catch (Exception ignored) {
                flags.add("CANDIDATURE_METRICS_UNAVAILABLE");
            }
        }

        int momentum = applicationCount == null ? 35 : clamp(applicationCount * 18, 0, 100);
        int track = clamp(Math.round((float) (completionRate * 100)), 0, 100);

        int success = clamp(
                Math.round(listingQuality * 0.38f + track * 0.22f + marketAttraction * 0.18f + momentum * 0.22f),
                0,
                100);

        if (p.getStatus() == ProjectStatus.CANCELLED) {
            success = clamp(success - 25, 0, 100);
            flags.add("PROJET_ANNULE");
        }
        if (p.getStatus() == ProjectStatus.COMPLETED) {
            success = Math.max(success, 88);
        }

        int[] hireRange = estimateHireRange(niche, applicationCount, listingQuality);

        String risk = resolveRiskLevel(listingQuality, flags.size(), p.getStatus());

        String summary = buildSummary(listingQuality, success, niche, applicationCount, hireRange);

        return ProjectInsightsDto.builder()
                .projectId(projectId)
                .listingQualityScore(listingQuality)
                .marketAttractivenessScore(marketAttraction)
                .compositeHealthScore(composite)
                .successLikelihoodScore(success)
                .nicheDifficulty(niche)
                .estimatedDaysToHireLow(hireRange[0])
                .estimatedDaysToHireHigh(hireRange[1])
                .dailyRate(Math.round(daily * 100.0) / 100.0)
                .applicationCount(applicationCount)
                .ownerCompletedProjects(ownerCompleted)
                .ownerOpenProjects(ownerOpen)
                .ownerTotalProjects(ownerTotal)
                .riskLevel(risk)
                .flags(List.copyOf(flags))
                .summary(summary)
                .modelVersion(MODEL_VERSION)
                .computedAt(Instant.now().toString())
                .build();
    }

    private static String nicheDifficulty(int skillCount) {
        if (skillCount <= 2) {
            return "LOW";
        }
        if (skillCount <= 6) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private static int listingQualityScore(int titleLen, int wordCount, int skillCount, double daily) {
        double title = clamp(100.0 * titleLen / 28.0, 0, 100);
        double desc = clamp(100.0 * wordCount / 90.0, 0, 100);
        double skills = skillCount <= 0 ? 0 : clamp(100.0 * Math.min(skillCount, 6) / 6.0, 0, 100);
        double coherence = 100;
        if (daily > 0 && (daily < 8 || daily > 350)) {
            coherence = 55;
        }
        return clamp((int) Math.round(title * 0.22 + desc * 0.38 + skills * 0.28 + coherence * 0.12), 0, 100);
    }

    /**
     * Taux journalier « idéal » pour attirer des profils qualifiés (TND) — courbe en cloche.
     */
    private static int marketAttractivenessScore(double daily) {
        if (daily <= 0) {
            return 45;
        }
        double peak = 72;
        double sigma = 48;
        double x = daily;
        double g = Math.exp(-Math.pow(x - peak, 2) / (2 * sigma * sigma));
        return clamp((int) Math.round(38 + g * 62), 0, 100);
    }

    private static int[] estimateHireRange(String niche, Integer applicationCount, int listingQuality) {
        int low;
        int high;
        switch (niche) {
            case "LOW" -> {
                low = 5;
                high = 18;
            }
            case "MEDIUM" -> {
                low = 10;
                high = 32;
            }
            default -> {
                low = 18;
                high = 55;
            }
        }
        if (applicationCount != null) {
            if (applicationCount >= 6) {
                low = Math.max(2, low - 8);
                high = Math.max(low + 3, high - 10);
            } else if (applicationCount >= 2) {
                low = Math.max(3, low - 4);
                high = Math.max(low + 4, high - 6);
            }
        }
        if (listingQuality < 42) {
            high += 12;
        }
        if (high < low + 5) {
            high = low + 5;
        }
        return new int[]{low, high};
    }

    private static String resolveRiskLevel(int listingQuality, int flagCount, ProjectStatus status) {
        if (status == ProjectStatus.CANCELLED) {
            return "HIGH";
        }
        if (listingQuality < 38 || flagCount >= 4) {
            return "HIGH";
        }
        if (listingQuality < 62 || flagCount >= 2) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String buildSummary(int listingQuality, int success, String niche, Integer applicationCount, int[] hireRange) {
        StringBuilder sb = new StringBuilder();
        sb.append("Qualité d’annonce ").append(listingQuality).append("/100, prédiction de réussite ").append(success).append("/100. ");
        sb.append("Profil ").append(switch (niche) {
            case "LOW" -> "large — recrutement généralement plus rapide";
            case "MEDIUM" -> "intermédiaire";
            default -> "exigeant — prévoir plus de temps de sélection";
        }).append(". ");
        if (applicationCount != null) {
            sb.append(applicationCount).append(" candidature(s). ");
        }
        sb.append("Estimation première shortlist : ").append(hireRange[0]).append("–").append(hireRange[1]).append(" jours.");
        return sb.toString();
    }

    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clamp(long v, int lo, int hi) {
        return clamp((int) v, lo, hi);
    }

    private static int clamp(double v, int lo, int hi) {
        return clamp((int) Math.round(v), lo, hi);
    }
}
