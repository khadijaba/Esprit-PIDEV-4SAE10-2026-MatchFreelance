package esprit.skill.Service;

import esprit.skill.entities.FreelancerBio;

public interface FreelancerBioService {

    FreelancerBio getOrEmpty(Long freelancerId);

    FreelancerBio save(Long freelancerId, String bio);

    void delete(Long freelancerId);
}
