package esprit.skill.Service;

import esprit.skill.Repositories.CVRepository;
import esprit.skill.entities.CV;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class CVServiceImpl implements CVService {

    @Autowired
    private CVRepository cvRepository;

    @Value("${file.upload.dir:}")
    private String uploadDirBase;

    /** Répertoire d'upload en chemin absolu pour éviter les erreurs avec le répertoire de travail Tomcat. */
    private Path uploadDirPath() throws IOException {
        String base = uploadDirBase != null && !uploadDirBase.trim().isEmpty()
                ? uploadDirBase.trim()
                : (System.getProperty("user.home") + "/matchfreelance-uploads");
        Path path = Paths.get(base).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }

    @Override
    public CV uploadCV(Long freelancerId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new RuntimeException("File is empty");

        Path dir = uploadDirPath();

        String safeName = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "document";
        String fileName = System.currentTimeMillis() + "_" + safeName;
        Path dest = dir.resolve(fileName);

        try {
            Files.copy(file.getInputStream(), dest);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }

        String filePath = dest.toAbsolutePath().toString();

        // Vérification si le CV existe déjà pour ce freelancer
        Optional<CV> existingCV = cvRepository.findByFreelancerId(freelancerId);

        CV cv;
        if (existingCV.isPresent()) {
            cv = existingCV.get();
            cv.setFileName(fileName);
            cv.setFilePath(filePath);
        } else {
            cv = new CV();
            cv.setFreelancerId(freelancerId); // ou setFreelancer si relation ManyToOne
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
    public Optional<CV> getCVByFreelancerOptional(Long freelancerId) {
        return cvRepository.findByFreelancerId(freelancerId);
    }

    @Override
    public void deleteCV(Long freelancerId) {
        cvRepository.findByFreelancerId(freelancerId).ifPresent(cv -> cvRepository.delete(cv));
    }

    @Override
    public List<CV> getAllCVs() {
        return cvRepository.findAll();
    }
}
