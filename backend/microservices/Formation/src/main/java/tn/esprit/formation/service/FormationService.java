package tn.esprit.formation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.formation.dto.FormationDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.repository.FormationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormationService {

    private final FormationRepository formationRepository;

    @Transactional(readOnly = true)
    public List<FormationDto> findAll() {
        return formationRepository.findAll().stream()
                .map(FormationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FormationDto> findOuvertes() {
        return formationRepository.findFormationsOuvertes().stream()
                .map(FormationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormationDto findById(Long id) {
        Formation f = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée: " + id));
        return FormationDto.fromEntity(f);
    }

    @Transactional
    public FormationDto create(FormationDto dto) {
        Formation f = dto.toEntity();
        f.setId(null);
        return FormationDto.fromEntity(formationRepository.save(f));
    }

    @Transactional
    public FormationDto update(Long id, FormationDto dto) {
        Formation existing = formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée: " + id));
        existing.setTitre(dto.getTitre());
        existing.setTypeFormation(dto.getTypeFormation() != null ? dto.getTypeFormation() : existing.getTypeFormation());
        existing.setDescription(dto.getDescription());
        existing.setDureeHeures(dto.getDureeHeures());
        existing.setDateDebut(dto.getDateDebut());
        existing.setDateFin(dto.getDateFin());
        existing.setCapaciteMax(dto.getCapaciteMax());
        if (dto.getStatut() != null) existing.setStatut(dto.getStatut());
        existing.setExamenRequisId(dto.getExamenRequisId());
        return FormationDto.fromEntity(formationRepository.save(existing));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!formationRepository.existsById(id))
            throw new RuntimeException("Formation non trouvée: " + id);
        formationRepository.deleteById(id);
    }
}
