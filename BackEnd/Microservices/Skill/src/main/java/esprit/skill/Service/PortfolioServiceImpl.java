package esprit.skill.Service;

import esprit.skill.Repositories.PortfolioRepository;
import esprit.skill.entities.Portfolio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Override
    public Portfolio addPortfolio(Long freelancerId, String portfolioUrl, String portfolioDescription) {
        Portfolio portfolio = new Portfolio();
        portfolio.setFreelancerId(freelancerId);
        portfolio.setPortfolioUrl(portfolioUrl);
        portfolio.setPortfolioDescription(portfolioDescription);
        return portfolioRepository.save(portfolio);
    }

    @Override
    public List<Portfolio> getPortfoliosByFreelancer(Long freelancerId) {
        return portfolioRepository.findAllByFreelancerIdOrderByCreatedAtDesc(freelancerId);
    }

    @Override
    public Portfolio updatePortfolio(Long id, String portfolioUrl, String portfolioDescription) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
        portfolio.setPortfolioUrl(portfolioUrl);
        portfolio.setPortfolioDescription(portfolioDescription);
        return portfolioRepository.save(portfolio);
    }

    @Override
    public void deletePortfolio(Long id) {
        portfolioRepository.deleteById(id);
    }

    @Override
    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }
}
