package tn.esprit.formation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import tn.esprit.formation.dto.FormationDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.TypeFormation;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FormationNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void notifyNewFormation_skipsWhenMailSenderNull() {
        FormationNotificationService svc = new FormationNotificationService(null, "a@b.com", true);
        svc.notifyNewFormation(sampleDto());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyNewFormation_skipsWhenRecipientsBlank() {
        FormationNotificationService svc = new FormationNotificationService(mailSender, "  ", true);
        svc.notifyNewFormation(sampleDto());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyNewFormation_sendsWhenConfigured() {
        FormationNotificationService svc = new FormationNotificationService(mailSender, " admin@test.com , other@test.com ", true);
        svc.notifyNewFormation(sampleDto());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactlyInAnyOrder("admin@test.com", "other@test.com");
        assertThat(msg.getSubject()).contains("Nouvelle formation");
        assertThat(msg.getText()).contains("Spring Boot");
    }

    private static FormationDto sampleDto() {
        return FormationDto.builder()
                .id(1L)
                .titre("Spring Boot")
                .typeFormation(TypeFormation.WEB_DEVELOPMENT)
                .description("desc")
                .dureeHeures(10)
                .dateDebut(LocalDate.of(2026, 1, 1))
                .dateFin(LocalDate.of(2026, 1, 10))
                .capaciteMax(20)
                .statut(Formation.StatutFormation.OUVERTE)
                .build();
    }
}
