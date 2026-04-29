package esprit.skill.Service;

import esprit.skill.Repositories.FreelancerBioRepository;
import esprit.skill.entities.FreelancerBio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreelancerBioServiceImplTest {

    @Mock
    private FreelancerBioRepository repository;

    @InjectMocks
    private FreelancerBioServiceImpl service;

    @Test
    void getOrEmpty_returnsExistingBio() {
        FreelancerBio bio = new FreelancerBio();
        bio.setFreelancerId(3L);
        bio.setBio("existing");
        when(repository.findByFreelancerId(3L)).thenReturn(Optional.of(bio));

        FreelancerBio out = service.getOrEmpty(3L);

        assertThat(out).isSameAs(bio);
    }

    @Test
    void getOrEmpty_returnsDefaultWhenMissing() {
        when(repository.findByFreelancerId(9L)).thenReturn(Optional.empty());

        FreelancerBio out = service.getOrEmpty(9L);

        assertThat(out.getFreelancerId()).isEqualTo(9L);
        assertThat(out.getBio()).isEmpty();
    }

    @Test
    void save_updatesExistingEntity() {
        FreelancerBio existing = new FreelancerBio();
        existing.setId(1L);
        existing.setFreelancerId(12L);
        existing.setBio("old");
        when(repository.findByFreelancerId(12L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        FreelancerBio out = service.save(12L, "new-bio");

        assertThat(out.getBio()).isEqualTo("new-bio");
        assertThat(out.getFreelancerId()).isEqualTo(12L);
    }

    @Test
    void save_normalizesNullBioToEmptyString() {
        when(repository.findByFreelancerId(7L)).thenReturn(Optional.empty());
        when(repository.save(any(FreelancerBio.class))).thenAnswer(i -> i.getArgument(0));

        FreelancerBio out = service.save(7L, null);

        assertThat(out.getFreelancerId()).isEqualTo(7L);
        assertThat(out.getBio()).isEmpty();
    }

    @Test
    void delete_deletesByFreelancerId() {
        service.delete(30L);
        verify(repository).deleteByFreelancerId(30L);
    }
}
