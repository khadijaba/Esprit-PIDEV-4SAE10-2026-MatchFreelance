package tn.esprit.evaluation.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillClientTest {

    @Mock
    private SkillFeignApi feignApi;

    @InjectMocks
    private SkillClient skillClient;

    @Test
    void getSkillsByFreelancerReturnsDataWhenFeignSucceeds() {
        List<Map<String, Object>> expected = List.of(
                Map.of("id", 1L, "label", "Java"),
                Map.of("id", 2L, "label", "Angular")
        );
        when(feignApi.getSkillsByFreelancer(5L)).thenReturn(expected);

        List<Map<String, Object>> result = skillClient.getSkillsByFreelancer(5L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getSkillsByFreelancerReturnsEmptyListWhenFeignThrows() {
        when(feignApi.getSkillsByFreelancer(5L)).thenThrow(new RuntimeException("down"));

        List<Map<String, Object>> result = skillClient.getSkillsByFreelancer(5L);

        assertThat(result).isEmpty();
    }

    @Test
    void createSkillReturnsIdWhenNumber() {
        when(feignApi.createSkill(Map.of("name", "Docker"))).thenReturn(Map.of("id", 42L));

        Long result = skillClient.createSkill(Map.of("name", "Docker"));

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void createSkillParsesStringId() {
        when(feignApi.createSkill(Map.of("name", "Kubernetes"))).thenReturn(Map.of("id", "43"));

        Long result = skillClient.createSkill(Map.of("name", "Kubernetes"));

        assertThat(result).isEqualTo(43L);
    }

    @Test
    void createSkillReturnsNullWhenMissingId() {
        when(feignApi.createSkill(Map.of("name", "Go"))).thenReturn(Map.of("name", "Go"));

        Long result = skillClient.createSkill(Map.of("name", "Go"));

        assertThat(result).isNull();
    }

    @Test
    void createSkillReturnsNullWhenFeignThrows() {
        when(feignApi.createSkill(Map.of("name", "Rust"))).thenThrow(new RuntimeException("down"));

        Long result = skillClient.createSkill(Map.of("name", "Rust"));

        assertThat(result).isNull();
    }
}
