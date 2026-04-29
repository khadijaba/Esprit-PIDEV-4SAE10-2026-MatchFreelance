package tn.esprit.formation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.formation.dto.InscriptionDto;
import tn.esprit.formation.service.InscriptionService;

import java.util.List;

/**
 * API Inscriptions (freelancer ↔ formation) - Consommable via Gateway.
 */
@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InscriptionController {

    private final InscriptionService inscriptionService;

    @GetMapping("/formation/{formationId}")
    public ResponseEntity<List<InscriptionDto>> getByFormation(@PathVariable Long formationId) {
        return ResponseEntity.ok(inscriptionService.findByFormation(formationId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<InscriptionDto>> getByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(inscriptionService.findByFreelancer(freelancerId));
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<InscriptionDto>> getEnAttente() {
        return ResponseEntity.ok(inscriptionService.findByStatutEnAttente());
    }

    @PostMapping("/formation/{formationId}/freelancer/{freelancerId}")
    public ResponseEntity<InscriptionDto> inscrire(
            @PathVariable Long formationId,
            @PathVariable Long freelancerId) {
        return ResponseEntity.ok(inscriptionService.inscrire(formationId, freelancerId));
    }

    @PatchMapping("/{id}/valider")
    public ResponseEntity<InscriptionDto> valider(@PathVariable Long id) {
        return ResponseEntity.ok(inscriptionService.validerInscription(id));
    }

    @PatchMapping("/{id}/annuler")
    public ResponseEntity<Void> annuler(@PathVariable Long id) {
        inscriptionService.annulerInscription(id);
        return ResponseEntity.noContent().build();
    }
}
