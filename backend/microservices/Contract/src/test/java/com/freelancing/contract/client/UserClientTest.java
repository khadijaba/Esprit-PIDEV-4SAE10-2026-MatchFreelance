package com.freelancing.contract.client;

import com.freelancing.contract.client.dto.UserRemoteDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserClientTest {

    @Mock
    private UserRemoteFeign userRemoteFeign;

    @InjectMocks
    private UserClient userClient;

    @Test
    void getUserById_null_returnsNull() {
        assertThat(userClient.getUserById(null)).isNull();
    }

    @Test
    void getUserById_mapsRemoteDtoToResponse() {
        UserRemoteDto dto = new UserRemoteDto();
        dto.setId(10L);
        dto.setEmail("a@b.c");
        dto.setFullName("Ada Lovelace");
        dto.setRole("FREELANCER");
        when(userRemoteFeign.getUserById(10L)).thenReturn(dto);

        UserClient.UserResponse r = userClient.getUserById(10L);
        assertThat(r.getId()).isEqualTo(10L);
        assertThat(r.getEmail()).isEqualTo("a@b.c");
        assertThat(r.getName()).isEqualTo("Ada Lovelace");
        assertThat(r.getRole()).isEqualTo("FREELANCER");
    }

    @Test
    void getUserById_feignThrows_returnsNull() {
        when(userRemoteFeign.getUserById(1L)).thenThrow(new RuntimeException("down"));
        assertThat(userClient.getUserById(1L)).isNull();
    }

    @Test
    void getUsersByIds_nullOrEmpty_returnsEmpty() {
        assertThat(userClient.getUsersByIds(null)).isEmpty();
        assertThat(userClient.getUsersByIds(List.of())).isEmpty();
    }

    @Test
    void getUsersByIds_filtersNullsAndDuplicates() {
        UserRemoteDto dto = new UserRemoteDto();
        dto.setId(5L);
        dto.setEmail("x@y.z");
        dto.setFullName("Test");
        dto.setRole("CLIENT");
        when(userRemoteFeign.getUserById(5L)).thenReturn(dto);

        List<UserClient.UserResponse> list = userClient.getUsersByIds(Arrays.asList(5L, null, 5L));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(5L);
    }
}
