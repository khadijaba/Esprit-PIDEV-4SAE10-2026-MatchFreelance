package tn.esprit.formation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.formation.dto.FormationDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.repository.FormationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormationService {

    private final FormationRepository formationRepository;
    private final FormationNotificationService notificationService;

    private static List<FormationDto> mapToDto(List<Formation> formations) {
        return formations.stream().map(FormationDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<FormationDto> findAll() {
        return mapToDto(formationRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<FormationDto> findOuvertes() {
        return mapToDto(formationRepository.findFormationsOuvertes());
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
        Formation saved = formationRepository.save(f);
        FormationDto result = FormationDto.fromEntity(saved);
        notificationService.notifyNewFormation(result);
        return result;
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
        existing.setNiveau(dto.getNiveau());
        existing.setExamenRequisId(dto.getExamenRequisId());
        return FormationDto.fromEntity(formationRepository.save(existing));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!formationRepository.existsById(id))
            throw new RuntimeException("Formation non trouvée: " + id);
        formationRepository.deleteById(id);
    }

    /**
     * Formations ouvertes suggérées (le filtrage par compétences Skill a été retiré avec le microservice Skill).
     *
     * @param freelancerId conservé pour l’API REST ; non utilisé tant que Skill est absent.
     */
    @Transactional(readOnly = true)
    public List<FormationDto> getRecommandationsForFreelancer(Long freelancerId) {
        // Paramètre conservé pour l’API ; le filtrage par compétences Skill n’est pas branché ici.
        if (freelancerId == null || freelancerId <= 0) {
            return List.of();
        }
        return findOuvertes();
    }
}
