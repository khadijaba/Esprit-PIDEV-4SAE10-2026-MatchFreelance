package com.freelancing.contract.controller;

import com.freelancing.contract.dto.ContractCancelPartyRequestDTO;
import com.freelancing.contract.dto.ContractHealthDTO;
import com.freelancing.contract.dto.ContractPartyAmendRequestDTO;
import com.freelancing.contract.dto.ContractRequestDTO;
import com.freelancing.contract.dto.ContractResponseDTO;
import com.freelancing.contract.dto.FinancialSummaryDTO;
import com.freelancing.contract.dto.PaymentMilestoneDTO;
import com.freelancing.contract.dto.ProposeExtraBudgetRequestDTO;
import com.freelancing.contract.dto.RespondExtraBudgetRequestDTO;
import com.freelancing.contract.dto.UpdateProgressRequestDTO;
import com.freelancing.contract.dto.RateContractRequestDTO;
import com.freelancing.contract.dto.CommunicationScoreDTO;
import com.freelancing.contract.dto.ContractAiBriefingDTO;
import com.freelancing.contract.dto.ExtraBudgetAiAnalysisDTO;
import com.freelancing.contract.service.ChatCommunicationScoreService;
import com.freelancing.contract.service.ContractAiBriefingService;
import com.freelancing.contract.service.ExtraBudgetAiAnalysisService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContractController {

    private final ContractService contractService;
    private final ContractFinancialService contractFinancialService;
    private final ContractHealthService contractHealthService;
    private final ChatCommunicationScoreService chatCommunicationScoreService;
    private final ContractAiBriefingService contractAiBriefingService;
    private final ExtraBudgetAiAnalysisService extraBudgetAiAnalysisService;

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

    /**
     * Client or freelancer cancels a DRAFT or ACTIVE contract. Body must contain exactly one of
     * {@code clientId} or {@code freelancerId}, matching the contract.
     * Deletes the contract entirely.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelContract(
            @PathVariable Long id,
            @RequestBody ContractCancelPartyRequestDTO body) {
        try {
            contractService.cancelContractByParty(id, body);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Partial update by the client (terms, budget, dates, application message) or by the freelancer
     * (terms, application message). Only for DRAFT or ACTIVE.
     */
    @PatchMapping("/{id}/party-amend")
    public ResponseEntity<ContractResponseDTO> amendContractByParty(
            @PathVariable Long id,
            @Valid @RequestBody ContractPartyAmendRequestDTO body) {
        try {
            return ResponseEntity.ok(contractService.amendContractByParty(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
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

    /**
     * Local Ollama analysis before sending an extra-budget proposal: need, price fit, project alignment, tips.
     * Same body as {@link #proposeExtraBudget} — does not persist a proposal.
     */
    /** PUT aligns with {@link #proposeExtraBudget} and avoids rare proxy/gateway POST quirks on /api/contracts. */
    @PutMapping("/{id}/extra-budget-ai-analysis")
    public ResponseEntity<ExtraBudgetAiAnalysisDTO> analyzeExtraBudget(
            @PathVariable Long id, @Valid @RequestBody ProposeExtraBudgetRequestDTO request) {
        return ResponseEntity.ok(extraBudgetAiAnalysisService.analyze(id, request));
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
    public ResponseEntity<byte[]> downloadContractPdf(
            @PathVariable Long id,
            @RequestParam(required = false) String signature) {
        byte[] pdf = contractService.generateContractPdf(id, signature);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("contract-" + id + ".pdf")
                        .build()
        );
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadContractPdfWithSignature(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String signature = body != null ? body.get("signature") : null;
        byte[] pdf = contractService.generateContractPdf(id, signature);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("contract-" + id + "-signed.pdf")
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

    /**
     * Local Ollama AI briefing (contract + health + financial + recent messages).
     * Query: exactly one of viewerFreelancerId or viewerClientId must match the contract party.
     */
    @GetMapping("/{id}/ai-briefing")
    public ResponseEntity<ContractAiBriefingDTO> getAiBriefing(
            @PathVariable Long id,
            @RequestParam(required = false) Long viewerFreelancerId,
            @RequestParam(required = false) Long viewerClientId) {
        return ResponseEntity.ok(contractAiBriefingService.buildBriefing(id, viewerFreelancerId, viewerClientId));
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
