package esprit.skill.Service;

import esprit.skill.Repositories.CVRepository;
import esprit.skill.entities.CV;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CVServiceImplTest {

    @Mock
    private CVRepository cvRepository;

    @InjectMocks
    private CVServiceImpl cvService;

    @TempDir
    Path tempDir;

    @Test
    void uploadCV_throwsWhenFileEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> cvService.uploadCV(10L, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    void uploadCV_createsNewCvWhenNotExisting() throws Exception {
        ReflectionTestUtils.setField(cvService, "uploadDirBase", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "my cv.pdf", "application/pdf", "content".getBytes());
        when(cvRepository.findByFreelancerId(5L)).thenReturn(Optional.empty());
        when(cvRepository.save(any(CV.class))).thenAnswer(i -> i.getArgument(0));

        CV saved = cvService.uploadCV(5L, file);

        assertThat(saved.getFreelancerId()).isEqualTo(5L);
        assertThat(saved.getFileName()).contains("my_cv.pdf");
        assertThat(saved.isParsed()).isFalse();
        assertThat(Files.exists(Path.of(saved.getFilePath()))).isTrue();
    }

    @Test
    void uploadCV_updatesExistingCvWhenPresent() throws Exception {
        ReflectionTestUtils.setField(cvService, "uploadDirBase", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "cv.docx", "application/octet-stream", "doc".getBytes());
        CV existing = new CV();
        existing.setId(1L);
        existing.setFreelancerId(8L);
        existing.setParsed(true);

        when(cvRepository.findByFreelancerId(8L)).thenReturn(Optional.of(existing));
        when(cvRepository.save(existing)).thenReturn(existing);

        CV out = cvService.uploadCV(8L, file);

        assertThat(out).isSameAs(existing);
        assertThat(out.getFilePath()).isNotBlank();
        assertThat(out.isParsed()).isTrue();
    }

    @Test
    void getCVByFreelancer_throwsWhenMissing() {
        when(cvRepository.findByFreelancerId(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cvService.getCVByFreelancer(77L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CV not found");
    }

    @Test
    void getCVByFreelancerOptional_delegates() {
        CV cv = new CV();
        cv.setFreelancerId(3L);
        when(cvRepository.findByFreelancerId(3L)).thenReturn(Optional.of(cv));

        assertThat(cvService.getCVByFreelancerOptional(3L)).contains(cv);
    }

    @Test
    void deleteCV_deletesWhenFound() {
        CV cv = new CV();
        cv.setId(9L);
        when(cvRepository.findByFreelancerId(2L)).thenReturn(Optional.of(cv));

        cvService.deleteCV(2L);

        verify(cvRepository).delete(cv);
    }

    @Test
    void getAllCVs_returnsRepositoryData() {
        CV cv = new CV();
        when(cvRepository.findAll()).thenReturn(List.of(cv));

        assertThat(cvService.getAllCVs()).hasSize(1);
    }
}
