package tn.esprit.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Certificat délivré lorsqu'un freelancer réussit un examen (score >= seuil).
 */
@Entity
@Table(name = "certificat", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"passage_examen_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numéro unique du certificat (ex: CERT-xxx) */
    @Column(name = "numero_certificat", nullable = false, unique = true, length = 64)
    private String numeroCertificat;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passage_examen_id", nullable = false, unique = true)
    private PassageExamen passageExamen;

    @Column(name = "date_delivrance", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateDelivrance = LocalDateTime.now();

    /** Génère un numéro unique pour le certificat. */
    public static String genererNumero() {
        return "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
