package esprit.skill.Contollers;

import esprit.skill.Service.PortfolioService;
import esprit.skill.entities.Portfolio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private PortfolioController controller;

    @Test
    void addPortfolio_throwsWhenBodyMissing() {
        assertThatThrownBy(() -> controller.addPortfolio(1L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Body required");
    }

    @Test
    void addPortfolio_throwsWhenUrlMissing() {
        Map<String, Object> body = Map.of("portfolioDescription", "demo");

        assertThatThrownBy(() -> controller.addPortfolio(2L, body))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("portfolioUrl is required");
    }

    @Test
    void addPortfolio_trimsFieldsAndDelegates() {
        Portfolio saved = new Portfolio();
        saved.setFreelancerId(5L);
        saved.setPortfolioUrl("https://site.dev");
        saved.setPortfolioDescription("my work");
        when(portfolioService.addPortfolio(5L, "https://site.dev", "my work")).thenReturn(saved);

        Portfolio out = controller.addPortfolio(5L, Map.of(
                "portfolioUrl", "  https://site.dev  ",
                "portfolioDescription", "  my work  "
        ));

        assertThat(out).isSameAs(saved);
        verify(portfolioService).addPortfolio(5L, "https://site.dev", "my work");
    }

    @Test
    void updatePortfolio_usesNullDescriptionWhenBlank() {
        Portfolio updated = new Portfolio();
        updated.setId(10L);
        updated.setPortfolioUrl("https://new.dev");
        updated.setPortfolioDescription(null);
        when(portfolioService.updatePortfolio(10L, "https://new.dev", null)).thenReturn(updated);

        Portfolio out = controller.updatePortfolio(10L, Map.of(
                "portfolioUrl", "https://new.dev",
                "portfolioDescription", "   "
        ));

        assertThat(out.getId()).isEqualTo(10L);
        verify(portfolioService).updatePortfolio(10L, "https://new.dev", null);
    }

    @Test
    void listAndDeleteEndpoints_delegateToService() {
        when(portfolioService.getPortfoliosByFreelancer(7L)).thenReturn(List.of());
        when(portfolioService.getAllPortfolios()).thenReturn(List.of());

        assertThat(controller.getPortfolios(7L)).isEmpty();
        assertThat(controller.getAllPortfolios()).isEmpty();
        controller.deletePortfolio(12L);

        verify(portfolioService).deletePortfolio(12L);
    }
}
