package com.freelancing.contract.service;

import com.freelancing.contract.dto.ContractRequestDTO;
import com.freelancing.contract.dto.ContractResponseDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.enums.ContractStatus;
import com.freelancing.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ContractService contractService;

    private Contract testContract;

    @BeforeEach
    void setUp() {
        testContract = new Contract();
        testContract.setId(1L);
        testContract.setProjectId(100L);
        testContract.setFreelancerId(200L);
        testContract.setClientId(300L);
        testContract.setStatus(ContractStatus.ACTIVE);
        testContract.setProposedBudget(1000.0);
        testContract.setTerms("Test contract terms");
    }

    @Test
    void testGetContractById_Success() {
        // Arrange
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));

        // Act
        ContractResponseDTO result = contractService.getContractById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getProjectId());
        assertEquals(200L, result.getFreelancerId());
        assertEquals(300L, result.getClientId());
        assertEquals(ContractStatus.ACTIVE, result.getStatus());
        verify(contractRepository, times(1)).findById(1L);
    }

    @Test
    void testGetContractById_NotFound() {
        // Arrange
        when(contractRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            contractService.getContractById(999L);
        });
        verify(contractRepository, times(1)).findById(999L);
    }

    @Test
    void testGetAllContracts() {
        // Arrange
        Contract contract2 = new Contract();
        contract2.setId(2L);
        contract2.setProjectId(101L);
        contract2.setFreelancerId(201L);
        contract2.setClientId(301L);
        contract2.setStatus(ContractStatus.DRAFT);

        when(contractRepository.findAll()).thenReturn(Arrays.asList(testContract, contract2));

        // Act
        List<ContractResponseDTO> results = contractService.getAllContracts();

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());
        verify(contractRepository, times(1)).findAll();
    }

    @Test
    void testGetContractsByProjectId() {
        // Arrange
        when(contractRepository.findByProjectId(100L)).thenReturn(Arrays.asList(testContract));

        // Act
        List<ContractResponseDTO> results = contractService.getContractsByProjectId(100L);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).getProjectId());
        verify(contractRepository, times(1)).findByProjectId(100L);
    }

    @Test
    void testGetContractsByFreelancerId() {
        // Arrange
        when(contractRepository.findByFreelancerId(200L)).thenReturn(Arrays.asList(testContract));

        // Act
        List<ContractResponseDTO> results = contractService.getContractsByFreelancerId(200L);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(200L, results.get(0).getFreelancerId());
        verify(contractRepository, times(1)).findByFreelancerId(200L);
    }

    @Test
    void testGetContractsByClientId() {
        // Arrange
        when(contractRepository.findByClientId(300L)).thenReturn(Arrays.asList(testContract));

        // Act
        List<ContractResponseDTO> results = contractService.getContractsByClientId(300L);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(300L, results.get(0).getClientId());
        verify(contractRepository, times(1)).findByClientId(300L);
    }

    @Test
    void testCreateContract_Success() {
        // Arrange
        ContractRequestDTO requestDTO = new ContractRequestDTO();
        requestDTO.setProjectId(100L);
        requestDTO.setFreelancerId(200L);
        requestDTO.setClientId(300L);
        requestDTO.setTerms("New contract terms");
        requestDTO.setProposedBudget(2000.0);
        requestDTO.setStatus(ContractStatus.DRAFT);

        when(contractRepository.save(any(Contract.class))).thenReturn(testContract);

        // Act
        ContractResponseDTO result = contractService.createContract(requestDTO);

        // Assert
        assertNotNull(result);
        verify(contractRepository, times(1)).save(any(Contract.class));
    }

    @Test
    void testCreateContract_DefaultStatus() {
        // Arrange
        ContractRequestDTO requestDTO = new ContractRequestDTO();
        requestDTO.setProjectId(100L);
        requestDTO.setFreelancerId(200L);
        requestDTO.setClientId(300L);
        requestDTO.setTerms("New contract terms");
        requestDTO.setProposedBudget(2000.0);
        // No status set - should default to DRAFT

        Contract savedContract = new Contract();
        savedContract.setId(1L);
        savedContract.setStatus(ContractStatus.DRAFT);
        savedContract.setProjectId(100L);
        savedContract.setFreelancerId(200L);
        savedContract.setClientId(300L);

        when(contractRepository.save(any(Contract.class))).thenReturn(savedContract);

        // Act
        ContractResponseDTO result = contractService.createContract(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(ContractStatus.DRAFT, result.getStatus());
        verify(contractRepository, times(1)).save(any(Contract.class));
    }
}
