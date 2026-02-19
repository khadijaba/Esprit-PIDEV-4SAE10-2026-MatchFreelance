package tn.esprit.evaluation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.evaluation.dto.CertificatDto;
import tn.esprit.evaluation.service.CertificatPdfService;
import tn.esprit.evaluation.service.CertificatService;

import java.util.List;

@RestController
@RequestMapping("/api/certificats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CertificatController {

    private final CertificatService certificatService;
    private final CertificatPdfService certificatPdfService;

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<CertificatDto>> getByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(certificatService.findByFreelancer(freelancerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificatDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(certificatService.findById(id));
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
        return ResponseEntity.ok(certificatService.findByPassageExamen(passageExamenId));
    }
}
