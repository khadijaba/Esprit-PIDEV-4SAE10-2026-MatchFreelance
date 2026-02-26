package com.freelancing.contract.controller;

import com.freelancing.contract.dto.ContractRequestDTO;
import com.freelancing.contract.dto.ContractResponseDTO;
import com.freelancing.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public ResponseEntity<List<ContractResponseDTO>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> getContractById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(contractService.getContractById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ContractResponseDTO>> getContractsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(contractService.getContractsByProjectId(projectId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<ContractResponseDTO>> getContractsByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(contractService.getContractsByFreelancerId(freelancerId));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ContractResponseDTO>> getContractsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(contractService.getContractsByClientId(clientId));
    }

    @PostMapping
    public ResponseEntity<ContractResponseDTO> createContract(@Valid @RequestBody ContractRequestDTO request) {
        try {
            ContractResponseDTO created = contractService.createContract(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody ContractRequestDTO request) {
        try {
            return ResponseEntity.ok(contractService.updateContract(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<ContractResponseDTO> payContract(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(contractService.markAsCompleted(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ContractResponseDTO> cancelContract(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(contractService.markAsCancelled(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        try {
            contractService.deleteContract(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
