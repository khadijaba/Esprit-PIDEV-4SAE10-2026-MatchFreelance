package tn.esprit.evaluation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.evaluation.client.FormationClient;
import tn.esprit.evaluation.dto.CertificatDto;
import tn.esprit.evaluation.service.CertificatPdfService;
import tn.esprit.evaluation.service.CertificatCarriereService;
import tn.esprit.evaluation.service.CertificatService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CertificatController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE);

    private final CertificatService certificatService;
    private final CertificatPdfService certificatPdfService;
    private final FormationClient formationClient;
    private final CertificatCarriereService certificatCarriereService;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<CertificatDto>> getByFreelancer(@PathVariable Long freelancerId) {
        List<CertificatDto> certificats = certificatService.findByFreelancer(freelancerId);
        certificats.forEach(certificatCarriereService::synchroniserSkillsDepuisCertificat);
        return ResponseEntity.ok(certificats);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificatDto> getById(@PathVariable Long id) {
        CertificatDto dto = certificatService.findById(id);
        certificatCarriereService.synchroniserSkillsDepuisCertificat(dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPdfById(@PathVariable Long id) {
        CertificatDto dto = certificatService.findById(id);
        byte[] pdf = certificatPdfService.generatePdf(dto);
        String filename = "certificat-" + dto.getNumeroCertificat() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/passage/{passageExamenId}")
    public ResponseEntity<CertificatDto> getByPassage(@PathVariable Long passageExamenId) {
        CertificatDto dto = certificatService.findByPassageExamen(passageExamenId);
        certificatCarriereService.synchroniserSkillsDepuisCertificat(dto);
        return ResponseEntity.ok(dto);
    }

    /**
     * Endpoint public de vérification d'un certificat via son numéro (ex: CERT-XXXX).
     * Utilisé par le QR code imprimé dans le PDF.
     */
    @GetMapping("/verify/{numero}")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable String numero) {
        try {
            CertificatDto dto = certificatService.findByNumero(numero);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("valid", true);
            body.put("numeroCertificat", dto.getNumeroCertificat());
            body.put("freelancerId", dto.getFreelancerId());
            body.put("examenId", dto.getExamenId());
            body.put("formationId", dto.getFormationId());
            body.put("examenTitre", dto.getExamenTitre());
            body.put("score", dto.getScore());
            body.put("dateDelivrance", dto.getDateDelivrance());
            try {
                body.put("formationsRecommandees", filterAutresFormations(dto, formationClient.getRecommandationsForFreelancer(dto.getFreelancerId())));
            } catch (Exception ex) {
                body.put("formationsRecommandees", List.of());
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "numeroCertificat", numero,
                    "message", e.getMessage() != null ? e.getMessage() : "Certificat introuvable"
            ));
        }
    }

    /**
     * Page HTML de vérification (pour scan du QR code).
     * Affiche une carte professionnelle : certificat valide ou introuvable.
     */
    @GetMapping(value = "/verify/{numero}/page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyPage(@PathVariable String numero) {
        String html;
        try {
            CertificatDto dto = certificatService.findByNumero(numero);
            String dateDelivrance = dto.getDateDelivrance() != null ? dto.getDateDelivrance().format(DATE_FORMAT) : "-";
            String datePassage = dto.getDatePassage() != null ? dto.getDatePassage().format(DATE_FORMAT) : "-";
            List<Map<String, Object>> autres;
            try {
                autres = filterAutresFormations(dto, formationClient.getRecommandationsForFreelancer(dto.getFreelancerId()));
            } catch (Exception ex) {
                autres = List.of();
            }
            String recoBlock = buildFormationsRecommandeesHtml(autres, normalizeFrontendBase());
            html = "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                    "<title>Vérification certificat - MatchFreelance</title>" +
                    "<style>body{font-family:system-ui,-apple-system,sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#0f172a 0%,#1e293b 100%);color:#fff;padding:1.5rem 0;}" +
                    ".wrap{max-width:520px;width:92%;}" +
                    ".card{background:#fff;color:#1e293b;border-radius:16px;box-shadow:0 25px 50px -12px rgba(0,0,0,.4);padding:2rem;text-align:center;}" +
                    ".badge{display:inline-flex;align-items:center;gap:8px;padding:10px 18px;border-radius:9999px;font-weight:700;font-size:14px;margin-bottom:1.5rem;}" +
                    ".badge.valid{background:#d1fae5;color:#065f46;}" +
                    ".badge.invalid{background:#fee2e2;color:#991b1b;}" +
                    "h1{font-size:1.5rem;margin:0 0 .5rem;}" +
                    ".numero{font-size:1rem;color:#64748b;margin-bottom:1.5rem;}" +
                    ".detail{text-align:left;background:#f8fafc;border-radius:8px;padding:1rem;margin:.5rem 0;font-size:14px;}" +
                    ".detail strong{display:inline-block;min-width:140px;color:#475569;}" +
                    ".reco{margin-top:1.75rem;text-align:left;border-top:1px solid #e2e8f0;padding-top:1.25rem;}" +
                    ".reco h2{font-size:1rem;margin:0 0 .75rem;color:#0f172a;text-align:center;}" +
                    ".reco ul{list-style:none;margin:0;padding:0;}" +
                    ".reco li{margin:.5rem 0;padding:.75rem;border-radius:8px;background:#f1f5f9;border:1px solid #e2e8f0;}" +
                    ".reco a{color:#0d9488;font-weight:600;text-decoration:none;}" +
                    ".reco a:hover{text-decoration:underline;}" +
                    ".reco .meta{font-size:12px;color:#64748b;margin-top:4px;}" +
                    ".reco .empty{font-size:13px;color:#64748b;text-align:center;}" +
                    ".footer{margin-top:1.5rem;font-size:12px;color:#94a3b8;}" +
                    "</style></head><body><div class=\"wrap\"><div class=\"card\">" +
                    "<div class=\"badge valid\"><span>✓</span> Certificat valide</div>" +
                    "<h1>MatchFreelance</h1>" +
                    "<p class=\"numero\">N° " + escapeHtml(dto.getNumeroCertificat()) + "</p>" +
                    "<div class=\"detail\"><strong>Examen</strong> " + escapeHtml(dto.getExamenTitre()) + "</div>" +
                    "<div class=\"detail\"><strong>Score</strong> " + dto.getScore() + " %</div>" +
                    "<div class=\"detail\"><strong>Freelancer</strong> #" + dto.getFreelancerId() + "</div>" +
                    "<div class=\"detail\"><strong>Date de passage</strong> " + escapeHtml(datePassage) + "</div>" +
                    "<div class=\"detail\"><strong>Délivré le</strong> " + escapeHtml(dateDelivrance) + "</div>" +
                    recoBlock +
                    "<p class=\"footer\">Ce certificat a été délivré par la plateforme MatchFreelance.</p></div></div></body></html>";
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Certificat introuvable.";
            html = "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                    "<title>Vérification certificat - MatchFreelance</title>" +
                    "<style>body{font-family:system-ui,-apple-system,sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#0f172a 0%,#1e293b 100%);color:#fff;}" +
                    ".card{background:#fff;color:#1e293b;border-radius:16px;box-shadow:0 25px 50px -12px rgba(0,0,0,.4);max-width:420px;width:90%;padding:2rem;text-align:center;}" +
                    ".badge{display:inline-flex;align-items:center;gap:8px;padding:10px 18px;border-radius:9999px;font-weight:700;font-size:14px;margin-bottom:1.5rem;}" +
                    ".badge.invalid{background:#fee2e2;color:#991b1b;}" +
                    "h1{font-size:1.5rem;margin:0 0 .5rem;}" +
                    ".numero{font-size:1rem;color:#64748b;margin-bottom:1rem;}" +
                    ".msg{color:#64748b;font-size:14px;}" +
                    ".footer{margin-top:1.5rem;font-size:12px;color:#94a3b8;}" +
                    "</style></head><body><div class=\"card\">" +
                    "<div class=\"badge invalid\"><span>✕</span> Certificat invalide</div>" +
                    "<h1>MatchFreelance</h1>" +
                    "<p class=\"numero\">N° " + escapeHtml(numero) + "</p>" +
                    "<p class=\"msg\">" + escapeHtml(msg) + "</p>" +
                    "<p class=\"footer\">Vérifiez le numéro ou contactez la plateforme.</p></div></body></html>";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(html);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Exclut la formation de l'examen courant pour proposer d'autres parcours. */
    private static List<Map<String, Object>> filterAutresFormations(CertificatDto dto, List<Map<String, Object>> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        Long exclude = dto != null ? dto.getFormationId() : null;
        if (exclude == null) return raw.stream().limit(12).collect(Collectors.toList());
        return raw.stream()
                .filter(m -> {
                    Object id = m.get("id");
                    if (id == null) return true;
                    try {
                        return !exclude.equals(Long.valueOf(id.toString()));
                    } catch (NumberFormatException e) {
                        return true;
                    }
                })
                .limit(12)
                .collect(Collectors.toList());
    }

    private String normalizeFrontendBase() {
        return frontendBaseUrl == null ? "" : frontendBaseUrl.replaceAll("/+$", "");
    }

    private String buildFormationsRecommandeesHtml(List<Map<String, Object>> formations, String base) {
        if (formations == null || formations.isEmpty()) {
            return "<div class=\"reco\"><h2>Autres formations recommandées pour vous</h2><p class=\"empty\">Aucune suggestion disponible pour le moment (vérifiez que le microservice Formation est démarré).</p></div>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"reco\"><h2>Autres formations recommandées pour vous</h2><ul>");
        for (Map<String, Object> f : formations) {
            Object idObj = f.get("id");
            String titre = f.get("titre") != null ? f.get("titre").toString() : "Formation";
            String type = f.get("typeFormation") != null ? f.get("typeFormation").toString() : "";
            String niveau = f.get("niveau") != null ? f.get("niveau").toString() : "";
            String href = base + "/formations/" + (idObj != null ? idObj : "");
            String meta = joinFormationMeta(type, niveau);
            sb.append("<li><a href=\"").append(escapeHtml(href)).append("\" target=\"_blank\" rel=\"noopener\">")
                    .append(escapeHtml(titre)).append("</a>");
            if (!meta.isEmpty()) {
                sb.append("<div class=\"meta\">").append(escapeHtml(meta)).append("</div>");
            }
            sb.append("</li>");
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    private static String joinFormationMeta(String type, String niveau) {
        List<String> parts = new ArrayList<>();
        if (type != null && !type.isBlank()) parts.add(type);
        if (niveau != null && !niveau.isBlank()) parts.add(niveau);
        return String.join(" · ", parts);
    }
}
