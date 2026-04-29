package tn.esprit.formation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.formation.client.EvaluationClient;
import tn.esprit.formation.dto.InscriptionDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.Inscription;
import tn.esprit.formation.repository.FormationRepository;
import tn.esprit.formation.repository.InscriptionRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscriptionServiceTest {

    @Mock
    private InscriptionRepository inscriptionRepository;
    @Mock
    private FormationRepository formationRepository;
    @Mock
    private EvaluationClient evaluationClient;

    @InjectMocks
    private InscriptionService inscriptionService;

    @Test
    void findByFreelancer_delegatesToRepository() {
        Formation f = openFormation(1L, 10);
        Inscription ins = Inscription.builder().id(2L).freelancerId(7L).formation(f).build();
        when(inscriptionRepository.findByFreelancerId(7L)).thenReturn(List.of(ins));

        assertThat(inscriptionService.findByFreelancer(7L)).hasSize(1);
    }

    @Test
    void findByStatutEnAttente_delegatesToRepository() {
        Formation f = openFormation(1L, 10);
        Inscription ins = Inscription.builder().id(3L).freelancerId(1L).formation(f).statut(Inscription.StatutInscription.EN_ATTENTE).build();
        when(inscriptionRepository.findByStatutOrderByDateInscriptionDesc(Inscription.StatutInscription.EN_ATTENTE))
                .thenReturn(List.of(ins));

        assertThat(inscriptionService.findByStatutEnAttente()).hasSize(1);
    }

    @Test
    void findByFormation_mapsDtos() {
        Formation f = openFormation(1L, 10);
        Inscription ins = Inscription.builder()
                .id(1L)
                .freelancerId(5L)
                .formation(f)
                .statut(Inscription.StatutInscription.EN_ATTENTE)
                .build();
        when(inscriptionRepository.findByFormationId(1L)).thenReturn(List.of(ins));

        List<InscriptionDto> out = inscriptionService.findByFormation(1L);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getFreelancerId()).isEqualTo(5L);
    }

    @Test
    void inscrire_throwsWhenAlreadyRegistered() {
        Formation f = openFormation(1L, 10);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(f));
        when(inscriptionRepository.existsByFormationIdAndFreelancerId(1L, 9L)).thenReturn(true);

        assertThatThrownBy(() -> inscriptionService.inscrire(1L, 9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("déjà inscrit");
    }

    @Test
    void inscrire_throwsWhenFull() {
        Formation f = openFormation(1L, 1);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(f));
        when(inscriptionRepository.existsByFormationIdAndFreelancerId(1L, 9L)).thenReturn(false);
        Inscription existing = Inscription.builder().id(1L).freelancerId(2L).formation(f).build();
        when(inscriptionRepository.findByFormationId(1L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> inscriptionService.inscrire(1L, 9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("complète");
    }

    @Test
    void inscrire_throwsWhenNotOpen() {
        Formation f = Formation.builder()
                .id(1L)
                .titre("X")
                .statut(Formation.StatutFormation.TERMINEE)
                .capaciteMax(10)
                .build();
        when(formationRepository.findById(1L)).thenReturn(Optional.of(f));
        when(inscriptionRepository.existsByFormationIdAndFreelancerId(1L, 9L)).thenReturn(false);
        when(inscriptionRepository.findByFormationId(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> inscriptionService.inscrire(1L, 9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("non ouverte");
    }

    @Test
    void inscrire_throwsWhenCertificatMissing() {
        Formation f = openFormation(1L, 10);
        f.setExamenRequisId(77L);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(f));
        when(inscriptionRepository.existsByFormationIdAndFreelancerId(1L, 9L)).thenReturn(false);
        when(inscriptionRepository.findByFormationId(1L)).thenReturn(Collections.emptyList());
        when(evaluationClient.getCertificatsByFreelancer(9L)).thenReturn(Collections.emptyList());
        when(evaluationClient.getExamenTitre(77L)).thenReturn("Examen Secu");

        assertThatThrownBy(() -> inscriptionService.inscrire(1L, 9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("certificat");
    }

    @Test
    void inscrire_succeedsWhenCertificatPresent() {
        Formation f = openFormation(1L, 10);
        f.setExamenRequisId(77L);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(f));
        when(inscriptionRepository.existsByFormationIdAndFreelancerId(1L, 9L)).thenReturn(false);
        when(inscriptionRepository.findByFormationId(1L)).thenReturn(Collections.emptyList());
        when(evaluationClient.getCertificatsByFreelancer(9L)).thenReturn(List.of(Map.of("examenId", 77)));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(i -> {
            Inscription ins = i.getArgument(0);
            ins.setId(50L);
            return ins;
        });

        InscriptionDto out = inscriptionService.inscrire(1L, 9L);

        assertThat(out.getId()).isEqualTo(50L);
        assertThat(out.getFreelancerId()).isEqualTo(9L);
    }

    @Test
    void inscrire_acceptsCertificatWithStringExamenId() {
        Formation f = openFormation(1L, 10);
        f.setExamenRequisId(77L);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(f));
        when(inscriptionRepository.existsByFormationIdAndFreelancerId(1L, 9L)).thenReturn(false);
        when(inscriptionRepository.findByFormationId(1L)).thenReturn(Collections.emptyList());
        when(evaluationClient.getCertificatsByFreelancer(9L)).thenReturn(List.of(Map.of("examenId", "77")));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(i -> {
            Inscription ins = i.getArgument(0);
            ins.setId(52L);
            return ins;
        });

        InscriptionDto out = inscriptionService.inscrire(1L, 9L);

        assertThat(out.getId()).isEqualTo(52L);
    }

    @Test
    void inscrire_withoutExamenRequis_saves() {
        Formation f = openFormation(1L, 10);
        when(formationRepository.findById(1L)).thenReturn(Optional.of(f));
        when(inscriptionRepository.existsByFormationIdAndFreelancerId(1L, 9L)).thenReturn(false);
        when(inscriptionRepository.findByFormationId(1L)).thenReturn(Collections.emptyList());
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(i -> {
            Inscription ins = i.getArgument(0);
            ins.setId(51L);
            return ins;
        });

        InscriptionDto out = inscriptionService.inscrire(1L, 9L);

        assertThat(out.getId()).isEqualTo(51L);
    }

    @Test
    void validerInscription_setsStatutValidee() {
        Formation f = openFormation(1L, 10);
        Inscription ins = Inscription.builder().id(8L).freelancerId(3L).formation(f).statut(Inscription.StatutInscription.EN_ATTENTE).build();
        when(inscriptionRepository.findById(8L)).thenReturn(Optional.of(ins));
        when(inscriptionRepository.save(ins)).thenReturn(ins);

        InscriptionDto out = inscriptionService.validerInscription(8L);

        assertThat(out.getStatut()).isEqualTo(Inscription.StatutInscription.VALIDEE);
    }

    @Test
    void annulerInscription_setsStatutAnnulee() {
        Formation f = openFormation(1L, 10);
        Inscription ins = Inscription.builder().id(8L).freelancerId(3L).formation(f).statut(Inscription.StatutInscription.EN_ATTENTE).build();
        when(inscriptionRepository.findById(8L)).thenReturn(Optional.of(ins));

        inscriptionService.annulerInscription(8L);

        assertThat(ins.getStatut()).isEqualTo(Inscription.StatutInscription.ANNULEE);
        verify(inscriptionRepository).save(ins);
    }

    private static Formation openFormation(Long id, int capacite) {
        return Formation.builder()
                .id(id)
                .titre("F" + id)
                .statut(Formation.StatutFormation.OUVERTE)
                .capaciteMax(capacite)
                .build();
    }
}
