package tn.esprit.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.dto.CorrectionItemDto;
import tn.esprit.evaluation.dto.FormationRecoDto;
import tn.esprit.evaluation.dto.PassageExamenDto;
import tn.esprit.evaluation.dto.ReponseExamenRequest;
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.exception.ResourceNotFoundException;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.PassageExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PassageExamenService {

    private final ExamenRepository examenRepository;
    private final QuestionRepository questionRepository;
    private final PassageExamenRepository passageExamenRepository;
    private final CertificatService certificatService;
    private final FormationClient formationClient;
    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    /**
     * Soumet les réponses du freelancer, calcule le score et enregistre le passage.
     * Un freelancer ne peut passer un examen qu'une seule fois (unique constraint).
     */
    @Transactional
    public PassageExamenDto soumettreExamen(Long examenId, ReponseExamenRequest request) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new ResourceNotFoundException("Examen", examenId));

        ReponseExamenRequest.ModePassage mode = request.getMode() != null
                ? request.getMode()
                : ReponseExamenRequest.ModePassage.CERTIFIANT;

        if (mode == ReponseExamenRequest.ModePassage.CERTIFIANT
                && passageExamenRepository.existsByExamenIdAndFreelancerId(examenId, request.getFreelancerId())) {
            throw new RuntimeException("Ce freelancer a déjà passé cet examen.");
        }

        List<Question> questions = questionRepository.findByExamenIdOrderByOrdreAsc(examenId);
        if (questions.isEmpty())
            throw new RuntimeException("Cet examen n'a pas de questions.");

        List<String> reponses = request.getReponses();
        if (reponses == null || reponses.size() != questions.size())
            throw new RuntimeException("Nombre de réponses incorrect. Attendu: " + questions.size());

        int bonnes = 0;
        List<CorrectionItemDto> correction = new java.util.ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            String rep = i < reponses.size() ? (reponses.get(i) == null ? "" : reponses.get(i).toUpperCase().trim()) : "";
            if (rep.length() > 0) rep = rep.substring(0, 1);
            String bonne = questions.get(i).getBonneReponse() != null ? questions.get(i).getBonneReponse().toUpperCase().trim() : "";
            boolean ok = bonne.equalsIgnoreCase(rep);
            if (ok)
                bonnes++;
            if (mode == ReponseExamenRequest.ModePassage.ENTRAINEMENT) {
                correction.add(CorrectionItemDto.builder()
                        .ordre(questions.get(i).getOrdre())
                        .enonce(questions.get(i).getEnonce())
                        .reponseChoisie(rep)
                        .bonneReponse(bonne)
                        .correct(ok)
                        .build());
            }
        }
        int score = (int) Math.round(100.0 * bonnes / questions.size());
        int erreurs = questions.size() - bonnes;
        PassageExamen.ResultatExamen resultat = score >= examen.getSeuilReussi()
                ? PassageExamen.ResultatExamen.REUSSI
                : PassageExamen.ResultatExamen.ECHOUE;

        PassageExamenDto dto;

        if (mode == ReponseExamenRequest.ModePassage.CERTIFIANT) {
            PassageExamen passage = PassageExamen.builder()
                    .freelancerId(request.getFreelancerId())
                    .examen(examen)
                    .score(score)
                    .resultat(resultat)
                    .build();
            passage = passageExamenRepository.save(passage);
            dto = PassageExamenDto.fromEntity(passage);
            // Certificat auto-généré si réussi : créé et renvoyé directement dans la réponse
            if (passage.getResultat() == PassageExamen.ResultatExamen.REUSSI) {
                dto.setCertificat(certificatService.creerSiReussi(passage));
            }
        } else {
            // ENTRAINEMENT : pas de sauvegarde, pas de certificat
            dto = PassageExamenDto.builder()
                    .id(null)
                    .freelancerId(request.getFreelancerId())
                    .examenId(examen.getId())
                    .examenTitre(examen.getTitre())
                    .score(score)
                    .resultat(resultat)
                    .datePassage(java.time.LocalDateTime.now())
                    .build();
            dto.setCorrection(correction);
        }

        dto.setMode(mode);
        dto.setTotalQuestions(questions.size());
        dto.setBonnesReponses(bonnes);
        dto.setAnalyseErreurs(buildAnalyseErreurs(questions, reponses));
        dto.setMessageFeedback(buildMessageFeedback(score, examen.getSeuilReussi(), erreurs));

        // Feedback automatique : formations recommandées (en lien avec Formation) — pour les deux modes
        List<Map<String, Object>> formations = formationClient.getRecommandationsForFreelancer(request.getFreelancerId());
        dto.setFormationsRecommandees(formations.stream().map(f -> FormationRecoDto.builder()
                .id(f.get("id") != null ? Long.valueOf(f.get("id").toString()) : null)
                .titre(f.get("titre") != null ? f.get("titre").toString() : null)
                .typeFormation(f.get("typeFormation") != null ? f.get("typeFormation").toString() : null)
                .niveau(f.get("niveau") != null ? f.get("niveau").toString() : null)
                .statut(f.get("statut") != null ? f.get("statut").toString() : null)
                .dateDebut(f.get("dateDebut") != null ? f.get("dateDebut").toString() : null)
                .dateFin(f.get("dateFin") != null ? f.get("dateFin").toString() : null)
                .lienDirect(buildFormationLink(f.get("id")))
                .build()).collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<PassageExamenDto> findByFreelancer(Long freelancerId) {
        return passageExamenRepository.findByFreelancerIdWithExamen(freelancerId).stream()
                .map(PassageExamenDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PassageExamenDto> findByExamen(Long examenId) {
        return passageExamenRepository.findByExamenIdWithExamen(examenId).stream()
                .map(PassageExamenDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PassageExamenDto getPassageByFreelancerAndExamen(Long examenId, Long freelancerId) {
        return passageExamenRepository.findByExamenIdAndFreelancerIdWithExamen(examenId, freelancerId)
                .map(PassageExamenDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Passage examen non trouvé pour cet examen et ce freelancer."));
    }

    private String buildFormationLink(Object formationIdRaw) {
        if (formationIdRaw == null) return null;
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
        return base + "/formations/" + formationIdRaw;
    }

    private List<String> buildAnalyseErreurs(List<Question> questions, List<String> reponses) {
        List<String> axes = new java.util.ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            String rep = i < reponses.size() && reponses.get(i) != null ? reponses.get(i).trim().toUpperCase() : "";
            if (!rep.isEmpty()) rep = rep.substring(0, 1);
            String bonne = questions.get(i).getBonneReponse() != null ? questions.get(i).getBonneReponse().trim().toUpperCase() : "";
            if (!Objects.equals(rep, bonne)) {
                String enonce = questions.get(i).getEnonce() != null ? questions.get(i).getEnonce().trim() : "Question " + (i + 1);
                axes.add("Revoir: " + shorten(enonce, 90));
            }
        }
        if (axes.isEmpty()) axes.add("Aucun axe critique: excellentes réponses.");
        return axes.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private String buildMessageFeedback(int score, Integer seuil, int erreurs) {
        int seuilSafe = seuil != null ? seuil : 60;
        if (score >= seuilSafe) {
            if (score >= 85) return "Excellent resultat. Continue avec des formations avancees pour renforcer ton expertise.";
            return "Bon resultat. Tu es admis, mais quelques revisions ciblees peuvent te faire passer au niveau superieur.";
        }
        if (erreurs >= 3) {
            return "Resultat insuffisant. Recommande: refaire en mode entrainement et suivre les formations suggerees avant une nouvelle tentative.";
        }
        return "Resultat proche du seuil. Revois les points faibles identifies puis repasse en mode certifiant.";
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }
}
