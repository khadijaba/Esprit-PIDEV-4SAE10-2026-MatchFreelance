package esprit.skill.Service;

import esprit.skill.Repositories.SkillRepository;
import esprit.skill.entities.Skill;
import esprit.skill.entities.SkillCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillServiceImpl skillService;

    private static Skill newSkill(String name, Long freelancerId) {
        Skill s = new Skill();
        s.setName(name);
        s.setCategory(SkillCategory.WEB_DEVELOPMENT);
        s.setFreelancerId(freelancerId);
        s.setLevel("INTERMEDIATE");
        s.setYearsOfExperience(3);
        return s;
    }

    @Test
    void addSkill_throwsWhenDuplicateForFreelancer() {
        Skill incoming = newSkill("Java", 10L);
        when(skillRepository.existsByFreelancerIdAndName(10L, "Java")).thenReturn(true);

        assertThatThrownBy(() -> skillService.addSkill(incoming))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Skill already exists");

        verify(skillRepository, never()).save(any());
    }

    @Test
    void addSkill_savesWhenNew() {
        Skill incoming = newSkill("Angular", 5L);
        when(skillRepository.existsByFreelancerIdAndName(5L, "Angular")).thenReturn(false);
        when(skillRepository.save(incoming)).thenReturn(incoming);

        Skill out = skillService.addSkill(incoming);

        assertThat(out).isSameAs(incoming);
        verify(skillRepository).save(incoming);
    }

    @Test
    void getSkillById_throwsWhenMissing() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Skill not found");
    }

    @Test
    void getSkillById_returnsWhenPresent() {
        Skill s = newSkill("Python", 2L);
        s.setId(7L);
        when(skillRepository.findById(7L)).thenReturn(Optional.of(s));

        assertThat(skillService.getSkillById(7L)).isSameAs(s);
    }

    @Test
    void getSkillsByFreelancer_delegatesToRepository() {
        List<Skill> list = List.of(newSkill("A", 1L), newSkill("B", 1L));
        when(skillRepository.findByFreelancerId(1L)).thenReturn(list);

        assertThat(skillService.getSkillsByFreelancer(1L)).isEqualTo(list);
    }

    @Test
    void getSkillsByCategory_delegatesToRepository() {
        List<Skill> list = List.of(newSkill("ML", 3L));
        when(skillRepository.findByCategory(SkillCategory.DATA_SCIENCE)).thenReturn(list);

        assertThat(skillService.getSkillsByCategory(SkillCategory.DATA_SCIENCE)).isEqualTo(list);
    }

    @Test
    void updateSkill_updatesFieldsAndSaves() {
        Skill existing = newSkill("Old", 8L);
        existing.setId(20L);
        existing.setLevel("BEGINNER");
        existing.setYearsOfExperience(1);

        Skill patch = new Skill();
        patch.setName("NewName");
        patch.setCategory(SkillCategory.MOBILE_DEVELOPMENT);
        patch.setLevel("EXPERT");
        patch.setYearsOfExperience(8);

        when(skillRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(skillRepository.save(existing)).thenReturn(existing);

        Skill out = skillService.updateSkill(20L, patch);

        assertThat(out.getName()).isEqualTo("NewName");
        assertThat(out.getCategory()).isEqualTo(SkillCategory.MOBILE_DEVELOPMENT);
        assertThat(out.getLevel()).isEqualTo("EXPERT");
        assertThat(out.getYearsOfExperience()).isEqualTo(8);
        assertThat(out.getFreelancerId()).isEqualTo(8L);

        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void updateSkill_throwsWhenSkillMissing() {
        Skill patch = new Skill();
        patch.setName("Any");
        patch.setCategory(SkillCategory.WEB_DEVELOPMENT);
        patch.setLevel("BEGINNER");
        patch.setYearsOfExperience(1);

        when(skillRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.updateSkill(404L, patch))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Skill not found");

        verify(skillRepository, never()).save(any());
    }

    @Test
    void deleteSkill_deletesById() {
        skillService.deleteSkill(15L);
        verify(skillRepository).deleteById(15L);
    }

    @Test
    void deleteSkillsByFreelancer_delegates() {
        skillService.deleteSkillsByFreelancer(4L);
        verify(skillRepository).deleteByFreelancerId(4L);
    }

    @Test
    void getAllSkills_returnsFindAll() {
        when(skillRepository.findAll()).thenReturn(List.of());

        assertThat(skillService.getAllSkills()).isEmpty();
        verify(skillRepository).findAll();
    }
}
