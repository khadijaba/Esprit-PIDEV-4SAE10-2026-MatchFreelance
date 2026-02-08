package esprit.skill.Service;


import esprit.skill.entities.CV;
import org.springframework.web.multipart.MultipartFile;

public interface CVService {

    CV uploadCV(Long freelancerId, MultipartFile file) throws Exception;

    CV getCVByFreelancer(Long freelancerId);

    void deleteCV(Long freelancerId);
}

