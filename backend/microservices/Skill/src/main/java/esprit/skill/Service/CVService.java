package esprit.skill.Service;


import esprit.skill.entities.CV;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface CVService {

    CV uploadCV(Long freelancerId, MultipartFile file) throws Exception;

    CV getCVByFreelancer(Long freelancerId);

    Optional<CV> getCVByFreelancerOptional(Long freelancerId);

    void deleteCV(Long freelancerId);

    List<CV> getAllCVs();
}

