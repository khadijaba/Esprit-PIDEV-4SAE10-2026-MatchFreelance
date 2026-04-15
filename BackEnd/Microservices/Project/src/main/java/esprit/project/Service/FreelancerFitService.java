package esprit.project.Service;

import esprit.project.Repositories.ClientFreelancerRatingRepository;
import esprit.project.Repositories.ProjectRepository;
import esprit.project.client.CandidatureClient;
import esprit.project.dto.FreelancerFitBatchDto;
import esprit.project.dto.FreelancerFitDto;
import esprit.project.dto.SubmitFreelancerRatingRequest;
import esprit.project.dto.candidature.CandidatureSummaryDto;
import esprit.project.entities.ClientFreelancerRating;
import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class FreelancerFitService {

    private static final double SIMILARITY_THRESHOLD = 0.18;

    private final ProjectRepository projectRepository;
    private final CandidatureClient candidatureClient;
    private final ClientFreelancerRatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public FreelancerFitBatchDto computeFit(Long projectId, List<Long> freelancerIds) {
        Project target = projectRepository.findById(projectId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
        if (freelancerIds == null || freelancerIds.isEmpty()) {
            return FreelancerFitBatchDto.builder().projectId(projectId).freelancers(List.of()).build();
        }
        List<FreelancerFitDto> rows = new ArrayList<>();
        for (Long fid : freelancerIds.stream().distinct().toList()) {
            rows.add(computeOne(target, fid));
        }
        return FreelancerFitBatchDto.builder().projectId(projectId).freelancers(rows).build();
    }

    private FreelancerFitDto computeOne(Project target, Long freelancerId) {
        List<CandidatureSummaryDto> candidatures = safeListCandidatures(freelancerId);
        List<Project> pastProjects = loadPastAcceptedProjects(target.getId(), freelancerId, candidatures);

        List<Double> ratiosSimilar = new ArrayList<>();
        List<Double> ratiosAll = new ArrayList<>();
        List<Integer> actualDaysSimilar = new ArrayList<>();
        List<Integer> actualDaysAll = new ArrayList<>();
        double bestSkillSim = 0;
        Set<String> unionPastSkills = new HashSet<>();

        List<String> targetSkills = Optional.ofNullable(target.getRequiredSkills()).orElse(List.of());
        for (Project p : pastProjects) {
            List<String> ps = Optional.ofNullable(p.getRequiredSkills()).orElse(List.of());
            unionPastSkills.addAll(normalizeSkills(ps));
            double sim = jaccard(targetSkills, ps);
            bestSkillSim = Math.max(bestSkillSim, sim);
            if (p.getStatus() != ProjectStatus.COMPLETED) {
                continue;
            }
            int actual = daysBetween(p.getCreatedAt(), p.getUpdatedAt());
            int planned = p.getDuration() != null && p.getDuration() > 0 ? p.getDuration() : Math.max(1, actual);
            double ratio = planned / (double) Math.max(1, actual);
            ratiosAll.add(Math.min(2.0, ratio));
            actualDaysAll.add(actual);
            if (sim >= SIMILARITY_THRESHOLD) {
                ratiosSimilar.add(Math.min(2.0, ratio));
                actualDaysSimilar.add(actual);
            }
        }

        int plannedTarget = target.getDuration() != null && target.getDuration() > 0 ? target.getDuration() : 30;
        double ratioBlend = medianDouble(ratiosSimilar.isEmpty() ? ratiosAll : ratiosSimilar);
        if (Double.isNaN(ratioBlend)) {
            ratioBlend = 1.0;
        }
        int estimate = (int) Math.round(plannedTarget * ratioBlend);
        estimate = clamp(estimate, 1, Math.max(1, plannedTarget * 3));

        int low = Math.max(1, (int) Math.round(estimate * 0.72));
        int high = Math.max(estimate, (int) Math.round(estimate * 1.38));
        List<Integer> actPool = actualDaysSimilar.isEmpty() ? actualDaysAll : actualDaysSimilar;
        if (!actPool.isEmpty()) {
            int medAct = medianInt(actPool);
            if (medAct > 0) {
                double factor = plannedTarget / (double) medAct;
                low = Math.max(1, (int) Math.round(percentile(actPool, 0.25) * factor));
                high = Math.max(estimate, (int) Math.round(percentile(actPool, 0.75) * factor));
            }
            if (low > high) {
                int t = low;
                low = high;
                high = t;
            }
        }

        double skillFitTarget;
        if (pastProjects.isEmpty()) {
            skillFitTarget = 45;
        } else {
            double jUnion = jaccard(targetSkills, new ArrayList<>(unionPastSkills));
            skillFitTarget = clamp(100 * Math.max(jUnion, bestSkillSim), 0, 100);
        }

        double efficiency = 50;
        if (!ratiosAll.isEmpty()) {
            double meanR = ratiosAll.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
            efficiency = clamp(meanR / 1.2 * 100, 0, 100);
        }

        long completed = pastProjects.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();
        long inProgress = pastProjects.stream().filter(p -> p.getStatus() == ProjectStatus.IN_PROGRESS).count();
        double experience = clamp(completed * 14 + inProgress * 6, 0, 100);

        Double avgClientRating = ratingRepository.averageRatingByFreelancerId(freelancerId);
        double reputation = avgClientRating == null ? 50 : (avgClientRating / 5.0) * 100;

        double score;
        if (avgClientRating == null) {
            score = 0.45 * skillFitTarget + 0.35 * efficiency + 0.20 * experience;
        } else {
            score = 0.38 * skillFitTarget + 0.28 * efficiency + 0.17 * experience + 0.17 * reputation;
        }
        int successScore = (int) Math.round(clamp(score, 0, 100));

        int considered = pastProjects.size();
        String confidence = considered >= 5 ? "HIGH" : considered >= 2 ? "MEDIUM" : "LOW";

        String summary = buildSummary(estimate, successScore, confidence, considered, avgClientRating);

        return FreelancerFitDto.builder()
                .freelancerId(freelancerId)
                .estimatedDurationDays(estimate)
                .durationLowDays(Math.min(low, estimate))
                .durationHighDays(Math.max(high, estimate))
                .successScore(successScore)
                .confidence(confidence)
                .pastMissionsConsidered(considered)
                .summary(summary)
                .build();
    }

    private static String buildSummary(int estimate, int score, String confidence, int past, Double avgRating) {
        String r = avgRating == null ? "pas encore de notes clients en base" : String.format("note moyenne clients ≈ %.1f/5", avgRating);
        return String.format(
                "Durée estimée ~%d j (intervalle affiché), score réussite projet %d/100, confiance %s (%d mission(s) passée(s), %s).",
                estimate, score, confidence, past, r);
    }

    @Transactional
    public ClientFreelancerRating submitRating(Long projectId, SubmitFreelancerRatingRequest request) {
        projectRepository.findById(projectId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
        if (request.getFreelancerId() == null || request.getRating() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "freelancerId and rating required");
        }
        ClientFreelancerRating row = ratingRepository
                .findByProjectIdAndFreelancerId(projectId, request.getFreelancerId())
                .orElseGet(ClientFreelancerRating::new);
        row.setProjectId(projectId);
        row.setFreelancerId(request.getFreelancerId());
        row.setRating(request.getRating());
        return ratingRepository.save(row);
    }

    private List<CandidatureSummaryDto> safeListCandidatures(Long freelancerId) {
        try {
            List<CandidatureSummaryDto> list = candidatureClient.listByFreelancer(freelancerId);
            return list != null ? list : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Project> loadPastAcceptedProjects(Long currentProjectId, Long freelancerId, List<CandidatureSummaryDto> candidatures) {
        Set<Long> ids = candidatures.stream()
                .filter(c -> "ACCEPTED".equalsIgnoreCase(String.valueOf(c.getStatus())))
                .map(CandidatureSummaryDto::getProjectId)
                .filter(Objects::nonNull)
                .filter(pid -> !pid.equals(currentProjectId))
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return List.of();
        }
        return projectRepository.findAllById(ids);
    }

    private static int daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 1;
        }
        int d = (int) ChronoUnit.DAYS.between(start, end);
        return Math.max(1, d);
    }

    private static double jaccard(List<String> a, List<String> b) {
        Set<String> A = normalizeSkills(a);
        Set<String> B = normalizeSkills(b);
        if (A.isEmpty() && B.isEmpty()) {
            return 0;
        }
        Set<String> inter = new HashSet<>(A);
        inter.retainAll(B);
        Set<String> union = new HashSet<>(A);
        union.addAll(B);
        return union.isEmpty() ? 0 : (double) inter.size() / union.size();
    }

    private static Set<String> normalizeSkills(List<String> skills) {
        if (skills == null) {
            return new HashSet<>();
        }
        return skills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private static double medianDouble(List<Double> values) {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static int medianInt(List<Integer> values) {
        if (values.isEmpty()) {
            return 1;
        }
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (int) Math.round((sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0);
    }

    private static double percentile(List<Integer> sortedCopy, double p) {
        if (sortedCopy.isEmpty()) {
            return 1;
        }
        List<Integer> s = new ArrayList<>(sortedCopy);
        Collections.sort(s);
        if (s.size() == 1) {
            return s.get(0);
        }
        double idx = p * (s.size() - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        return s.get(lo) + (s.get(hi) - s.get(lo)) * (idx - lo);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
