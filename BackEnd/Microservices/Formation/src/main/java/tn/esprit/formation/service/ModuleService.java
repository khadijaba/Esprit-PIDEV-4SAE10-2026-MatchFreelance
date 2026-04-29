package tn.esprit.formation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.formation.dto.ModuleDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.Module;
import tn.esprit.formation.repository.FormationRepository;
import tn.esprit.formation.repository.ModuleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final FormationRepository formationRepository;

    @Transactional(readOnly = true)
    public List<ModuleDto> findByFormationId(Long formationId) {
        return moduleRepository.findByFormationIdOrderByOrdreAsc(formationId).stream()
                .map(ModuleDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ModuleDto findById(Long id) {
        Module m = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module non trouvé: " + id));
        return ModuleDto.fromEntity(m);
    }

    @Transactional
    public ModuleDto create(ModuleDto dto) {
        Formation formation = formationRepository.findById(dto.getFormationId())
                .orElseThrow(() -> new RuntimeException("Formation non trouvée: " + dto.getFormationId()));
        Module module = Module.builder()
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .dureeMinutes(dto.getDureeMinutes())
                .ordre(dto.getOrdre() != null ? dto.getOrdre() : 0)
                .formation(formation)
                .build();
        return ModuleDto.fromEntity(moduleRepository.save(module));
    }

    @Transactional
    public ModuleDto update(Long id, ModuleDto dto) {
        Module existing = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module non trouvé: " + id));
        existing.setTitre(dto.getTitre());
        existing.setDescription(dto.getDescription());
        existing.setDureeMinutes(dto.getDureeMinutes());
        if (dto.getOrdre() != null) existing.setOrdre(dto.getOrdre());
        return ModuleDto.fromEntity(moduleRepository.save(existing));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!moduleRepository.existsById(id))
            throw new RuntimeException("Module non trouvé: " + id);
        moduleRepository.deleteById(id);
    }
}
