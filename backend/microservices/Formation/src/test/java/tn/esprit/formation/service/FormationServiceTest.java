package tn.esprit.formation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.formation.dto.FormationDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.TypeFormation;
import tn.esprit.formation.repository.FormationRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void getRecommandationsForFreelancer_returnsAllOpenFormations() {
        Formation devops = formation("DevOps Mastery", TypeFormation.DEVOPS);
        Formation ai = formation("IA Avancee", TypeFormation.AI);
        Formation data = formation("Data Science", TypeFormation.DATA_SCIENCE);
        when(formationRepository.findFormationsOuvertes()).thenReturn(List.of(devops, ai, data));

        List<FormationDto> recos = formationService.getRecommandationsForFreelancer(7L);

        assertEquals(3, recos.size());
        assertTrue(recos.stream().anyMatch(f -> "DevOps Mastery".equals(f.getTitre())));
    }

    @Test
    void getRecommandationsForFreelancer_returnsAllWhenMultipleOpen() {
        Formation mobile = formation("Mobile", TypeFormation.MOBILE_DEVELOPMENT);
        Formation cyber = formation("Cyber", TypeFormation.CYBERSECURITY);
        when(formationRepository.findFormationsOuvertes()).thenReturn(List.of(mobile, cyber));

        List<FormationDto> recos = formationService.getRecommandationsForFreelancer(3L);

        assertEquals(2, recos.size());
        assertTrue(recos.stream().anyMatch(f -> "Mobile".equals(f.getTitre())));
        assertTrue(recos.stream().anyMatch(f -> "Cyber".equals(f.getTitre())));
    }

    private Formation formation(String titre, TypeFormation type) {
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
