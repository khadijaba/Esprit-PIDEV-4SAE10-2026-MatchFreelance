package com.freelancing.contract.service;

import com.freelancing.contract.client.UserClient;
import com.freelancing.contract.dto.ContractRequestDTO;
import com.freelancing.contract.dto.ContractResponseDTO;
import com.freelancing.contract.entity.Contract;
import com.freelancing.contract.enums.ContractStatus;
import com.freelancing.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getAllContracts() {
        return contractRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractResponseDTO getContractById(Long id) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        return toResponseDTO(c);
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getContractsByProjectId(Long projectId) {
        return contractRepository.findByProjectId(projectId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getContractsByFreelancerId(Long freelancerId) {
        return contractRepository.findByFreelancerId(freelancerId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getContractsByClientId(Long clientId) {
        return contractRepository.findByClientId(clientId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContractResponseDTO createContract(ContractRequestDTO dto) {
        // Un seul contrat « ouvert » par projet : ACTIVE ou DRAFT. COMPLETED / CANCELLED ne bloquent pas
        // une nouvelle acceptation (projet rouvert ou nouveau cycle).
        List<Contract> blocking = contractRepository.findByProjectIdAndStatusIn(
                dto.getProjectId(),
                EnumSet.of(ContractStatus.ACTIVE, ContractStatus.DRAFT));
        if (!blocking.isEmpty()) {
            throw new RuntimeException(
                    "Un contrat est déjà actif ou en brouillon pour le projet " + dto.getProjectId());
        }
        Contract c = new Contract();
        c.setProjectId(dto.getProjectId());
        c.setFreelancerId(dto.getFreelancerId());
        c.setClientId(dto.getClientId());
        c.setTerms(dto.getTerms());
        c.setProposedBudget(dto.getProposedBudget());
        c.setExtraTasksBudget(dto.getExtraTasksBudget());
        c.setApplicationMessage(dto.getApplicationMessage());
        c.setStatus(parseContractStatus(dto.getStatus(), ContractStatus.DRAFT));
        c.setStartDate(dto.getStartDate());
        c.setEndDate(dto.getEndDate());
        Contract saved = contractRepository.save(c);
        return toResponseDTO(saved, null);
    }

    @Transactional
    public ContractResponseDTO updateContract(Long id, ContractRequestDTO dto) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (dto.getTerms() != null) c.setTerms(dto.getTerms());
        if (dto.getProposedBudget() != null) c.setProposedBudget(dto.getProposedBudget());
        if (dto.getExtraTasksBudget() != null) c.setExtraTasksBudget(dto.getExtraTasksBudget());
        if (dto.getApplicationMessage() != null) c.setApplicationMessage(dto.getApplicationMessage());
        if (dto.getStartDate() != null) c.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) c.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            c.setStatus(parseContractStatus(dto.getStatus(), c.getStatus()));
        }
        return toResponseDTO(contractRepository.save(c), null);
    }

    @Transactional
    public ContractResponseDTO markAsCompleted(Long id) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (c.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Only ACTIVE contracts can be marked as paid");
        }
        c.setStatus(ContractStatus.COMPLETED);
        return toResponseDTO(contractRepository.save(c), null);
    }

    @Transactional
    public ContractResponseDTO markAsCancelled(Long id) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (c.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Only ACTIVE contracts can be cancelled");
        }
        c.setStatus(ContractStatus.CANCELLED);
        return toResponseDTO(contractRepository.save(c), null);
    }

    @Transactional
    public void deleteContract(Long id) {
        if (!contractRepository.existsById(id)) {
            throw new RuntimeException("Contract not found with id: " + id);
        }
        contractRepository.deleteById(id);
    }

    @Transactional
    public ContractResponseDTO proposeExtraBudget(Long contractId, Double amount, String reason, Long freelancerId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));
        if (!c.getFreelancerId().equals(freelancerId)) {
            throw new RuntimeException("Only the freelancer of this contract can propose extra budget");
        }
        if (c.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Only ACTIVE contracts can have extra budget proposals");
        }
        if (c.getPendingExtraAmount() != null) {
            throw new RuntimeException("A proposal is already pending. Wait for the client to respond.");
        }
        if (amount == null || amount <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        c.setPendingExtraAmount(amount);
        c.setPendingExtraReason(reason != null ? reason.trim() : null);
        c.setPendingExtraRequestedAt(new java.util.Date());
        return toResponseDTO(contractRepository.save(c), null);
    }

    @Transactional
    public ContractResponseDTO respondToExtraBudgetProposal(Long contractId, Long clientId, boolean accept) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));
        if (!c.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the client of this contract can respond to the proposal");
        }
        if (c.getPendingExtraAmount() == null) {
            throw new RuntimeException("No extra budget proposal pending");
        }
        if (accept) {
            double current = c.getExtraTasksBudget() != null ? c.getExtraTasksBudget() : 0;
            c.setExtraTasksBudget(current + c.getPendingExtraAmount());
        }
        c.setPendingExtraAmount(null);
        c.setPendingExtraReason(null);
        c.setPendingExtraRequestedAt(null);
        return toResponseDTO(contractRepository.save(c), null);
    }

    @Transactional
    public ContractResponseDTO updateProgress(Long contractId, Integer progressPercent, Long freelancerId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));
        if (!c.getFreelancerId().equals(freelancerId)) {
            throw new RuntimeException("Only the freelancer of this contract can update progress");
        }
        int percent = progressPercent == null ? 0 : Math.max(0, Math.min(100, progressPercent));
        c.setProgressPercent(percent);
        return toResponseDTO(contractRepository.save(c), null);
    }

    @Transactional
    public ContractResponseDTO rateContract(Long contractId, Long clientId, Integer rating, String review) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));
        if (!c.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the client of this contract can leave a rating");
        }
        if (c.getStatus() != ContractStatus.COMPLETED) {
            throw new RuntimeException("Only COMPLETED contracts can be rated");
        }
        if (c.getClientRating() != null) {
            throw new RuntimeException("This contract has already been rated");
        }
        int safeRating = rating == null ? 0 : Math.max(1, Math.min(5, rating));
        c.setClientRating(safeRating);
        c.setClientReview(review != null ? review.trim() : null);
        c.setClientReviewedAt(new java.util.Date());
        return toResponseDTO(contractRepository.save(c), null);
    }

    @Transactional(readOnly = true)
    public byte[] generateContractPdf(Long contractId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;
            float leading = 16;

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 18);
            content.newLineAtOffset(margin, y);
            content.showText("Contract #" + c.getId());
            content.endText();
            y -= 2 * leading;

            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(margin, y);
            content.showText("Project ID: " + c.getProjectId());
            content.endText();
            y -= leading;

            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(margin, y);
            content.showText("Client ID: " + c.getClientId() + "   Freelancer ID: " + c.getFreelancerId());
            content.endText();
            y -= leading;

            if (c.getStartDate() != null || c.getEndDate() != null) {
                String start = c.getStartDate() != null ? df.format(c.getStartDate()) : "-";
                String end = c.getEndDate() != null ? df.format(c.getEndDate()) : "-";
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(margin, y);
                content.showText("Period: " + start + " to " + end);
                content.endText();
                y -= leading;
            }

            if (c.getProposedBudget() != null) {
                double total = (c.getProposedBudget() != null ? c.getProposedBudget() : 0)
                        + (c.getExtraTasksBudget() != null ? c.getExtraTasksBudget() : 0);
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(margin, y);
                content.showText(String.format("Budget: %.0f$", total));
                content.endText();
                y -= leading;
            }

            if (c.getClientRating() != null) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(margin, y);
                content.showText("Client rating: " + c.getClientRating() + "/5");
                content.endText();
                y -= leading;
            }

            y -= leading;

            String terms = c.getTerms();
            if (terms != null && !terms.isBlank()) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(margin, y);
                content.showText("Terms");
                content.endText();
                y -= leading;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(margin, y);
                for (String line : terms.split("\\r?\\n")) {
                    content.showText(line);
                    content.newLineAtOffset(0, -leading);
                }
                content.endText();
            }

            content.close();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate contract PDF", e);
        }
    }

    private ContractResponseDTO toResponseDTO(Contract c) {
        return toResponseDTO(c, null);
    }

    private Map<Long, String> resolveUserNames(List<Contract> contracts) {
        List<Long> ids = contracts.stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getClientId(), c.getFreelancerId()))
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return userClient.getUsersByIds(ids).stream()
                .filter(u -> u != null && u.getId() != null && u.getName() != null)
                .collect(Collectors.toMap(UserClient.UserResponse::getId, UserClient.UserResponse::getName, (a, b) -> a));
    }

    private ContractResponseDTO toResponseDTO(Contract c, Map<Long, String> userNames) {
        ContractResponseDTO dto = new ContractResponseDTO();
        dto.setId(c.getId());
        dto.setProjectId(c.getProjectId());
        dto.setFreelancerId(c.getFreelancerId());
        dto.setClientId(c.getClientId());
        String freelancerName = null;
        String clientName = null;
        if (userNames != null) {
            freelancerName = c.getFreelancerId() != null ? userNames.get(c.getFreelancerId()) : null;
            clientName = c.getClientId() != null ? userNames.get(c.getClientId()) : null;
        }
        if (freelancerName == null && c.getFreelancerId() != null) {
            UserClient.UserResponse user = userClient.getUserById(c.getFreelancerId());
            freelancerName = user != null ? user.getName() : null;
        }
        if (clientName == null && c.getClientId() != null) {
            UserClient.UserResponse user = userClient.getUserById(c.getClientId());
            clientName = user != null ? user.getName() : null;
        }
        dto.setFreelancerName(freelancerName);
        dto.setClientName(clientName);
        dto.setTerms(c.getTerms());
        dto.setProposedBudget(c.getProposedBudget());
        dto.setExtraTasksBudget(c.getExtraTasksBudget());
        dto.setApplicationMessage(c.getApplicationMessage());
        dto.setStatus(c.getStatus());
        dto.setStartDate(c.getStartDate());
        dto.setEndDate(c.getEndDate());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setPendingExtraAmount(c.getPendingExtraAmount());
        dto.setPendingExtraReason(c.getPendingExtraReason());
        dto.setPendingExtraRequestedAt(c.getPendingExtraRequestedAt());
        dto.setProgressPercent(c.getProgressPercent());
        dto.setClientRating(c.getClientRating());
        dto.setClientReview(c.getClientReview());
        dto.setClientReviewedAt(c.getClientReviewedAt());
        return dto;
    }

    private static ContractStatus parseContractStatus(String raw, ContractStatus fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return ContractStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut de contrat invalide: " + raw);
        }
    }
}
