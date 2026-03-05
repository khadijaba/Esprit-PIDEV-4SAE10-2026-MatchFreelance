package tn.esprit.formation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.formation.dto.ModuleDto;
import tn.esprit.formation.service.ModuleService;

import java.util.List;

/**
 * API Modules (courts) - Séquences pédagogiques d'une formation.
 * Consommable via Gateway : /api/modules, /api/modules/formation/{formationId}.
 */
@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping("/formation/{formationId}")
    public ResponseEntity<List<ModuleDto>> getByFormation(@PathVariable Long formationId) {
        return ResponseEntity.ok(moduleService.findByFormationId(formationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(moduleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ModuleDto> create(@Valid @RequestBody ModuleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moduleService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModuleDto> update(@PathVariable Long id, @Valid @RequestBody ModuleDto dto) {
        return ResponseEntity.ok(moduleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        moduleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
