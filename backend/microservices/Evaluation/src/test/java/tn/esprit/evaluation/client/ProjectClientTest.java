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
class ProjectClientTest {

    @Mock
    private ProjectFeignApi feignApi;

    @InjectMocks
    private ProjectClient projectClient;

    @Test
    void getProjectsByStatusReturnsDataWhenFeignSucceeds() {
        List<Map<String, Object>> expected = List.of(
                Map.of("id", 10L, "status", "OPEN"),
                Map.of("id", 11L, "status", "OPEN")
        );
        when(feignApi.getProjectsByStatus("OPEN")).thenReturn(expected);

        List<Map<String, Object>> result = projectClient.getProjectsByStatus("OPEN");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getProjectsByStatusReturnsEmptyListWhenFeignReturnsNull() {
        when(feignApi.getProjectsByStatus("OPEN")).thenReturn(null);

        List<Map<String, Object>> result = projectClient.getProjectsByStatus("OPEN");

        assertThat(result).isEmpty();
    }

    @Test
    void getProjectsByStatusReturnsEmptyListWhenFeignThrows() {
        when(feignApi.getProjectsByStatus("OPEN")).thenThrow(new RuntimeException("down"));

        List<Map<String, Object>> result = projectClient.getProjectsByStatus("OPEN");

        assertThat(result).isEmpty();
    }
}
