package tn.esprit.evaluation.service;

import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import tn.esprit.evaluation.dto.CertificatDto;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;

/**
 * Génère le PDF du certificat pour téléchargement / affichage.
 */
@Service
@RequiredArgsConstructor
public class CertificatPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE);

    /**
     * Gateway / API publique (ex. http://localhost:8050) — utilisé si QR pointe vers la page HTML du backend.
     */
    @Value("${app.public.base-url:http://localhost:8050}")
    private String publicBaseUrl;

    /**
     * Front Angular (ex. http://192.168.1.10:4200) — recommandé pour QR : scan mobile ne voit pas
     * « localhost » du PC. Utiliser l’IP locale de votre machine + port 4200.
     */
    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    /**
     * Si true (défaut), le QR ouvre le front : /verify-certificat/{numero} (proxy /api vers la Gateway).
     * Si false, le QR pointe vers /api/certificats/verify/{numero}/page sur app.public.base-url.
     */
    @Value("${app.certificat.qr-use-frontend:true}")
    private boolean qrUseFrontend;

    @Value("${certificate.logo.path:}")
    private String logoPath;

    public byte[] generatePdf(CertificatDto dto) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(15, 23, 42));
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(100, 116, 139));
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            Font fontMention = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(5, 150, 105));

            // Header branding (logo si dispo, sinon bloc texte stylé)
            addHeader(document);

            Paragraph pCert = new Paragraph("CERTIFICAT DE RÉUSSITE", fontTitle);
            pCert.setAlignment(Element.ALIGN_CENTER);
            document.add(pCert);

            Paragraph pSousTitre = new Paragraph("Microservice Evaluation — MatchFreelance", fontSubtitle);
            pSousTitre.setSpacingBefore(6);
            pSousTitre.setAlignment(Element.ALIGN_CENTER);
            document.add(pSousTitre);
            document.add(new Paragraph(" "));

            Paragraph pTitre = new Paragraph(dto.getExamenTitre() != null ? dto.getExamenTitre() : "Examen", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK));
            pTitre.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitre);

            Paragraph pNum = new Paragraph("N° " + (dto.getNumeroCertificat() != null ? dto.getNumeroCertificat() : "-"), fontSubtitle);
            pNum.setAlignment(Element.ALIGN_CENTER);
            document.add(pNum);
            document.add(new Paragraph(" "));

            String mention = computeMention(dto.getScore());
            String attestation = "Ce certificat atteste que le participant (Freelancer #" + dto.getFreelancerId() + ") " +
                    "a passé l'examen avec un score de " + safeInt(dto.getScore()) + " %.";
            document.add(new Paragraph(attestation, fontBody));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2.5f});
            table.setSpacingBefore(6);
            table.addCell(infoCell("Score obtenu"));
            table.addCell(valueCell(safeInt(dto.getScore()) + " %"));
            table.addCell(infoCell("Seuil de passage"));
            table.addCell(valueCell(safeInt(dto.getSeuilReussi()) + " %"));
            table.addCell(infoCell("Décision"));
            table.addCell(valueCell((dto.getScore() != null && dto.getSeuilReussi() != null && dto.getScore() >= dto.getSeuilReussi()) ? "ADMIS" : "REFUSÉ"));
            table.addCell(infoCell("Mention"));
            table.addCell(valueCell(mention));
            document.add(table);
            document.add(new Paragraph(" "));

            Paragraph pMention = new Paragraph("Mention : " + mention, fontMention);
            pMention.setAlignment(Element.ALIGN_CENTER);
            document.add(pMention);
            document.add(new Paragraph(" "));

            String datePassage = dto.getDatePassage() != null ? dto.getDatePassage().format(DATE_FORMAT) : "-";
            String dateDelivrance = dto.getDateDelivrance() != null ? dto.getDateDelivrance().format(DATE_FORMAT) : "-";
            document.add(new Paragraph("Date de passage : " + datePassage, fontSmall));
            document.add(new Paragraph("Date de délivrance : " + dateDelivrance, fontSmall));
            document.add(new Paragraph(" "));

            // QR code de vérification → page Angular (défaut) ou page HTML du microservice
            String verifyUrl = buildVerifyQrUrl(dto.getNumeroCertificat());
            try {
                Image qr = buildQrImage(verifyUrl, 140, 140);
                qr.setAlignment(Element.ALIGN_CENTER);
                document.add(qr);
                Paragraph pVerify = new Paragraph("Vérifier ce certificat : " + verifyUrl, fontSmall);
                pVerify.setAlignment(Element.ALIGN_CENTER);
                document.add(pVerify);
            } catch (Exception e) {
                // Si le QR échoue, on n'empêche pas la génération du PDF
                document.add(new Paragraph("Vérification : " + verifyUrl, fontSmall));
            }

            Paragraph footer = new Paragraph("MatchFreelance — Plateforme de formation et d'évaluation", fontSmall);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF certificat", e);
        }
        return out.toByteArray();
    }

    /**
     * URL encodée dans le QR. Préférer le front Angular (même Wi‑Fi : IP du PC au lieu de localhost).
     */
    private String buildVerifyQrUrl(String numeroCertificat) {
        String num = numeroCertificat != null ? numeroCertificat : "";
        String encoded = UriUtils.encodePathSegment(num, StandardCharsets.UTF_8);
        if (qrUseFrontend) {
            String base = frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
            return base + "/verify-certificat/" + encoded;
        }
        String base = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        return base + "/api/certificats/verify/" + encoded + "/page";
    }

    private Image buildQrImage(String content, int width, int height) throws WriterException, IOException, BadElementException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Image.getInstance(baos.toByteArray());
    }

    private void addHeader(Document document) throws DocumentException {
        try {
            if (logoPath != null && !logoPath.isBlank()) {
                InputStream is = getClass().getResourceAsStream(logoPath.startsWith("/") ? logoPath : "/" + logoPath);
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    Image logo = Image.getInstance(bytes);
                    logo.scaleToFit(120, 60);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                    document.add(new Paragraph(" "));
                    return;
                }
            }
        } catch (Exception ignored) {
            // fallback texte
        }

        Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(20, 184, 166));
        Paragraph pBrand = new Paragraph("MATCHFREELANCE", brand);
        pBrand.setAlignment(Element.ALIGN_CENTER);
        document.add(pBrand);
        document.add(new Paragraph(" "));
    }

    private PdfPCell infoCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(71, 85, 105))));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setPadding(8f);
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK)));
        cell.setPadding(8f);
        return cell;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String computeMention(Integer score) {
        int s = safeInt(score);
        if (s >= 85) return "Très bien";
        if (s >= 70) return "Bien";
        if (s >= 60) return "Acceptable";
        return "Refus";
    }
}
