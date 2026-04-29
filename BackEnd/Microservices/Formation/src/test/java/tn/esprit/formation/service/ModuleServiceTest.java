package tn.esprit.formation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.formation.dto.ModuleDto;
import tn.esprit.formation.entity.Formation;
import tn.esprit.formation.entity.Module;
import tn.esprit.formation.repository.FormationRepository;
import tn.esprit.formation.repository.ModuleRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private FormationRepository formationRepository;

    @InjectMocks
    private ModuleService moduleService;

    @Test
    void findByFormationId_mapsModules() {
        Formation f = Formation.builder().id(1L).titre("F").build();
        Module m = Module.builder().id(9L).titre("M").dureeMinutes(30).ordre(1).formation(f).build();
        when(moduleRepository.findByFormationIdOrderByOrdreAsc(1L)).thenReturn(List.of(m));

        List<ModuleDto> out = moduleService.findByFormationId(1L);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getTitre()).isEqualTo("M");
        assertThat(out.get(0).getFormationId()).isEqualTo(1L);
    }

    @Test
    void findById_throwsWhenMissing() {
        when(moduleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.findById(404L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Module non trouvé");
    }

    @Test
    void findById_returnsDto() {
        Formation f = Formation.builder().id(1L).titre("F").build();
        Module m = Module.builder().id(12L).titre("M").dureeMinutes(5).ordre(0).formation(f).build();
        when(moduleRepository.findById(12L)).thenReturn(Optional.of(m));

        ModuleDto out = moduleService.findById(12L);

        assertThat(out.getId()).isEqualTo(12L);
        assertThat(out.getTitre()).isEqualTo("M");
    }

    @Test
    void create_throwsWhenFormationMissing() {
        ModuleDto dto = ModuleDto.builder()
                .titre("Mod")
                .description("d")
                .dureeMinutes(15)
                .ordre(0)
                .formationId(99L)
                .build();
        when(formationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.create(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Formation non trouvée");
    }

    @Test
    void create_savesWithDefaultOrdre() {
        Formation formation = Formation.builder().id(2L).titre("F").build();
        when(formationRepository.findById(2L)).thenReturn(Optional.of(formation));
        when(moduleRepository.save(any(Module.class))).thenAnswer(i -> {
            Module mod = i.getArgument(0);
            mod.setId(100L);
            return mod;
        });

        ModuleDto dto = ModuleDto.builder()
                .titre("Mod")
                .description("d")
                .dureeMinutes(20)
                .formationId(2L)
                .build();

        ModuleDto out = moduleService.create(dto);

        assertThat(out.getId()).isEqualTo(100L);
        assertThat(out.getOrdre()).isZero();
    }

    @Test
    void update_throwsWhenModuleMissing() {
        when(moduleRepository.findById(404L)).thenReturn(Optional.empty());
        ModuleDto patch = ModuleDto.builder().titre("x").dureeMinutes(1).formationId(1L).build();

        assertThatThrownBy(() -> moduleService.update(404L, patch))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Module non trouvé");
    }

    @Test
    void update_patchesFields() {
        Module existing = Module.builder()
                .id(5L)
                .titre("Old")
                .description("a")
                .dureeMinutes(10)
                .ordre(1)
                .formation(Formation.builder().id(1L).build())
                .build();
        when(moduleRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(moduleRepository.save(existing)).thenReturn(existing);

        ModuleDto patch = ModuleDto.builder()
                .titre("New")
                .description("b")
                .dureeMinutes(25)
                .ordre(3)
                .formationId(1L)
                .build();

        ModuleDto out = moduleService.update(5L, patch);

        assertThat(out.getTitre()).isEqualTo("New");
        assertThat(existing.getDureeMinutes()).isEqualTo(25);
    }

    @Test
    void deleteById_deletesWhenExists() {
        when(moduleRepository.existsById(7L)).thenReturn(true);

        moduleService.deleteById(7L);

        verify(moduleRepository).deleteById(7L);
    }

    @Test
    void deleteById_throwsWhenMissing() {
        when(moduleRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> moduleService.deleteById(7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Module non trouvé");
    }
}
