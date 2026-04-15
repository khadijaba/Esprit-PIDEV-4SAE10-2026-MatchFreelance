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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ranking global des freelancers + recommandations projets basées sur les scores et compétences.
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
        int n = Math.max(1, Math.min(limit, 30));
        Set<String> profileTokens = buildFreelancerSkillTokens(skillClient.getSkillsByFreelancer(freelancerId));
        Integer examGlobal = resolveExamGlobalScore(freelancerId);
        List<Map<String, Object>> openProjects = projectClient.getProjetsOuverts();
        List<ProjetMarcheDto> scored = openProjects.stream()
                .map(project -> mapProjectWithScore(project, profileTokens, examGlobal))
                .sorted(Comparator.comparingInt(
                                (ProjetMarcheDto dto) -> dto.getScoreComposite() != null ? dto.getScoreComposite() : 0)
                        .reversed())
                .limit(n)
                .collect(Collectors.toList());
        return FreelancerProjectMatchingDto.builder()
                .freelancerId(freelancerId)
                .profileSkillTokens(profileTokens.size())
                .freelancerExamGlobalScore(examGlobal)
                .projects(scored)
                .build();
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

    /**
     * Score global 0-100 : exam performance + régularité de réussite + bonus certifications.
     */
    private static int computeGlobalScore(int averageScore, int successRate, int certifs) {
        int certifBonus = Math.min(100, certifs * 20);
        double weighted = averageScore * 0.6 + successRate * 0.3 + certifBonus * 0.1;
        int rounded = (int) Math.round(weighted);
        return Math.max(0, Math.min(100, rounded));
    }

    private static ProjetMarcheDto mapProjectWithScore(
            Map<String, Object> project, Set<String> profileTokens, Integer freelancerExamGlobal) {
        int align = scoreAlignementProjet(project, profileTokens);
        int composite = computeCompositeMatchingScore(align, freelancerExamGlobal);
        String title = valueAsString(project.get("title"));
        String reason = buildMatchingReason(align, composite, freelancerExamGlobal);
        return ProjetMarcheDto.builder()
                .id(toLong(project.get("id")))
                .titre(title)
                .budget(toDouble(project.get("budget")))
                .dureeJours(toInt(project.get("duration")))
                .statut(valueAsString(project.get("status")))
                .scoreAlignementSkills(align)
                .scoreComposite(composite)
                .raison(reason)
                .build();
    }

    /**
     * Combine adéquation skills (65 %) et score global examens (35 %) lorsque l’historique existe ;
     * sinon le tri repose uniquement sur l’alignement compétences.
     */
    private static int computeCompositeMatchingScore(int alignementSkills, Integer freelancerExamGlobal) {
        if (freelancerExamGlobal == null) {
            return alignementSkills;
        }
        return (int) Math.round(alignementSkills * 0.65 + freelancerExamGlobal * 0.35);
    }

    private static String buildMatchingReason(int align, int composite, Integer examGlobal) {
        String base = align >= 75
                ? "Très bonne adéquation avec vos compétences principales."
                : align >= 45
                ? "Adéquation partielle : projet pertinent avec montée en compétence possible."
                : "Faible adéquation : intéressant surtout en apprentissage.";
        if (examGlobal == null) {
            return base + " (tri : compétences uniquement — pas encore d’historique d’examens.)";
        }
        return base + String.format(
                " Score combiné (skills + performance examens %d/100) : %d/100.",
                examGlobal, composite);
    }

    private static Set<String> buildFreelancerSkillTokens(List<Map<String, Object>> skills) {
        Set<String> tokens = new HashSet<>();
        for (Map<String, Object> s : skills) {
            addTokens(tokens, valueAsString(s.get("name")));
            addTokens(tokens, valueAsString(s.get("category")));
            addTokens(tokens, valueAsString(s.get("level")));
        }
        return tokens;
    }

    private static void addTokens(Set<String> tokens, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String up = value.toUpperCase(Locale.ROOT);
        tokens.add(up);
        for (String token : up.split("[^A-Z0-9]+")) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
    }

    /**
     * 0-100 : part des skills requises couvertes par le profil.
     */
    private static int scoreAlignementProjet(Map<String, Object> project, Set<String> profileTokens) {
        Object requiredSkills = project.get("requiredSkills");
        if (!(requiredSkills instanceof List<?> list) || list.isEmpty()) {
            return profileTokens.isEmpty() ? 0 : 20;
        }
        int matched = 0;
        int total = 0;
        for (Object req : list) {
            String reqText = extractRequiredSkillText(req);
            if (reqText == null || reqText.isBlank()) {
                continue;
            }
            total++;
            String up = reqText.toUpperCase(Locale.ROOT);
            boolean ok = profileTokens.stream().anyMatch(tok -> up.contains(tok) || tok.contains(up));
            if (ok) {
                matched++;
            }
        }
        if (total == 0) {
            return profileTokens.isEmpty() ? 0 : 20;
        }
        return (int) Math.round(100.0 * matched / total);
    }

    private static String extractRequiredSkillText(Object req) {
        if (req == null) {
            return null;
        }
        if (req instanceof Map<?, ?> m) {
            if (m.get("name") != null) {
                return m.get("name").toString();
            }
            if (m.get("skillName") != null) {
                return m.get("skillName").toString();
            }
            return m.toString();
        }
        return req.toString();
    }

    private static String valueAsString(Object o) {
        return o != null ? o.toString() : null;
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer toInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double toDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
