package com.freelancing.candidature.service;

import com.freelancing.candidature.client.ContractClient;
import com.freelancing.candidature.client.ProjectClient;
import com.freelancing.candidature.dto.BudgetStatsDTO;
import com.freelancing.candidature.dto.RankedCandidatureDTO;
import com.freelancing.candidature.dto.ScoreBreakdownDTO;
import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.repository.CandidatureRepository;
import com.freelancing.candidature.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Advanced ranking and budget analytics for candidatures.
 * Computes composite scores (AI + budget + response speed + proposal quality + pitch analyzer)
 * and statistical budget metrics for a project.
 */
@Service
@RequiredArgsConstructor
public class CandidatureRankingService {

    private static final double WEIGHT_AI = 0.18;
    private static final double WEIGHT_BUDGET = 0.28;
    private static final double WEIGHT_RESPONSE_SPEED = 0.14;
    private static final double WEIGHT_PROPOSAL_QUALITY = 0.14;
    private static final double WEIGHT_PITCH = 0.18;
    private static final double WEIGHT_CHAT_COMMUNICATION = 0.08;

    private final CandidatureRepository candidatureRepository;
    private final ProjectClient projectClient;
    private final UserClient userClient;
    private final PitchAnalyzerService pitchAnalyzerService;
    private final ContractClient contractClient;

    /**
     * Returns candidatures for a project ranked by composite score, with optional filters.
     */
    @Transactional(readOnly = true)
    public List<RankedCandidatureDTO> getRankedCandidatures(Long projectId, Long clientId, Double minScore, Integer limit) {
        assertProjectOwner(projectId, clientId);
        ProjectClient.ProjectResponse project = projectClient.getProjectById(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        List<Candidature> list = candidatureRepository.findByProjectId(projectId);
        if (list.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> createdAtOrder = new HashMap<>();
        List<Candidature> sortedByCreated = list.stream()
                .sorted(Comparator.comparing(Candidature::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (int i = 0; i < sortedByCreated.size(); i++) {
            createdAtOrder.put(sortedByCreated.get(i).getId(), (long) (i + 1));
        }

        List<RankedCandidatureDTO> ranked = new ArrayList<>();
        int position = 0;
        List<Map.Entry<Candidature, CompositeScoreResult>> withScores = list.stream()
                .map(c -> {
                    CompositeScoreResult result = computeCompositeScore(c, project, list, createdAtOrder);
                    return (Map.Entry<Candidature, CompositeScoreResult>) new AbstractMap.SimpleEntry<>(c, result);
                })
                .sorted((a, b) -> Double.compare(b.getValue().compositeScore, a.getValue().compositeScore))
                .toList();

        for (Map.Entry<Candidature, CompositeScoreResult> entry : withScores) {
            Candidature c = entry.getKey();
            CompositeScoreResult result = entry.getValue();
            if (minScore != null && result.compositeScore < minScore) continue;
            position++;
            if (limit != null && position > limit) break;

            RankedCandidatureDTO dto = toRankedDTO(c, result, position);
            ranked.add(dto);
        }

        List<Long> freelancerIds = list.stream().map(Candidature::getFreelancerId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> names = resolveFreelancerNames(freelancerIds);
        ranked.forEach(dto -> dto.setFreelancerName(names.get(dto.getFreelancerId())));
        return ranked;
    }

    /**
     * Returns budget statistics for candidatures on a project (mean, median, percentiles, recommended range).
     */
    @Transactional(readOnly = true)
    public BudgetStatsDTO getBudgetStats(Long projectId, Long clientId) {
        assertProjectOwner(projectId, clientId);
        List<Candidature> list = candidatureRepository.findByProjectId(projectId);
        List<Double> budgets = list.stream()
                .map(Candidature::getProposedBudget)
                .filter(Objects::nonNull)
                .sorted(Double::compare)
                .toList();

        BudgetStatsDTO dto = new BudgetStatsDTO();
        dto.setProjectId(projectId);
        dto.setCandidatureCount(budgets.size());

        if (budgets.isEmpty()) {
            dto.setMinProposedBudget(null);
            dto.setMaxProposedBudget(null);
            dto.setAverageProposedBudget(null);
            dto.setMedianProposedBudget(null);
            dto.setPercentile25(null);
            dto.setPercentile75(null);
            dto.setRecommendedMin(null);
            dto.setRecommendedMax(null);
            dto.setStandardDeviation(null);
            return dto;
        }

        dto.setMinProposedBudget(budgets.get(0));
        dto.setMaxProposedBudget(budgets.get(budgets.size() - 1));
        double sum = budgets.stream().mapToDouble(Double::doubleValue).sum();
        dto.setAverageProposedBudget(round(sum / budgets.size(), 2));
        dto.setMedianProposedBudget(percentile(budgets, 0.5));
        dto.setPercentile25(percentile(budgets, 0.25));
        dto.setPercentile75(percentile(budgets, 0.75));
        dto.setRecommendedMin(dto.getPercentile25());
        dto.setRecommendedMax(dto.getPercentile75());
        dto.setStandardDeviation(standardDeviation(budgets));
        return dto;
    }

    private CompositeScoreResult computeCompositeScore(Candidature c, ProjectClient.ProjectResponse project,
                                                      List<Candidature> allForProject, Map<Long, Long> orderByCreated) {
        double aiRaw = c.getAiMatchScore() != null ? Math.min(100.0, Math.max(0.0, c.getAiMatchScore())) : 50.0;
        double budgetRaw = computeBudgetCompetitiveness(c.getProposedBudget(), project.getMinBudget(), project.getMaxBudget(), allForProject);
        long order = orderByCreated.getOrDefault(c.getId(), 999L);
        double speedRaw = computeResponseSpeedScore(order, allForProject.size());
        double qualityRaw = computeProposalQualityScore(c.getMessage(), c.getExtraTasksBudget());
        double pitchRaw = pitchAnalyzerService.computePitchScore(project, c.getMessage());
        double chatRaw = getChatCommunicationScore(c.getFreelancerId());

        double aiContrib = aiRaw * WEIGHT_AI;
        double budgetContrib = budgetRaw * WEIGHT_BUDGET;
        double speedContrib = speedRaw * WEIGHT_RESPONSE_SPEED;
        double qualityContrib = qualityRaw * WEIGHT_PROPOSAL_QUALITY;
        double pitchContrib = pitchRaw * WEIGHT_PITCH;
        double chatContrib = chatRaw * WEIGHT_CHAT_COMMUNICATION;
        double composite = aiContrib + budgetContrib + speedContrib + qualityContrib + pitchContrib + chatContrib;
        composite = Math.min(100.0, Math.max(0.0, composite));

        ScoreBreakdownDTO breakdown = new ScoreBreakdownDTO(
                round(aiContrib, 2),
                round(budgetContrib, 2),
                round(speedContrib, 2),
                round(qualityContrib, 2),
                round(pitchContrib, 2),
                round(chatContrib, 2),
                String.format("composite = %.0f%% AI + %.0f%% budget + %.0f%% speed + %.0f%% quality + %.0f%% pitch + %.0f%% chat",
                        WEIGHT_AI * 100, WEIGHT_BUDGET * 100, WEIGHT_RESPONSE_SPEED * 100, WEIGHT_PROPOSAL_QUALITY * 100, WEIGHT_PITCH * 100, WEIGHT_CHAT_COMMUNICATION * 100)
        );
        return new CompositeScoreResult(round(composite, 2), breakdown);
    }

    private double computeBudgetCompetitiveness(Double proposed, Double minB, Double maxB, List<Candidature> all) {
        if (proposed == null || minB == null || maxB == null) return 50.0;
        double range = maxB - minB;
        if (range <= 0) return 100.0;
        double position = (proposed - minB) / range;
        double competitiveScore = 100.0 - (position * 40.0);
        List<Double> others = all.stream().map(Candidature::getProposedBudget).filter(Objects::nonNull).toList();
        if (others.size() >= 2) {
            double avg = others.stream().mapToDouble(Double::doubleValue).average().orElse(proposed);
            if (proposed <= avg) competitiveScore = Math.min(100.0, competitiveScore + 10.0);
        }
        return Math.max(0.0, Math.min(100.0, competitiveScore));
    }

    private double computeResponseSpeedScore(long orderPosition, int total) {
        if (total <= 1) return 100.0;
        return Math.max(0.0, 100.0 - ((orderPosition - 1) * (80.0 / Math.max(1, total - 1))));
    }

    private double computeProposalQualityScore(String message, Double extraTasksBudget) {
        int score = 30;
        if (message != null && !message.isBlank()) {
            int words = message.trim().split("\\s+").length;
            if (words >= 20 && words <= 200) score += 50;
            else if (words > 200) score += 30;
            else if (words >= 10) score += 20;
        }
        if (extraTasksBudget != null && extraTasksBudget >= 0) score += 20;
        return Math.min(100.0, score);
    }

    /**
     * Chat communication score (0–100) from contract-service based on freelancer's message history.
     * Returns 50 when no chat data or on error.
     */
    private double getChatCommunicationScore(Long freelancerId) {
        if (freelancerId == null) return 50.0;
        try {
            ContractClient.CommunicationScoreResponse r = contractClient.getCommunicationScore(freelancerId);
            return r != null ? Math.max(0.0, Math.min(100.0, r.getScore())) : 50.0;
        } catch (Exception e) {
            return 50.0;
        }
    }

    private static double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return 0.0;
        double idx = p * (sorted.size() - 1);
        int i = (int) Math.floor(idx);
        double f = idx - i;
        if (i >= sorted.size() - 1) return sorted.get(sorted.size() - 1);
        return round(sorted.get(i) * (1 - f) + sorted.get(i + 1) * f, 2);
    }

    private static double standardDeviation(List<Double> values) {
        if (values.size() < 2) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / values.size();
        return round(Math.sqrt(variance), 2);
    }

    private static double round(double v, int scale) {
        double f = Math.pow(10, scale);
        return Math.round(v * f) / f;
    }

    private RankedCandidatureDTO toRankedDTO(Candidature c, CompositeScoreResult result, int rank) {
        RankedCandidatureDTO dto = new RankedCandidatureDTO();
        dto.setId(c.getId());
        dto.setProjectId(c.getProjectId());
        dto.setFreelancerId(c.getFreelancerId());
        dto.setMessage(c.getMessage());
        dto.setProposedBudget(c.getProposedBudget());
        dto.setExtraTasksBudget(c.getExtraTasksBudget());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setAiMatchScore(c.getAiMatchScore());
        dto.setAiInsights(c.getAiInsights());
        dto.setCompositeScore(result.compositeScore);
        dto.setScoreBreakdown(result.breakdown);
        dto.setRank(rank);
        return dto;
    }

    private Map<Long, String> resolveFreelancerNames(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return userClient.getUsersByIds(ids).stream()
                .filter(u -> u != null && u.getId() != null && u.getName() != null)
                .collect(Collectors.toMap(UserClient.UserResponse::getId, UserClient.UserResponse::getName, (a, b) -> a));
    }

    private void assertProjectOwner(Long projectId, Long clientId) {
        if (clientId == null) {
            throw new RuntimeException("clientId is required");
        }
        ProjectClient.ProjectResponse project = projectClient.getProjectById(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
        if (project.getClientId() == null || !project.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the project owner can access candidature analytics for this project");
        }
    }

    private record CompositeScoreResult(double compositeScore, ScoreBreakdownDTO breakdown) {}
}
