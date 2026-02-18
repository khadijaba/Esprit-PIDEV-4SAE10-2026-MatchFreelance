package tn.esprit.evaluation.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.evaluation.dto.CertificatDto;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Génère le PDF du certificat pour téléchargement / affichage.
 */
@Service
@RequiredArgsConstructor
public class CertificatPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE);

    public byte[] generatePdf(CertificatDto dto) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

            document.add(new Paragraph(" "));
            Paragraph pCert = new Paragraph("CERTIFICAT DE RÉUSSITE", fontTitle);
            pCert.setAlignment(Element.ALIGN_CENTER);
            document.add(pCert);
            document.add(new Paragraph(" "));

            Paragraph pTitre = new Paragraph(dto.getExamenTitre(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK));
            pTitre.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitre);

            Paragraph pNum = new Paragraph("N° " + dto.getNumeroCertificat(), fontSubtitle);
            pNum.setAlignment(Element.ALIGN_CENTER);
            document.add(pNum);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            String attestation = "Ce certificat atteste que le participant (Freelancer #" + dto.getFreelancerId() + ") a réussi l'examen avec un score de " + dto.getScore() + " %.";
            document.add(new Paragraph(attestation, fontBody));
            document.add(new Paragraph(" "));
            String datePassage = dto.getDatePassage() != null ? dto.getDatePassage().format(DATE_FORMAT) : "-";
            String dateDelivrance = dto.getDateDelivrance() != null ? dto.getDateDelivrance().format(DATE_FORMAT) : "-";
            document.add(new Paragraph("Date de passage : " + datePassage, fontSmall));
            document.add(new Paragraph("Date de délivrance : " + dateDelivrance, fontSmall));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            Paragraph footer = new Paragraph("MatchFreelance — Plateforme de formation et d'évaluation", fontSmall);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF certificat", e);
        }
        return out.toByteArray();
    }
}
