package esprit.project.Service;

import esprit.project.Repositories.ProjectRepository;
import esprit.project.dto.ProjectEffortEstimateDto;
import esprit.project.entities.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProjectEffortEstimateService {

    public static final String METHOD_VERSION = "heuristic-effort-v1";

    /** Mots-clés qui augmentent la charge estimée (FR / EN courants). */
    private static final Set<String> HEAVY_KEYWORDS = Set.of(
            "microservices", "microservice", "migration", "kubernetes", "k8s", "devops", "ci/cd", "pipeline",
            "refonte", "legacy", "architecture", "sécurité", "security", "conformité", "rgpd", "gdpr",
            "temps réel", "real-time", "websocket", "blockchain", "machine learning", "deep learning",
            "intégration", "integration", "erp", "crm", "multi-tenant", "scalabilité", "scalability",
            "performance", "optimisation", "optimization", "audit", "recette", "tests e2e", "e2e",
            "internationalisation", "i18n", "accessibilité", "accessibility", "a11y"
    );

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public ProjectEffortEstimateDto estimate(Long projectId) {
        Project p = projectRepository.findById(projectId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));

        String title = Optional.ofNullable(p.getTitle()).orElse("");
        String desc = Optional.ofNullable(p.getDescription()).orElse("");
        int wordCount = countWords(title) + countWords(desc);
        int skillCount = Optional.ofNullable(p.getRequiredSkills()).map(List::size).orElse(0);
        int duration = p.getDuration() != null && p.getDuration() > 0 ? p.getDuration() : 1;

        int keywordBoost = countKeywordHits(title + " " + desc);
        double base = 6.0 + wordCount * 0.22 + skillCount * 2.8 + keywordBoost * 6.5;
        if (wordCount > 120) {
            base *= 1.12;
        }
        if (wordCount > 220) {
            base *= 1.08;
        }

        int man = (int) Math.round(clamp(base, 4, 320));
        int low = Math.max(2, (int) Math.round(man * 0.72));
        int high = Math.max(man + 1, (int) Math.round(man * 1.38));

        int impliedOneFte = duration;
        double fteNeed = man / (double) Math.max(1, impliedOneFte);

        List<String> flags = new ArrayList<>();
        if (wordCount < 40) {
            flags.add("DESCRIPTION_LEGERE_CHARGE_INCERTAINE");
        }
        if (skillCount == 0) {
            flags.add("PAS_DE_COMPETENCES_POUR_AFFINER");
        }
        if (fteNeed > 1.25) {
            flags.add("EFFORT_SUPERIEUR_A_UNE_RESSOURCE_SUR_LA_DUREE");
        }
        if (fteNeed < 0.35 && man > 15) {
            flags.add("DUREE_CALENDAIRE_LARGE_VS_EFFORT");
        }

        String summary = String.format(
                "Charge indicative ~ %d j-h (%d–%d). Durée annoncée %d j ; besoin ~ %.2f ETP plein sur cette période.",
                man, low, high, duration, fteNeed
        );

        return ProjectEffortEstimateDto.builder()
                .projectId(projectId)
                .estimatedManDays(man)
                .estimatedManDaysLow(low)
                .estimatedManDaysHigh(high)
                .declaredDurationDays(duration)
                .impliedCapacityOneFteDays(impliedOneFte)
                .fteRequiredVsDeclared(round2(fteNeed))
                .flags(List.copyOf(flags))
                .summary(summary)
                .methodVersion(METHOD_VERSION)
                .build();
    }

    private static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private static int countKeywordHits(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String t = text.toLowerCase(Locale.ROOT);
        int n = 0;
        for (String k : HEAVY_KEYWORDS) {
            if (t.contains(k.toLowerCase(Locale.ROOT))) {
                n++;
            }
        }
        return n;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
