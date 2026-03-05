package com.freelancing.contract.controller;

import com.freelancing.contract.dto.ContractHealthDTO;
import com.freelancing.contract.dto.ContractRequestDTO;
import com.freelancing.contract.dto.ContractResponseDTO;
import com.freelancing.contract.dto.FinancialSummaryDTO;
import com.freelancing.contract.dto.PaymentMilestoneDTO;
import com.freelancing.contract.dto.ProposeExtraBudgetRequestDTO;
import com.freelancing.contract.dto.RespondExtraBudgetRequestDTO;
import com.freelancing.contract.dto.UpdateProgressRequestDTO;
import com.freelancing.contract.dto.RateContractRequestDTO;
import com.freelancing.contract.dto.CommunicationScoreDTO;
import com.freelancing.contract.service.ChatCommunicationScoreService;
import com.freelancing.contract.service.ContractFinancialService;
import com.freelancing.contract.service.ContractHealthService;
import com.freelancing.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContractController {

    private final ContractService contractService;
    private final ContractFinancialService contractFinancialService;
    private final ContractHealthService contractHealthService;
    private final ChatCommunicationScoreService chatCommunicationScoreService;

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

    @GetMapping("/freelancer/{freelancerId}/communication-score")
    public ResponseEntity<CommunicationScoreDTO> getFreelancerCommunicationScore(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(chatCommunicationScoreService.getCommunicationScore(freelancerId));
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

    @PutMapping("/{id}/propose-extra-budget")
    public ResponseEntity<ContractResponseDTO> proposeExtraBudget(
            @PathVariable Long id,
            @Valid @RequestBody ProposeExtraBudgetRequestDTO request) {
        try {
            return ResponseEntity.ok(contractService.proposeExtraBudget(
                    id, request.getAmount(), request.getReason(), request.getFreelancerId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/respond-extra-budget")
    public ResponseEntity<ContractResponseDTO> respondToExtraBudget(
            @PathVariable Long id,
            @Valid @RequestBody RespondExtraBudgetRequestDTO request) {
        try {
            return ResponseEntity.ok(contractService.respondToExtraBudgetProposal(
                    id, request.getClientId(), request.isAccept()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<ContractResponseDTO> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProgressRequestDTO request) {
        try {
            return ResponseEntity.ok(contractService.updateProgress(
                    id, request.getProgressPercent(), request.getFreelancerId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<ContractResponseDTO> rateContract(
            @PathVariable Long id,
            @Valid @RequestBody RateContractRequestDTO request) {
        try {
            return ResponseEntity.ok(contractService.rateContract(
                    id, request.getClientId(), request.getRating(), request.getReview()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadContractPdf(@PathVariable Long id) {
        byte[] pdf = contractService.generateContractPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("contract-" + id + ".pdf")
                        .build()
        );
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/financial-summary")
    public ResponseEntity<FinancialSummaryDTO> getFinancialSummary(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(contractFinancialService.getFinancialSummary(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/payment-schedule")
    public ResponseEntity<List<PaymentMilestoneDTO>> getPaymentSchedule(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(contractFinancialService.getPaymentSchedule(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<ContractHealthDTO> getContractHealth(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(contractHealthService.getContractHealth(id));
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
