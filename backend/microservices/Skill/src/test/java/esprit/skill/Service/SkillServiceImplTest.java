package esprit.skill.Service;

import esprit.skill.Repositories.SkillRepository;
import esprit.skill.entities.Skill;
import esprit.skill.entities.SkillCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillServiceImpl service;

    @Test
    void addSkillRejectsDuplicatePerFreelancer() {
        Skill in = new Skill();
        in.setFreelancerId(3L);
        in.setName("Spring");
        when(skillRepository.existsByFreelancerIdAndName(3L, "Spring")).thenReturn(true);

        assertThatThrownBy(() -> service.addSkill(in))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getSkillByIdReturnsEntityWhenFound() {
        Skill s = new Skill();
        s.setId(12L);
        s.setName("Docker");
        s.setCategory(SkillCategory.DEVOPS);
        when(skillRepository.findById(12L)).thenReturn(Optional.of(s));

        Skill out = service.getSkillById(12L);

        assertThat(out.getName()).isEqualTo("Docker");
        assertThat(out.getCategory()).isEqualTo(SkillCategory.DEVOPS);
    }

    @Test
    void updateSkillCopiesEditableFields() {
        Skill existing = new Skill();
        existing.setId(5L);
        existing.setName("Java");
        existing.setCategory(SkillCategory.WEB_DEVELOPMENT);
        existing.setLevel("INTERMEDIATE");
        existing.setYearsOfExperience(2);

        Skill update = new Skill();
        update.setName("Java 21");
        update.setCategory(SkillCategory.WEB_DEVELOPMENT);
        update.setLevel("ADVANCED");
        update.setYearsOfExperience(4);

        when(skillRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(skillRepository.save(existing)).thenReturn(existing);

        Skill out = service.updateSkill(5L, update);

        assertThat(out.getName()).isEqualTo("Java 21");
        assertThat(out.getLevel()).isEqualTo("ADVANCED");
        assertThat(out.getYearsOfExperience()).isEqualTo(4);
        verify(skillRepository).save(existing);
    }
}
