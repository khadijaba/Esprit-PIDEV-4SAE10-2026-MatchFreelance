package com.freelancing.contract.service;

import com.freelancing.contract.dto.FinancialSummaryDTO;
import com.freelancing.contract.dto.PaymentMilestoneDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractFinancialServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ContractFinancialService contractFinancialService;

    private Contract testContract;

    @BeforeEach
    void setUp() {
        testContract = new Contract();
        testContract.setId(1L);
        testContract.setProposedBudget(1000.0);
        testContract.setExtraTasksBudget(200.0);
        testContract.setProgressPercent(50);
    }

    @Test
    void testGetFinancialSummary_Success() {
        // Arrange
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));

        // Act
        FinancialSummaryDTO result = contractFinancialService.getFinancialSummary(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getContractId());
        assertEquals(1000.0, result.getBaseBudget());
        assertEquals(200.0, result.getExtraTasksBudget());
        assertEquals(1200.0, result.getTotalContractValue());
        assertEquals(10.0, result.getPlatformFeePercent());
        assertEquals(120.0, result.getPlatformFeeAmount());
        assertEquals(1080.0, result.getFreelancerNetAmount());
        verify(contractRepository, times(1)).findById(1L);
    }

    @Test
    void testGetFinancialSummary_WithZeroProgress() {
        // Arrange
        testContract.setProgressPercent(0);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));

        // Act
        FinancialSummaryDTO result = contractFinancialService.getFinancialSummary(1L);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getAmountReleasableByProgress());
        verify(contractRepository, times(1)).findById(1L);
    }

    @Test
    void testGetFinancialSummary_With100PercentProgress() {
        // Arrange
        testContract.setProgressPercent(100);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));

        // Act
        FinancialSummaryDTO result = contractFinancialService.getFinancialSummary(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1200.0, result.getAmountReleasableByProgress());
        assertEquals(1200.0, result.getTotalContractValue());
        verify(contractRepository, times(1)).findById(1L);
    }

    @Test
    void testGetFinancialSummary_NotFound() {
        // Arrange
        when(contractRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            contractFinancialService.getFinancialSummary(999L);
        });
        verify(contractRepository, times(1)).findById(999L);
    }

    @Test
    void testGetPaymentSchedule_Success() {
        // Arrange
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));

        // Act
        List<PaymentMilestoneDTO> result = contractFinancialService.getPaymentSchedule(1L);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size());
        
        // Check first milestone (25%)
        PaymentMilestoneDTO milestone1 = result.get(0);
        assertEquals(1, milestone1.getMilestoneIndex());
        assertEquals(25, milestone1.getProgressPercentTrigger());
        assertTrue(milestone1.isReleased()); // Progress is 50%, so 25% is released
        
        // Check second milestone (50%)
        PaymentMilestoneDTO milestone2 = result.get(1);
        assertEquals(2, milestone2.getMilestoneIndex());
        assertEquals(50, milestone2.getProgressPercentTrigger());
        assertTrue(milestone2.isReleased()); // Progress is 50%, so 50% is released
        
        // Check third milestone (75%)
        PaymentMilestoneDTO milestone3 = result.get(2);
        assertEquals(3, milestone3.getMilestoneIndex());
        assertEquals(75, milestone3.getProgressPercentTrigger());
        assertFalse(milestone3.isReleased()); // Progress is 50%, so 75% is not released
        
        verify(contractRepository, times(1)).findById(1L);
    }

    @Test
    void testGetPaymentSchedule_WithNullBudgets() {
        // Arrange
        testContract.setProposedBudget(null);
        testContract.setExtraTasksBudget(null);
        testContract.setProgressPercent(0);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));

        // Act
        List<PaymentMilestoneDTO> result = contractFinancialService.getPaymentSchedule(1L);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size());
        // All amounts should be 0
        for (PaymentMilestoneDTO milestone : result) {
            assertEquals(0.0, milestone.getAmount());
            assertFalse(milestone.isReleased());
        }
        verify(contractRepository, times(1)).findById(1L);
    }

    @Test
    void testPlatformFeeCalculation() {
        // Arrange
        testContract.setProposedBudget(1000.0);
        testContract.setExtraTasksBudget(0.0);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));

        // Act
        FinancialSummaryDTO result = contractFinancialService.getFinancialSummary(1L);

        // Assert
        // 10% of 1000 = 100
        assertEquals(100.0, result.getPlatformFeeAmount());
        // Freelancer gets 1000 - 100 = 900
        assertEquals(900.0, result.getFreelancerNetAmount());
        verify(contractRepository, times(1)).findById(1L);
    }

    @Test
    void testMilestoneReleasedAtExactProgress() {
        // Arrange
        testContract.setProgressPercent(75); // Exactly at milestone
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));

        // Act
        List<PaymentMilestoneDTO> result = contractFinancialService.getPaymentSchedule(1L);

        // Assert
        // Milestones at 25%, 50%, 75% should be released
        assertTrue(result.get(0).isReleased()); // 25%
        assertTrue(result.get(1).isReleased()); // 50%
        assertTrue(result.get(2).isReleased()); // 75%
        assertFalse(result.get(3).isReleased()); // 100%
        verify(contractRepository, times(1)).findById(1L);
    }
}
