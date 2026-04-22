package esprit.project.ml;

import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectRiskFeatureVectorTest {

    @Test
    void build_returnsSevenFloats() {
        Project p = new Project();
        p.setTitle("Mon titre");
        p.setDescription("un deux trois quatre cinq");
        p.setBudget(10_000.0);
        p.setDuration(50);
        p.setStatus(ProjectStatus.OPEN);
        p.setProjectOwnerId(1L);
        p.setRequiredSkills(List.of("Java", "Spring"));

        float[] v = ProjectRiskFeatureVector.build(p, 3L, 10L);

        assertThat(v).hasSize(ProjectRiskFeatureVector.DIMENSION);
        assertThat(v[0]).isGreaterThan(0); // title length normalized
        assertThat(v[2]).isGreaterThan(0); // skills / 10
        assertThat(v[6]).isBetween(0.0f, 1.0f); // completion ratio
    }
}
