package tn.esprit.formation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tn.esprit.formation.dto.FormationDto;

import java.util.Arrays;
import java.util.List;

/**
 * Envoi d'un email automatique lorsqu'une nouvelle formation (examen/certificat) est ajoutée.
 * Si SMTP n'est pas configuré, l'envoi est ignoré sans erreur.
 */
@Service
@Slf4j
public class FormationNotificationService {

    private final JavaMailSender mailSender;
    private final String mailTo;
    private final boolean enabled;

    public FormationNotificationService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${formation.notification.mail.to:}") String mailTo,
            @Value("${formation.notification.mail.enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.mailTo = mailTo != null ? mailTo : "";
        this.enabled = enabled;
    }

    public void notifyNewFormation(FormationDto dto) {
        if (mailSender == null || !enabled || mailTo.isBlank()) {
            log.debug("Notification nouvelle formation désactivée ou pas d'adresse/SMTP configuré.");
            return;
        }
        List<String> toList = Arrays.stream(mailTo.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (toList.isEmpty()) return;

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toList.toArray(new String[0]));
            msg.setSubject("MatchFreelance — Nouvelle formation ajoutée (examen/certificat)");
            msg.setText(String.format(
                    "Une nouvelle formation a été ajoutée sur la plateforme.%n%n" +
                    "Titre : %s%n" +
                    "Type : %s%n" +
                    "Dates : %s → %s%n" +
                    "Durée : %d heures%n" +
                    "Statut : %s%n%n" +
                    "Vous pouvez configurer l'examen et le certificat associés dans le microservice Evaluation (Admin → Examens).",
                    dto.getTitre(),
                    dto.getTypeFormation() != null ? dto.getTypeFormation().name() : "-",
                    dto.getDateDebut(),
                    dto.getDateFin(),
                    dto.getDureeHeures() != null ? dto.getDureeHeures() : 0,
                    dto.getStatut() != null ? dto.getStatut().name() : "-"
            ));
            mailSender.send(msg);
            log.info("Notification nouvelle formation envoyée à {} pour « {} »", toList, dto.getTitre());
        } catch (Exception e) {
            log.warn("Impossible d'envoyer la notification nouvelle formation : {}", e.getMessage());
        }
    }
}
