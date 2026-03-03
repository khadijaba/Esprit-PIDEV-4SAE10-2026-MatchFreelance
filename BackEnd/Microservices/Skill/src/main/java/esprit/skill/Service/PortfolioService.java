package esprit.skill.Service;

import esprit.skill.entities.Portfolio;

import java.util.List;

public interface PortfolioService {

    Portfolio addPortfolio(Long freelancerId, String portfolioUrl, String portfolioDescription);

    List<Portfolio> getPortfoliosByFreelancer(Long freelancerId);

    Portfolio updatePortfolio(Long id, String portfolioUrl, String portfolioDescription);

    void deletePortfolio(Long id);

    List<Portfolio> getAllPortfolios();
}
