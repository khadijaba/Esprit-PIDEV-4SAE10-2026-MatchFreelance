package com.freelancing.contract.service;

import com.freelancing.contract.client.UserClient;
import com.freelancing.contract.dto.ContractCancelPartyRequestDTO;
import com.freelancing.contract.dto.ContractPartyAmendRequestDTO;
import com.freelancing.contract.dto.ContractRequestDTO;
import com.freelancing.contract.dto.ContractResponseDTO;
import com.freelancing.contract.dto.ContractSignatureStatusDTO;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HexFormat;

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
        Contract c = new Contract();
        c.setProjectId(dto.getProjectId());
        c.setFreelancerId(dto.getFreelancerId());
        c.setClientId(dto.getClientId());
        c.setTerms(dto.getTerms());
        c.setProposedBudget(dto.getProposedBudget());
        c.setExtraTasksBudget(dto.getExtraTasksBudget());
        c.setApplicationMessage(dto.getApplicationMessage());
        c.setStatus(dto.getStatus() != null ? dto.getStatus() : ContractStatus.DRAFT);
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
        if (dto.getStatus() != null) c.setStatus(dto.getStatus());
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

    /**
     * Client or freelancer cancels the contract while it is still {@link ContractStatus#DRAFT} or {@link ContractStatus#ACTIVE}.
     * Clears any pending extra-budget proposal.
     */
    @Transactional
    public ContractResponseDTO cancelContractByParty(Long id, ContractCancelPartyRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required with clientId or freelancerId");
        }
        boolean hasClient = dto.getClientId() != null;
        boolean hasFreelancer = dto.getFreelancerId() != null;
        if (hasClient == hasFreelancer) {
            throw new IllegalArgumentException("Provide exactly one of clientId or freelancerId");
        }
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (hasClient) {
            if (!c.getClientId().equals(dto.getClientId())) {
                throw new IllegalArgumentException("clientId does not match this contract's client");
            }
        } else {
            if (!c.getFreelancerId().equals(dto.getFreelancerId())) {
                throw new IllegalArgumentException("freelancerId does not match this contract's freelancer");
            }
        }
        if (c.getStatus() != ContractStatus.ACTIVE && c.getStatus() != ContractStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT or ACTIVE contracts can be cancelled by the parties");
        }
        clearPendingExtra(c);
        c.setStatus(ContractStatus.CANCELLED);
        return toResponseDTO(contractRepository.save(c), null);
    }

    /**
     * Party-scoped partial update (no status change). Allowed on DRAFT or ACTIVE only.
     */
    @Transactional
    public ContractResponseDTO amendContractByParty(Long id, ContractPartyAmendRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        boolean actorClient = dto.getActorClientId() != null;
        boolean actorFreelancer = dto.getActorFreelancerId() != null;
        if (actorClient == actorFreelancer) {
            throw new IllegalArgumentException("Provide exactly one of actorClientId or actorFreelancerId");
        }
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (c.getStatus() != ContractStatus.ACTIVE && c.getStatus() != ContractStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT or ACTIVE contracts can be amended by the parties");
        }
        if (actorClient) {
            if (!c.getClientId().equals(dto.getActorClientId())) {
                throw new IllegalArgumentException("actorClientId does not match this contract's client");
            }
            if (dto.getTerms() != null) c.setTerms(dto.getTerms());
            if (dto.getProposedBudget() != null) c.setProposedBudget(dto.getProposedBudget());
            if (dto.getStartDate() != null) c.setStartDate(dto.getStartDate());
            if (dto.getEndDate() != null) c.setEndDate(dto.getEndDate());
            if (dto.getApplicationMessage() != null) c.setApplicationMessage(dto.getApplicationMessage());
        } else {
            if (!c.getFreelancerId().equals(dto.getActorFreelancerId())) {
                throw new IllegalArgumentException("actorFreelancerId does not match this contract's freelancer");
            }
            if (dto.getTerms() != null) c.setTerms(dto.getTerms());
            if (dto.getApplicationMessage() != null) c.setApplicationMessage(dto.getApplicationMessage());
        }
        return toResponseDTO(contractRepository.save(c), null);
    }

    private static void clearPendingExtra(Contract c) {
        c.setPendingExtraAmount(null);
        c.setPendingExtraReason(null);
        c.setPendingExtraRequestedAt(null);
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

    @Transactional(readOnly = true)
    public ContractSignatureStatusDTO getSignatureStatus(Long contractId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));
        return ContractSignatureStatusDTO.builder()
                .contractId(c.getId())
                .clientId(c.getClientId())
                .clientSigned(c.getClientSignedAt() != null && c.getClientSignaturePng() != null && c.getClientSignatureHash() != null)
                .clientSignedAt(c.getClientSignedAt())
                .clientSignatureHash(c.getClientSignatureHash())
                .build();
    }

    @Transactional
    public ContractResponseDTO signAsClient(Long contractId, Long clientId, byte[] signaturePng, String ip, String userAgent) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));
        if (!c.getClientId().equals(clientId)) {
            throw new RuntimeException("Only the client of this contract can sign");
        }
        if (c.getStatus() != ContractStatus.DRAFT && c.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Only DRAFT or ACTIVE contracts can be signed");
        }
        if (c.getClientSignedAt() != null || c.getClientSignaturePng() != null) {
            throw new RuntimeException("Contract is already signed by the client");
        }
        if (signaturePng == null || signaturePng.length == 0) {
            throw new RuntimeException("Signature image is required");
        }
        // Simple sanity check: PNG magic header 89 50 4E 47 0D 0A 1A 0A
        if (signaturePng.length < 8
                || (signaturePng[0] & 0xFF) != 0x89
                || signaturePng[1] != 0x50
                || signaturePng[2] != 0x4E
                || signaturePng[3] != 0x47) {
            throw new RuntimeException("Signature must be a PNG image");
        }

        Date signedAt = new Date();
        String canonicalTerms = canonicalizeTerms(c.getTerms());
        String hash = computeSignatureHash(c.getId(), clientId, signedAt, canonicalTerms, signaturePng);

        c.setClientSignedAt(signedAt);
        c.setClientSignaturePng(signaturePng);
        c.setClientSignatureHash(hash);
        c.setClientSignatureIp(ip);
        c.setClientSignatureUserAgent(userAgent != null ? truncateUserAgent(userAgent) : null);

        return toResponseDTO(contractRepository.save(c), null);
    }

    private static String canonicalizeTerms(String terms) {
        if (terms == null) return "";
        // normalize line endings + trim trailing whitespace
        return terms.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

    private static String truncateUserAgent(String ua) {
        String t = ua.trim();
        return t.length() <= 255 ? t : t.substring(0, 255);
    }

    private static String computeSignatureHash(Long contractId, Long clientId, Date signedAt, String canonicalTerms, byte[] signaturePng) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(("contractId=" + contractId + "\n").getBytes(StandardCharsets.UTF_8));
            md.update(("clientId=" + clientId + "\n").getBytes(StandardCharsets.UTF_8));
            md.update(("signedAt=" + signedAt.getTime() + "\n").getBytes(StandardCharsets.UTF_8));
            md.update(("terms=" + canonicalTerms + "\n").getBytes(StandardCharsets.UTF_8));
            md.update(signaturePng);
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute signature hash", e);
        }
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
    public byte[] generateContractPdf(Long contractId, String signature) {
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
                    y -= leading;
                }
                content.endText();
            }

            // Add signature if provided (base64 PNG image)
            if (signature != null && !signature.isBlank()) {
                y -= 2 * leading;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                content.newLineAtOffset(margin, y);
                content.showText("Digital Signature:");
                content.endText();
                y -= leading;

                try {
                    // Decode base64 PNG signature
                    String base64Data = signature;
                    if (signature.startsWith("data:image/png;base64,")) {
                        base64Data = signature.substring("data:image/png;base64,".length());
                    }
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                    
                    // Create PDImageXObject from PNG bytes
                    org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage = 
                        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray(
                            document, imageBytes, "signature");
                    
                    // Calculate image dimensions (max width 200, maintain aspect ratio)
                    float maxWidth = 200;
                    float imageWidth = pdImage.getWidth();
                    float imageHeight = pdImage.getHeight();
                    float scale = Math.min(maxWidth / imageWidth, 1.0f);
                    float scaledWidth = imageWidth * scale;
                    float scaledHeight = imageHeight * scale;
                    
                    // Draw the signature image
                    content.drawImage(pdImage, margin, y - scaledHeight, scaledWidth, scaledHeight);
                    y -= scaledHeight + leading;
                } catch (Exception e) {
                    // Fallback to text if image processing fails
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA_OBLIQUE, 14);
                    content.newLineAtOffset(margin, y);
                    content.showText("[Signature image]");
                    content.endText();
                    y -= leading;
                }

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(margin, y);
                content.showText("Signed on: " + df.format(new java.util.Date()));
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
}
