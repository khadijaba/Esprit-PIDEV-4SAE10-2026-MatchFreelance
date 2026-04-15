package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.entity.FreelancerQuestionTrace;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.repository.FreelancerQuestionTraceRepository;
import tn.esprit.evaluation.util.QcmReponseNormalizer;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FreelancerQuestionTraceService {

    private final FreelancerQuestionTraceRepository traceRepository;

    @Transactional
    public void enregistrerResultats(Long freelancerId, List<Question> questions, List<String> reponses) {
        if (freelancerId == null || questions == null) {
            return;
        }
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (q.getId() == null) {
                continue;
            }
            String brute = i < reponses.size() ? reponses.get(i) : null;
            boolean ok = QcmReponseNormalizer.reponseQcmCorrecte(brute, q.getBonneReponse());

            FreelancerQuestionTrace trace = traceRepository
                    .findByFreelancerIdAndQuestion_Id(freelancerId, q.getId())
                    .orElseGet(() -> FreelancerQuestionTrace.builder()
                            .freelancerId(freelancerId)
                            .question(q)
                            .wrongCount(0)
                            .correctCount(0)
                            .lastWasWrong(false)
                            .build());
            if (ok) {
                trace.setCorrectCount(trace.getCorrectCount() + 1);
                trace.setLastWasWrong(false);
            } else {
                trace.setWrongCount(trace.getWrongCount() + 1);
                trace.setLastWasWrong(true);
            }
            trace.setUpdatedAt(LocalDateTime.now());
            traceRepository.save(trace);
        }
    }

    public List<Long> listerQuestionIdsRatéesHistoriquement(Long freelancerId, Long examenId) {
        return traceRepository.findQuestionIdsRatéesHistoriquement(freelancerId, examenId);
    }
}
