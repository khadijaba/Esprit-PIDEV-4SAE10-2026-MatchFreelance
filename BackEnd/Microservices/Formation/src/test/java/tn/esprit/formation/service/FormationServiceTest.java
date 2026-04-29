package tn.esprit.formation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.formation.dto.FormationDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.TypeFormation;
import tn.esprit.formation.repository.FormationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock
    private FormationRepository formationRepository;
    @Mock
    private FormationNotificationService notificationService;

    @InjectMocks
    private FormationService formationService;

    @Test
    void getRecommandationsForFreelancer_returnsOpenFormations_whenFreelancerIdValid() {
        Formation devops = formation("DevOps Mastery", TypeFormation.DEVOPS);
        Formation ai = formation("IA Avancee", TypeFormation.AI);
        Formation data = formation("Data Science", TypeFormation.DATA_SCIENCE);
        when(formationRepository.findFormationsOuvertes()).thenReturn(List.of(devops, ai, data));

        List<FormationDto> recos = formationService.getRecommandationsForFreelancer(7L);

        assertThat(recos).hasSize(3);
        assertThat(recos).extracting(FormationDto::getTitre)
                .containsExactlyInAnyOrder("DevOps Mastery", "IA Avancee", "Data Science");
    }

    @Test
    void getRecommandationsForFreelancer_returnsEmpty_whenFreelancerIdNullOrNonPositive() {
        assertThat(formationService.getRecommandationsForFreelancer(null)).isEmpty();
        assertThat(formationService.getRecommandationsForFreelancer(0L)).isEmpty();
        verify(formationRepository, never()).findFormationsOuvertes();
    }

    @Test
    void findById_returnsDto_whenFound() {
        Formation f = formation("X", TypeFormation.WEB_DEVELOPMENT);
        f.setId(10L);
        when(formationRepository.findById(10L)).thenReturn(Optional.of(f));

        FormationDto dto = formationService.findById(10L);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getTitre()).isEqualTo("X");
    }

    @Test
    void findById_throws_whenMissing() {
        when(formationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formationService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Formation non trouvée");
    }

    @Test
    void findOuvertes_delegatesToRepository() {
        when(formationRepository.findFormationsOuvertes()).thenReturn(List.of(formation("Open", TypeFormation.AI)));

        List<FormationDto> out = formationService.findOuvertes();

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getTitre()).isEqualTo("Open");
    }

    @Test
    void findAll_mapsFromRepository() {
        when(formationRepository.findAll()).thenReturn(List.of(formation("A", TypeFormation.AI)));

        List<FormationDto> all = formationService.findAll();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitre()).isEqualTo("A");
    }

    @Test
    void create_savesNotifiesAndReturnsDto() {
        FormationDto input = FormationDto.builder()
                .titre("New")
                .typeFormation(TypeFormation.DEVOPS)
                .description("d")
                .dureeHeures(5)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(1))
                .capaciteMax(10)
                .build();
        when(formationRepository.save(any(Formation.class))).thenAnswer(i -> {
            Formation arg = i.getArgument(0);
            arg.setId(42L);
            return arg;
        });

        FormationDto out = formationService.create(input);

        assertThat(out.getId()).isEqualTo(42L);
        ArgumentCaptor<FormationDto> captor = ArgumentCaptor.forClass(FormationDto.class);
        verify(notificationService).notifyNewFormation(captor.capture());
        assertThat(captor.getValue().getTitre()).isEqualTo("New");
    }

    @Test
    void update_throwsWhenFormationMissing() {
        when(formationRepository.findById(99L)).thenReturn(Optional.empty());
        FormationDto patch = FormationDto.builder()
                .titre("X")
                .dureeHeures(1)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> formationService.update(99L, patch))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Formation non trouvée");
    }

    @Test
    void update_patchesExisting() {
        Formation existing = formation("Old", TypeFormation.AI);
        existing.setId(3L);
        when(formationRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(formationRepository.save(existing)).thenReturn(existing);

        FormationDto patch = FormationDto.builder()
                .titre("NewTitle")
                .typeFormation(TypeFormation.CYBERSECURITY)
                .description("desc")
                .dureeHeures(20)
                .dateDebut(LocalDate.of(2026, 1, 1))
                .dateFin(LocalDate.of(2026, 2, 1))
                .capaciteMax(30)
                .statut(Formation.StatutFormation.EN_COURS)
                .build();

        FormationDto out = formationService.update(3L, patch);

        assertThat(out.getTitre()).isEqualTo("NewTitle");
        assertThat(existing.getTitre()).isEqualTo("NewTitle");
        assertThat(existing.getTypeFormation()).isEqualTo(TypeFormation.CYBERSECURITY);
    }

    @Test
    void deleteById_deletesWhenExists() {
        when(formationRepository.existsById(5L)).thenReturn(true);

        formationService.deleteById(5L);

        verify(formationRepository).deleteById(5L);
    }

    @Test
    void deleteById_throwsWhenMissing() {
        when(formationRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> formationService.deleteById(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Formation non trouvée");
    }

    private static Formation formation(String titre, TypeFormation type) {
        return Formation.builder()
                .id((long) titre.hashCode())
                .titre(titre)
                .typeFormation(type)
                .description("desc")
                .dureeHeures(8)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(10))
                .capaciteMax(20)
                .statut(Formation.StatutFormation.OUVERTE)
                .build();
    }
}
