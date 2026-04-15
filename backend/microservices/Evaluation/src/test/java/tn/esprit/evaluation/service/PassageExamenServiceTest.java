package tn.esprit.evaluation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.dto.CertificatDto;
import tn.esprit.evaluation.dto.PassageExamenDto;
import tn.esprit.evaluation.dto.ReponseExamenRequest;
import tn.esprit.evaluation.dto.RiskEvaluationDto;
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.ParcoursInclusion;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.domain.NiveauDifficulteQuestion;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.PassageExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;
import tn.esprit.evaluation.service.FreelancerQuestionTraceService;
import tn.esprit.evaluation.service.FreelancerThemeScoreLogService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassageExamenServiceTest {

    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private PassageExamenRepository passageExamenRepository;
    @Mock
    private CertificatService certificatService;
    @Mock
    private CertificatCarriereService certificatCarriereService;
    @Mock
    private FormationClient formationClient;

    @Mock
    private ParcoursApprenantService parcoursApprenantService;

    @Mock
    private FreelancerQuestionTraceService freelancerQuestionTraceService;

    @Mock
    private FreelancerThemeScoreLogService freelancerThemeScoreLogService;

    @InjectMocks
    private PassageExamenService service;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:4200");
        RiskEvaluationDto risk = RiskEvaluationDto.builder().scoreRisque(10).niveau("FAIBLE").messageApprenant("ok").build();
        when(parcoursApprenantService.evaluerRisque(any(), any())).thenReturn(risk);
        /* Appelé seulement si bonnes < total (parcours avec erreurs) : lenient évite UnnecessaryStubbing si score parfait. */
        lenient().when(parcoursApprenantService.proposerModulesRevision(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void soumettreExamen_modeEntrainement_returnsCorrectionWithoutSaving() {
        Examen examen = Examen.builder().id(11L).formationId(3L).titre("QCM IA").seuilReussi(60).build();
        List<Question> questions = questionsAlternatingAB(10);
        List<String> reponses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            reponses.add(i % 2 == 0 ? "A" : "D");
        }
        ReponseExamenRequest request = ReponseExamenRequest.builder()
                .freelancerId(5L)
                .mode(ReponseExamenRequest.ModePassage.ENTRAINEMENT)
                .reponses(reponses)
                .build();
        when(examenRepository.findById(11L)).thenReturn(Optional.of(examen));
        when(questionRepository.findByExamenIdOrderByOrdreAsc(11L)).thenReturn(questions);
        when(formationClient.getRecommandationsForFreelancer(5L)).thenReturn(List.of(
                Map.of("id", 9, "titre", "DevOps", "typeFormation", "DEVOPS")
        ));

        PassageExamenDto result = service.soumettreExamen(11L, request);

        assertEquals(ReponseExamenRequest.ModePassage.ENTRAINEMENT, result.getMode());
        assertNull(result.getId());
        assertEquals(50, result.getScore());
        assertEquals(20, result.getPointsMax());
        assertEquals(10, result.getPointsObtenus());
        assertNotNull(result.getCorrection());
        assertEquals(10, result.getCorrection().size());
        assertNull(result.getCertificat());
        verify(certificatCarriereService, never()).enrichirApresCertificat(any(), any(), any());
        assertNotNull(result.getAnalyseErreurs());
        assertNotNull(result.getMessageFeedback());
        assertEquals("http://localhost:4200/formations/9", result.getFormationsRecommandees().get(0).getLienDirect());
        verify(passageExamenRepository, never()).save(any());
    }

    @Test
    void soumettreExamen_modeCertifiant_reussi_savesAndAttachesCertificate() {
        Examen examen = Examen.builder().id(12L).formationId(4L).titre("QCM Cloud").seuilReussi(60).build();
        List<Question> questions = questionsAlternatingAB(10);
        List<String> reponses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            reponses.add(i % 2 == 0 ? "A" : "B");
        }
        ReponseExamenRequest request = ReponseExamenRequest.builder()
                .freelancerId(6L)
                .mode(ReponseExamenRequest.ModePassage.CERTIFIANT)
                .reponses(reponses)
                .build();
        PassageExamen saved = PassageExamen.builder()
                .id(99L)
                .freelancerId(6L)
                .examen(examen)
                .score(100)
                .resultat(PassageExamen.ResultatExamen.REUSSI)
                .datePassage(LocalDateTime.now())
                .build();
        CertificatDto certificat = CertificatDto.builder().id(7L).numeroCertificat("CERT-XYZ").score(100).build();

        when(examenRepository.findById(12L)).thenReturn(Optional.of(examen));
        when(questionRepository.findByExamenIdOrderByOrdreAsc(12L)).thenReturn(questions);
        when(passageExamenRepository.existsByExamenIdAndFreelancerId(12L, 6L)).thenReturn(false);
        when(passageExamenRepository.save(any(PassageExamen.class))).thenReturn(saved);
        when(certificatService.creerSiReussi(saved)).thenReturn(certificat);
        when(formationClient.getRecommandationsForFreelancer(6L)).thenReturn(List.of());

        PassageExamenDto result = service.soumettreExamen(12L, request);

        assertEquals(ReponseExamenRequest.ModePassage.CERTIFIANT, result.getMode());
        assertEquals(99L, result.getId());
        assertEquals(100, result.getScore());
        assertEquals(20, result.getPointsMax());
        assertEquals(20, result.getPointsObtenus());
        assertNotNull(result.getCertificat());
        assertEquals("CERT-XYZ", result.getCertificat().getNumeroCertificat());
        assertNotNull(result.getCorrection());
        assertEquals(10, result.getCorrection().size());
        verify(passageExamenRepository).save(any(PassageExamen.class));
        verify(certificatCarriereService).enrichirApresCertificat(any(PassageExamenDto.class), eq(saved), eq(examen));
    }

    @Test
    void soumettreExamen_normaliseFormatReponsePourScore() {
        Examen examen = Examen.builder().id(13L).formationId(5L).titre("QCM").seuilReussi(60).build();
        List<Question> questions = List.of(
                Question.builder().id(1L).ordre(1).enonce("Q1").bonneReponse("A").parcoursInclusion(ParcoursInclusion.COMMUN).build(),
                Question.builder().id(2L).ordre(2).enonce("Q2").bonneReponse("B").parcoursInclusion(ParcoursInclusion.COMMUN).build(),
                Question.builder().id(3L).ordre(3).enonce("Q3").bonneReponse("C").parcoursInclusion(ParcoursInclusion.COMMUN).build(),
                Question.builder().id(4L).ordre(4).enonce("Q4").bonneReponse("D").parcoursInclusion(ParcoursInclusion.COMMUN).build(),
                Question.builder().id(5L).ordre(5).enonce("Q5").bonneReponse("A").parcoursInclusion(ParcoursInclusion.COMMUN).build(),
                Question.builder().id(6L).ordre(6).enonce("Q6").bonneReponse("B").parcoursInclusion(ParcoursInclusion.COMMUN).build(),
                Question.builder().id(7L).ordre(7).enonce("Q7").bonneReponse("C").parcoursInclusion(ParcoursInclusion.COMMUN).build()
        );
        List<String> reponses = List.of("(a)", " b) ", "C", "d", " A ", "B", "x");
        ReponseExamenRequest request = ReponseExamenRequest.builder()
                .freelancerId(7L)
                .mode(ReponseExamenRequest.ModePassage.ENTRAINEMENT)
                .reponses(reponses)
                .build();
        when(examenRepository.findById(13L)).thenReturn(Optional.of(examen));
        when(questionRepository.findByExamenIdOrderByOrdreAsc(13L)).thenReturn(questions);
        when(formationClient.getRecommandationsForFreelancer(7L)).thenReturn(List.of());

        PassageExamenDto result = service.soumettreExamen(13L, request);

        assertEquals(6, result.getBonnesReponses());
        assertEquals(85, result.getScore());
        assertEquals(85, result.getScoreSansPonderation());
        assertEquals(14, result.getPointsMax());
        assertEquals(12, result.getPointsObtenus());
    }

    @Test
    void soumettreExamen_scorePondere_differeDuTauxBrut() {
        Examen examen = Examen.builder().id(14L).formationId(6L).titre("QCM").seuilReussi(50).build();
        List<Question> questions = List.of(
                Question.builder().id(1L).ordre(0).enonce("F").bonneReponse("A")
                        .parcoursInclusion(ParcoursInclusion.COMMUN).niveauDifficulte(NiveauDifficulteQuestion.FACILE).build(),
                Question.builder().id(2L).ordre(1).enonce("D").bonneReponse("A")
                        .parcoursInclusion(ParcoursInclusion.COMMUN).niveauDifficulte(NiveauDifficulteQuestion.DIFFICILE).build(),
                Question.builder().id(3L).ordre(2).enonce("M").bonneReponse("A")
                        .parcoursInclusion(ParcoursInclusion.COMMUN).niveauDifficulte(NiveauDifficulteQuestion.MOYEN).build()
        );
        /* Ratée FACILE (poids 1), bonnes DIFFICILE + MOYEN (3+2) → pondéré > taux brut. */
        List<String> reponses = List.of("B", "A", "A");
        ReponseExamenRequest request = ReponseExamenRequest.builder()
                .freelancerId(8L)
                .mode(ReponseExamenRequest.ModePassage.ENTRAINEMENT)
                .reponses(reponses)
                .build();
        when(examenRepository.findById(14L)).thenReturn(Optional.of(examen));
        when(questionRepository.findByExamenIdOrderByOrdreAsc(14L)).thenReturn(questions);
        when(formationClient.getRecommandationsForFreelancer(8L)).thenReturn(List.of());

        PassageExamenDto result = service.soumettreExamen(14L, request);

        assertEquals(2, result.getBonnesReponses());
        assertEquals(66, result.getScoreSansPonderation());
        assertEquals(3, result.getCorrection().get(1).getPoids());
        assertEquals(83, result.getScore());
        assertEquals(6, result.getPointsMax());
        assertEquals(5, result.getPointsObtenus());
    }

    private static List<Question> questionsAlternatingAB(int n) {
        List<Question> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            String bonne = i % 2 == 1 ? "A" : "B";
            list.add(Question.builder()
                    .id((long) i)
                    .ordre(i)
                    .enonce("Question " + i)
                    .bonneReponse(bonne)
                    .parcoursInclusion(ParcoursInclusion.COMMUN)
                    .build());
        }
        return list;
    }
}
