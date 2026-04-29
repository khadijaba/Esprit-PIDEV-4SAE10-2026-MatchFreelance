package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.client.ProjectClient;
import tn.esprit.evaluation.client.SkillClient;
import tn.esprit.evaluation.dto.FreelancerProjectMatchingDto;
import tn.esprit.evaluation.dto.FreelancerRankingDto;
import tn.esprit.evaluation.dto.ProjetMarcheDto;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.repository.CertificatRepository;
import tn.esprit.evaluation.repository.PassageExamenRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ranking global des freelancers et recommandations de projets (Skill + Project via Eureka).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingRecommendationService {

    private final PassageExamenRepository passageExamenRepository;
    private final CertificatRepository certificatRepository;
    private final SkillClient skillClient;
    private final ProjectClient projectClient;

    public List<FreelancerRankingDto> getGlobalRanking() {
        List<FreelancerRankingDto> rows = aggregateRankingRows();
        rows.sort(Comparator
                .comparingInt(FreelancerRankingDto::getGlobalScore).reversed()
                .thenComparingInt(FreelancerRankingDto::getCertificationsCount).reversed()
                .thenComparingInt(FreelancerRankingDto::getAverageScore).reversed()
                .thenComparing(FreelancerRankingDto::getLastAttemptAt, Comparator.nullsLast(Comparator.reverseOrder())));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank(i + 1);
        }
        return rows;
    }

    public List<FreelancerRankingDto> getTopPerformers(int limit) {
        int n = Math.max(1, limit);
        return getGlobalRanking().stream().limit(n).collect(Collectors.toList());
    }

    public FreelancerProjectMatchingDto recommendProjects(Long freelancerId, int limit) {
        Integer examGlobal = resolveExamGlobalScore(freelancerId);
        List<Map<String, Object>> profileSkills = skillClient.getSkillsByFreelancer(freelancerId);
        int tokens = profileSkills.size();
        List<Map<String, Object>> open = projectClient.getProjectsByStatus("OPEN");
        int n = Math.max(1, limit);
        List<ProjetMarcheDto> projects = open.stream()
                .map(p -> toProjetMatch(p, profileSkills, examGlobal))
                .sorted(Comparator.comparingInt(ProjetMarcheDto::getScoreComposite).reversed())
                .limit(n)
                .collect(Collectors.toList());
        return FreelancerProjectMatchingDto.builder()
                .freelancerId(freelancerId)
                .profileSkillTokens(tokens)
                .freelancerExamGlobalScore(examGlobal)
                .projects(projects)
                .build();
    }

    private ProjetMarcheDto toProjetMatch(Map<String, Object> p, List<Map<String, Object>> freelancerSkills, Integer examGlobal) {
        Long id = toLong(p.get("id"));
        String title = p.get("title") != null ? p.get("title").toString() : "";
        Double budget = p.get("budget") instanceof Number nb ? nb.doubleValue() : null;
        Integer duration = p.get("duration") instanceof Number nb ? nb.intValue() : null;
        String statut = p.get("status") != null ? p.get("status").toString() : "";
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) p.get("requiredSkills");
        int align = alignmentScore(freelancerSkills, required);
        int composite = examGlobal != null
                ? (int) Math.round(align * 0.6 + examGlobal * 0.4)
                : align;
        return ProjetMarcheDto.builder()
                .id(id)
                .titre(title)
                .budget(budget)
                .dureeJours(duration)
                .statut(statut)
                .raison("Alignement compétences profil / exigences projet")
                .scoreAlignementSkills(align)
                .scoreComposite(composite)
                .build();
    }

    private static int alignmentScore(List<Map<String, Object>> freelancerSkills, List<String> required) {
        if (required == null || required.isEmpty()) {
            return 50;
        }
        Set<String> profile = freelancerSkills.stream()
                .map(m -> normalizeToken(m.get("name")))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (profile.isEmpty()) {
            return 0;
        }
        int match = 0;
        for (String req : required) {
            String n = normalizeToken(req);
            if (n.isEmpty()) {
                continue;
            }
            boolean ok = profile.stream().anyMatch(p -> p.equals(n) || p.contains(n) || n.contains(p));
            if (ok) {
                match++;
            }
        }
        return (int) Math.round(100.0 * match / required.size());
    }

    private static String normalizeToken(Object name) {
        if (name == null) {
            return "";
        }
        return name.toString().toLowerCase(Locale.ROOT).trim();
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer resolveExamGlobalScore(Long freelancerId) {
        return aggregateRankingRows().stream()
                .filter(r -> freelancerId.equals(r.getFreelancerId()))
                .map(FreelancerRankingDto::getGlobalScore)
                .findFirst()
                .orElse(null);
    }

    private List<FreelancerRankingDto> aggregateRankingRows() {
        List<PassageExamen> passages = passageExamenRepository.findAll();
        if (passages.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<PassageExamen>> byFreelancer = passages.stream()
                .collect(Collectors.groupingBy(PassageExamen::getFreelancerId));

        Map<Long, Integer> certifCountByFreelancer = new HashMap<>();
        certificatRepository.findAll().forEach(c -> {
            if (c.getPassageExamen() != null && c.getPassageExamen().getFreelancerId() != null) {
                Long freelancerId = c.getPassageExamen().getFreelancerId();
                certifCountByFreelancer.merge(freelancerId, 1, Integer::sum);
            }
        });

        List<FreelancerRankingDto> rows = new ArrayList<>();
        for (Map.Entry<Long, List<PassageExamen>> entry : byFreelancer.entrySet()) {
            Long freelancerId = entry.getKey();
            List<PassageExamen> attempts = entry.getValue();
            int attemptsCount = attempts.size();
            int avgScore = (int) Math.round(attempts.stream()
                    .map(PassageExamen::getScore)
                    .filter(s -> s != null)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0));
            long successCount = attempts.stream()
                    .filter(p -> p.getResultat() == PassageExamen.ResultatExamen.REUSSI)
                    .count();
            int successRate = (int) Math.round(100.0 * successCount / Math.max(1, attemptsCount));
            int certifs = certifCountByFreelancer.getOrDefault(freelancerId, 0);
            int globalScore = computeGlobalScore(avgScore, successRate, certifs);
            LocalDateTime lastAttemptAt = attempts.stream()
                    .map(PassageExamen::getDatePassage)
                    .filter(d -> d != null)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            rows.add(FreelancerRankingDto.builder()
                    .freelancerId(freelancerId)
                    .globalScore(globalScore)
                    .averageScore(avgScore)
                    .successRate(successRate)
                    .certificationsCount(certifs)
                    .attemptsCount(attemptsCount)
                    .lastAttemptAt(lastAttemptAt)
                    .build());
        }
        return rows;
    }

    private static int computeGlobalScore(int averageScore, int successRate, int certifs) {
        int certifBonus = Math.min(100, certifs * 20);
        double weighted = averageScore * 0.6 + successRate * 0.3 + certifBonus * 0.1;
        int rounded = (int) Math.round(weighted);
        return Math.max(0, Math.min(100, rounded));
    }
}
