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
import tn.esprit.evaluation.entity.Examen;
import tn.esprit.evaluation.entity.PassageExamen;
import tn.esprit.evaluation.entity.Question;
import tn.esprit.evaluation.repository.ExamenRepository;
import tn.esprit.evaluation.repository.PassageExamenRepository;
import tn.esprit.evaluation.repository.QuestionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
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
    private FormationClient formationClient;

    @InjectMocks
    private PassageExamenService service;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:4200");
    }

    @Test
    void soumettreExamen_modeEntrainement_returnsCorrectionWithoutSaving() {
        Examen examen = Examen.builder().id(11L).formationId(3L).titre("QCM IA").seuilReussi(60).build();
        List<Question> questions = List.of(
                question(1, "Question 1", "A"),
                question(2, "Question 2", "B")
        );
        ReponseExamenRequest request = ReponseExamenRequest.builder()
                .freelancerId(5L)
                .mode(ReponseExamenRequest.ModePassage.ENTRAINEMENT)
                .reponses(List.of("A", "D"))
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
        assertNotNull(result.getCorrection());
        assertEquals(2, result.getCorrection().size());
        assertNull(result.getCertificat());
        assertNotNull(result.getAnalyseErreurs());
        assertNotNull(result.getMessageFeedback());
        assertEquals("http://localhost:4200/formations/9", result.getFormationsRecommandees().get(0).getLienDirect());
        verify(passageExamenRepository, never()).save(any());
    }

    @Test
    void soumettreExamen_modeCertifiant_reussi_savesAndAttachesCertificate() {
        Examen examen = Examen.builder().id(12L).formationId(4L).titre("QCM Cloud").seuilReussi(60).build();
        List<Question> questions = List.of(
                question(1, "Question 1", "A"),
                question(2, "Question 2", "B")
        );
        ReponseExamenRequest request = ReponseExamenRequest.builder()
                .freelancerId(6L)
                .mode(ReponseExamenRequest.ModePassage.CERTIFIANT)
                .reponses(List.of("A", "B"))
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
        assertNotNull(result.getCertificat());
        assertEquals("CERT-XYZ", result.getCertificat().getNumeroCertificat());
        verify(passageExamenRepository).save(any(PassageExamen.class));
    }

    private Question question(int ordre, String enonce, String bonne) {
        return Question.builder()
                .ordre(ordre)
                .enonce(enonce)
                .bonneReponse(bonne)
                .build();
    }
}
