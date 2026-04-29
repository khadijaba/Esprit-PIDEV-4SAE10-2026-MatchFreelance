package esprit.skill.Service;

import esprit.skill.Repositories.FreelancerBioRepository;
import esprit.skill.entities.FreelancerBio;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FreelancerBioServiceImpl implements FreelancerBioService {

    private final FreelancerBioRepository repository;

    public FreelancerBioServiceImpl(FreelancerBioRepository repository) {
        this.repository = repository;
    }

    @Override
    public FreelancerBio getOrEmpty(Long freelancerId) {
        return repository.findByFreelancerId(freelancerId)
                .orElseGet(() -> {
                    FreelancerBio b = new FreelancerBio();
                    b.setFreelancerId(freelancerId);
                    b.setBio("");
                    return b;
                });
    }

    @Override
    public FreelancerBio save(Long freelancerId, String bio) {
        Optional<FreelancerBio> existing = repository.findByFreelancerId(freelancerId);
        FreelancerBio entity = existing.orElseGet(FreelancerBio::new);
        entity.setFreelancerId(freelancerId);
        entity.setBio(bio != null ? bio : "");
        return repository.save(entity);
    }

    @Override
    public void delete(Long freelancerId) {
        repository.deleteByFreelancerId(freelancerId);
    }
}
