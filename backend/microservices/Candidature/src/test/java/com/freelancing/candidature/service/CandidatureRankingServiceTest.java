package com.freelancing.candidature.service;

import com.freelancing.candidature.client.ContractClient;
import com.freelancing.candidature.client.ProjectClient;
import com.freelancing.candidature.client.UserClient;
import com.freelancing.candidature.dto.BudgetStatsDTO;
import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.repository.CandidatureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatureRankingServiceTest {

    @Mock
    private CandidatureRepository candidatureRepository;
    @Mock
    private ProjectClient projectClient;
    @Mock
    private UserClient userClient;
    @Mock
    private PitchAnalyzerService pitchAnalyzerService;
    @Mock
    private ContractClient contractClient;

    @InjectMocks
    private CandidatureRankingService candidatureRankingService;

    @Test
    void getBudgetStats_requiresClientId() {
        assertThatThrownBy(() -> candidatureRankingService.getBudgetStats(1L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("clientId is required");
    }

    @Test
    void getBudgetStats_projectNotFound() {
        when(projectClient.getProjectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> candidatureRankingService.getBudgetStats(1L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void getBudgetStats_forbiddenWhenNotOwner() {
        ProjectClient.ProjectResponse p = new ProjectClient.ProjectResponse();
        p.setId(1L);
        p.setClientId(99L);
        when(projectClient.getProjectById(1L)).thenReturn(p);

        assertThatThrownBy(() -> candidatureRankingService.getBudgetStats(1L, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only the project owner");
    }

    @Test
    void getBudgetStats_emptyCandidatures() {
        stubOwnerProject(2L, 20L);
        when(candidatureRepository.findByProjectId(2L)).thenReturn(List.of());

        BudgetStatsDTO dto = candidatureRankingService.getBudgetStats(2L, 20L);
        assertThat(dto.getProjectId()).isEqualTo(2L);
        assertThat(dto.getCandidatureCount()).isZero();
        assertThat(dto.getMinProposedBudget()).isNull();
        assertThat(dto.getAverageProposedBudget()).isNull();
    }

    @Test
    void getBudgetStats_computesDistribution() {
        stubOwnerProject(3L, 30L);
        when(candidatureRepository.findByProjectId(3L)).thenReturn(List.of(
                candidature(1L, 100.0),
                candidature(2L, 200.0),
                candidature(3L, 300.0)
        ));

        BudgetStatsDTO dto = candidatureRankingService.getBudgetStats(3L, 30L);
        assertThat(dto.getCandidatureCount()).isEqualTo(3);
        assertThat(dto.getMinProposedBudget()).isEqualTo(100.0);
        assertThat(dto.getMaxProposedBudget()).isEqualTo(300.0);
        assertThat(dto.getAverageProposedBudget()).isEqualTo(200.0);
        assertThat(dto.getMedianProposedBudget()).isEqualTo(200.0);
        assertThat(dto.getPercentile25()).isEqualTo(150.0);
        assertThat(dto.getPercentile75()).isEqualTo(250.0);
        assertThat(dto.getRecommendedMin()).isEqualTo(150.0);
        assertThat(dto.getRecommendedMax()).isEqualTo(250.0);
        assertThat(dto.getStandardDeviation()).isGreaterThan(0);
    }

    private void stubOwnerProject(Long projectId, Long clientId) {
        ProjectClient.ProjectResponse p = new ProjectClient.ProjectResponse();
        p.setId(projectId);
        p.setClientId(clientId);
        when(projectClient.getProjectById(projectId)).thenReturn(p);
    }

    private static Candidature candidature(Long id, Double budget) {
        Candidature c = new Candidature();
        c.setId(id);
        c.setProjectId(3L);
        c.setFreelancerId(id + 100);
        c.setProposedBudget(budget);
        return c;
    }
}
