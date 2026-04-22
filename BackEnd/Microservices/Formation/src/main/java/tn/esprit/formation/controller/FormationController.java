package tn.esprit.formation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.formation.dto.FormationDto;
import tn.esprit.formation.service.FormationService;

import java.util.List;

/**
 * API Formations - Consommable via Gateway (ex: /formation-service/api/formations).
 */
@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FormationController {

    private final FormationService formationService;

    @GetMapping
    public ResponseEntity<List<FormationDto>> getAll() {
        return ResponseEntity.ok(formationService.findAll());
    }

    @GetMapping("/ouvertes")
    public ResponseEntity<List<FormationDto>> getOuvertes() {
        return ResponseEntity.ok(formationService.findOuvertes());
    }

<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
    /** Métier avancé : formations recommandées pour un freelancer (selon ses gaps de compétences). */
    @GetMapping("/recommandations/freelancer/{freelancerId}")
    public ResponseEntity<List<FormationDto>> getRecommandationsForFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(formationService.getRecommandationsForFreelancer(freelancerId));
    }

<<<<<<< HEAD
=======
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
    @GetMapping("/{id}")
    public ResponseEntity<FormationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.findById(id));
    }

    @PostMapping
    public ResponseEntity<FormationDto> create(@Valid @RequestBody FormationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formationService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormationDto> update(@PathVariable Long id, @Valid @RequestBody FormationDto dto) {
        return ResponseEntity.ok(formationService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        formationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
