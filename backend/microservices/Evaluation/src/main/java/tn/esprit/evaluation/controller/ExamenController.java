package tn.esprit.evaluation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.evaluation.dto.ExamenDto;
import tn.esprit.evaluation.dto.PassageExamenDto;
import tn.esprit.evaluation.dto.ReponseExamenRequest;
import tn.esprit.evaluation.service.ExamenService;
import tn.esprit.evaluation.service.PassageExamenService;

import java.util.List;

/**
 * API Examens et passages - Consommable via Gateway.
 */
@RestController
@RequestMapping("/api/examens")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExamenController {

    private final ExamenService examenService;
    private final PassageExamenService passageExamenService;

    // ---------- CRUD Examens ----------
    @GetMapping
    public ResponseEntity<List<ExamenDto>> getAll() {
        return ResponseEntity.ok(examenService.findAll());
    }

    @GetMapping("/formation/{formationId}")
    public ResponseEntity<List<ExamenDto>> getByFormation(@PathVariable Long formationId) {
        return ResponseEntity.ok(examenService.findByFormationId(formationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamenDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(examenService.findById(id));
    }

    @GetMapping("/{id}/passage")
    public ResponseEntity<ExamenDto> getPourPassage(@PathVariable Long id) {
        return ResponseEntity.ok(examenService.getExamenPourPassage(id));
    }

    @PostMapping
    public ResponseEntity<ExamenDto> create(@Valid @RequestBody ExamenDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examenService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamenDto> update(@PathVariable Long id, @Valid @RequestBody ExamenDto dto) {
        return ResponseEntity.ok(examenService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examenService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Passer un examen / Résultats ----------
    @PostMapping("/{examenId}/passer")
    public ResponseEntity<PassageExamenDto> passerExamen(
            @PathVariable Long examenId,
            @Valid @RequestBody ReponseExamenRequest request) {
        return ResponseEntity.ok(passageExamenService.soumettreExamen(examenId, request));
    }

    @GetMapping("/resultats/freelancer/{freelancerId}")
    public ResponseEntity<List<PassageExamenDto>> getResultatsByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(passageExamenService.findByFreelancer(freelancerId));
    }

    @GetMapping("/{examenId}/resultats")
    public ResponseEntity<List<PassageExamenDto>> getResultatsByExamen(@PathVariable Long examenId) {
        return ResponseEntity.ok(passageExamenService.findByExamen(examenId));
    }

    @GetMapping("/{examenId}/freelancer/{freelancerId}")
    public ResponseEntity<PassageExamenDto> getPassage(@PathVariable Long examenId, @PathVariable Long freelancerId) {
        return ResponseEntity.ok(passageExamenService.getPassageByFreelancerAndExamen(examenId, freelancerId));
    }
}
