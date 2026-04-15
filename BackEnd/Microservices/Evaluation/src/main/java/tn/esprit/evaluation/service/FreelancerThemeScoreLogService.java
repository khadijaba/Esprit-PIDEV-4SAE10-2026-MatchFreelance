package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.entity.FreelancerThemeScoreLog;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.repository.FreelancerThemeScoreLogRepository;
import tn.esprit.evaluation.util.QcmReponseNormalizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FreelancerThemeScoreLogService {

    private final FreelancerThemeScoreLogRepository logRepository;

    @Transactional
    public void enregistrerScoresParTheme(Long freelancerId, Long examenId, List<Question> questions, List<String> reponses) {
        if (freelancerId == null || examenId == null || questions == null) {
            return;
        }
        Map<String, Integer> bonParTheme = new HashMap<>();
        Map<String, Integer> totalParTheme = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String theme = q.getTheme();
            if (theme == null || theme.isBlank()) {
                theme = "Sans thème";
            } else {
                theme = theme.trim();
            }
            String brute = i < reponses.size() ? reponses.get(i) : null;
            boolean ok = QcmReponseNormalizer.reponseQcmCorrecte(brute, q.getBonneReponse());

            totalParTheme.merge(theme, 1, Integer::sum);
            if (ok) {
                bonParTheme.merge(theme, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> e : totalParTheme.entrySet()) {
            String theme = e.getKey();
            int total = e.getValue();
            int bon = bonParTheme.getOrDefault(theme, 0);
            int pct = total > 0 ? QcmReponseNormalizer.pourcentagePlancher(bon, total) : 0;
            logRepository.save(FreelancerThemeScoreLog.builder()
                    .freelancerId(freelancerId)
                    .examenId(examenId)
                    .theme(theme)
                    .scorePercent(pct)
                    .build());
        }
    }
}
