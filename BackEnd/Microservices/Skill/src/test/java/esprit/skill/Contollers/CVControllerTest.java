package esprit.skill.Contollers;

import esprit.skill.Service.CVService;
import esprit.skill.entities.CV;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CVControllerTest {

    @Mock
    private CVService cvService;

    @InjectMocks
    private CVController controller;

    @Test
    void uploadCV_returnsSavedCv_whenServiceSucceeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "pdf".getBytes());
        CV cv = new CV();
        cv.setFreelancerId(3L);
        when(cvService.uploadCV(3L, file)).thenReturn(cv);

        CV out = controller.uploadCV(3L, file);

        assertThat(out.getFreelancerId()).isEqualTo(3L);
    }

    @Test
    void uploadCV_wrapsServiceException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "pdf".getBytes());
        when(cvService.uploadCV(9L, file)).thenThrow(new RuntimeException("disk error"));

        assertThatThrownBy(() -> controller.uploadCV(9L, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("disk error");
    }

    @Test
    void getCV_returnsNotFound_whenCvMissing() {
        when(cvService.getCVByFreelancerOptional(1L)).thenReturn(Optional.empty());

        var response = controller.getCV(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteAndList_delegateToService() {
        when(cvService.getAllCVs()).thenReturn(List.of());

        var deleteResponse = controller.deleteCV(5L);
        var list = controller.getAllCVs();

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(list).isEmpty();
        verify(cvService).deleteCV(5L);
    }
}
