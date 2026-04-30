package com.freelancing.contract.service;

import com.freelancing.contract.dto.ContractHealthDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.enums.ContractStatus;
import com.freelancing.contract.repository.ContractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractHealthServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ContractHealthService contractHealthService;

    @Test
    void getContractHealth_missingContract_throws() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> contractHealthService.getContractHealth(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Contract not found");
    }

    @Test
    void getContractHealth_completed_highRating() {
        Contract c = baseContract(1L, ContractStatus.COMPLETED);
        c.setClientRating(5);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(c));

        ContractHealthDTO dto = contractHealthService.getContractHealth(1L);
        assertThat(dto.getHealthScore()).isEqualTo(100);
        assertThat(dto.getHealthLevel()).isEqualTo("HEALTHY");
        assertThat(dto.getFlags()).contains("COMPLETED", "HIGH_RATING");
        assertThat(dto.getTimelineStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void getContractHealth_completed_lowRating_reducesScore() {
        Contract c = baseContract(2L, ContractStatus.COMPLETED);
        c.setClientRating(1);
        when(contractRepository.findById(2L)).thenReturn(Optional.of(c));

        ContractHealthDTO dto = contractHealthService.getContractHealth(2L);
        assertThat(dto.getHealthScore()).isEqualTo(70);
        assertThat(dto.getFlags()).contains("LOW_RATING");
    }

    @Test
    void getContractHealth_cancelled() {
        Contract c = baseContract(3L, ContractStatus.CANCELLED);
        when(contractRepository.findById(3L)).thenReturn(Optional.of(c));

        ContractHealthDTO dto = contractHealthService.getContractHealth(3L);
        assertThat(dto.getHealthScore()).isZero();
        assertThat(dto.getFlags()).containsExactly("CANCELLED");
    }

    @Test
    void getContractHealth_active_pendingExtra() {
        Contract c = baseContract(4L, ContractStatus.ACTIVE);
        c.setPendingExtraAmount(100.0);
        c.setProgressPercent(40);
        when(contractRepository.findById(4L)).thenReturn(Optional.of(c));

        ContractHealthDTO dto = contractHealthService.getContractHealth(4L);
        assertThat(dto.getHealthScore()).isEqualTo(85);
        assertThat(dto.getFlags()).contains("PENDING_EXTRA_BUDGET_DECISION");
    }

    @Test
    void getContractHealth_active_invalidDateRange() {
        Contract c = baseContract(5L, ContractStatus.ACTIVE);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        c.setStartDate(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, -2);
        c.setEndDate(cal.getTime());
        c.setProgressPercent(10);
        when(contractRepository.findById(5L)).thenReturn(Optional.of(c));

        ContractHealthDTO dto = contractHealthService.getContractHealth(5L);
        assertThat(dto.getTimelineStatus()).isEqualTo("INVALID_DATES");
        assertThat(dto.getFlags()).contains("INVALID_END_DATE");
    }

    @Test
    void getContractHealth_active_behindSchedule() {
        Contract c = baseContract(6L, ContractStatus.ACTIVE);
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, -20);
        Calendar end = Calendar.getInstance();
        end.add(Calendar.DAY_OF_MONTH, -5);
        c.setStartDate(start.getTime());
        c.setEndDate(end.getTime());
        c.setProgressPercent(50);
        when(contractRepository.findById(6L)).thenReturn(Optional.of(c));

        ContractHealthDTO dto = contractHealthService.getContractHealth(6L);
        assertThat(dto.getFlags()).contains("BEHIND_SCHEDULE");
        assertThat(dto.getTimelineStatus()).isEqualTo("BEHIND_SCHEDULE");
        assertThat(dto.getHealthScore()).isEqualTo(75);
    }

    @Test
    void getContractHealth_draft() {
        Contract c = baseContract(7L, ContractStatus.DRAFT);
        when(contractRepository.findById(7L)).thenReturn(Optional.of(c));

        ContractHealthDTO dto = contractHealthService.getContractHealth(7L);
        assertThat(dto.getHealthScore()).isEqualTo(50);
        assertThat(dto.getFlags()).contains("DRAFT_OR_INACTIVE");
        assertThat(dto.getHealthLevel()).isEqualTo("AT_RISK");
    }

    private static Contract baseContract(Long id, ContractStatus status) {
        Contract c = new Contract();
        c.setId(id);
        c.setProjectId(10L);
        c.setFreelancerId(20L);
        c.setClientId(30L);
        c.setStatus(status);
        c.setCreatedAt(new Date());
        return c;
    }
}
