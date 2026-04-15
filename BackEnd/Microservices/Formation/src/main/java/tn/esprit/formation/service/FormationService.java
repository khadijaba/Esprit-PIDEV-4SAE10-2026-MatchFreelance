package tn.esprit.formation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.formation.client.SkillClient;
import tn.esprit.formation.dto.FormationDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.repository.FormationRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormationService {

    private final FormationRepository formationRepository;
    private final SkillClient skillClient;
    private final FormationNotificationService notificationService;

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
     * Métier avancé : recommandations de formations pour un freelancer.
     * Retourne les formations ouvertes dont le type correspond à un "gap" du freelancer
     * (domaines où il n'a pas encore de compétence), pour l'aider à se former.
     */
    @Transactional(readOnly = true)
    public List<FormationDto> getRecommandationsForFreelancer(Long freelancerId) {
        List<String> categoriesFreelancer = skillClient.getCategoriesByFreelancer(freelancerId);
        Set<String> categories = Set.copyOf(categoriesFreelancer);
        return formationRepository.findFormationsOuvertes().stream()
                .filter(f -> f.getTypeFormation() != null
                        && !categories.contains(f.getTypeFormation().name()))
                .map(FormationDto::fromEntity)
                .collect(Collectors.toList());
    }
}
