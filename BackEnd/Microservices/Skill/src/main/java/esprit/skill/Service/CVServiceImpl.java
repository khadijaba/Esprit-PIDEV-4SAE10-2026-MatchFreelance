package esprit.skill.Service;


import esprit.skill.Repositories.CVRepository;
import esprit.skill.entities.CV;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Service
public class CVServiceImpl implements CVService {

    @Autowired
    private CVRepository cvRepository;

    private final String uploadDir = "uploads/";

    @Override
    public CV uploadCV(Long freelancerId, MultipartFile file) throws IOException {

        String fileName = file.getOriginalFilename();
        String filePath = uploadDir + fileName;

        File dest = new File(filePath);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        Optional<CV> existingCV = cvRepository.findByFreelancerId(freelancerId);

        CV cv;
        if (existingCV.isPresent()) {
            cv = existingCV.get();
            cv.setFileName(fileName);
            cv.setFilePath(filePath);
        } else {
            cv = new CV();
            cv.setFreelancerId(freelancerId);
            cv.setFileName(fileName);
            cv.setFilePath(filePath);
            cv.setParsed(false);
        }

        return cvRepository.save(cv);
    }

    @Override
    public CV getCVByFreelancer(Long freelancerId) {
        return cvRepository.findByFreelancerId(freelancerId)
                .orElseThrow(() -> new RuntimeException("CV not found"));
    }

    @Override
    public void deleteCV(Long freelancerId) {
        cvRepository.deleteByFreelancerId(freelancerId);
    }
}

