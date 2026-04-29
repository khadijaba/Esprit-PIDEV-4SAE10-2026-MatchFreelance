package esprit.skill.Service;

import esprit.skill.Repositories.PortfolioRepository;
import esprit.skill.entities.Portfolio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    @Test
    void addPortfolio_buildsAndSavesEntity() {
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArgument(0));

        Portfolio saved = portfolioService.addPortfolio(4L, "https://portfolio.dev", "fullstack");

        assertThat(saved.getFreelancerId()).isEqualTo(4L);
        assertThat(saved.getPortfolioUrl()).isEqualTo("https://portfolio.dev");
        assertThat(saved.getPortfolioDescription()).isEqualTo("fullstack");
    }

    @Test
    void getPortfoliosByFreelancer_returnsOrderedListFromRepository() {
        Portfolio p1 = new Portfolio();
        Portfolio p2 = new Portfolio();
        when(portfolioRepository.findAllByFreelancerIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(p1, p2));

        assertThat(portfolioService.getPortfoliosByFreelancer(10L)).containsExactly(p1, p2);
    }

    @Test
    void updatePortfolio_updatesAndSavesWhenFound() {
        Portfolio existing = new Portfolio();
        existing.setId(55L);
        existing.setPortfolioUrl("old");
        existing.setPortfolioDescription("old-desc");
        when(portfolioRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(portfolioRepository.save(existing)).thenReturn(existing);

        Portfolio out = portfolioService.updatePortfolio(55L, "new-url", "new-desc");

        assertThat(out.getPortfolioUrl()).isEqualTo("new-url");
        assertThat(out.getPortfolioDescription()).isEqualTo("new-desc");
    }

    @Test
    void updatePortfolio_throwsWhenNotFound() {
        when(portfolioRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.updatePortfolio(404L, "x", "y"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Portfolio not found");
    }

    @Test
    void deletePortfolio_deletesById() {
        portfolioService.deletePortfolio(22L);
        verify(portfolioRepository).deleteById(22L);
    }

    @Test
    void getAllPortfolios_returnsFindAll() {
        when(portfolioRepository.findAll()).thenReturn(List.of());
        assertThat(portfolioService.getAllPortfolios()).isEmpty();
    }
}
