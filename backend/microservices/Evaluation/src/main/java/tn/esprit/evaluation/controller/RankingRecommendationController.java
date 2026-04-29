package tn.esprit.evaluation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.evaluation.dto.FreelancerProjectMatchingDto;
import tn.esprit.evaluation.dto.FreelancerRankingDto;
import tn.esprit.evaluation.service.RankingRecommendationService;

import java.util.List;
import java.util.Map;

/**
 * Ranking global + top performers + recommandation projets basée score/skills.
 */
@RestController
@RequestMapping("/api/examens/ranking")
@RequiredArgsConstructor
public class RankingRecommendationController {

    private final RankingRecommendationService rankingRecommendationService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("ranking", "deployed", "paths", "global, top-performers, project-matching"));
    }

    @GetMapping("/global")
    public ResponseEntity<List<FreelancerRankingDto>> getGlobalRanking() {
        return ResponseEntity.ok(rankingRecommendationService.getGlobalRanking());
    }

    @GetMapping("/top-performers")
    public ResponseEntity<List<FreelancerRankingDto>> getTopPerformers(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(rankingRecommendationService.getTopPerformers(limit));
    }

    @GetMapping("/freelancer/{freelancerId}/project-matching")
    public ResponseEntity<FreelancerProjectMatchingDto> getProjectMatching(
            @PathVariable Long freelancerId,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(rankingRecommendationService.recommendProjects(freelancerId, limit));
    }
}
