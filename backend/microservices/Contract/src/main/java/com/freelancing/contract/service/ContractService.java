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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            float margin = 42f;
            float pageWidth = page.getMediaBox().getWidth();
            float y = page.getMediaBox().getHeight() - margin;
            float leading = 16f;
            float contentWidth = pageWidth - (2 * margin);
            final int mfBlueR = 18, mfBlueG = 84, mfBlueB = 147;
            final int mfOrangeR = 241, mfOrangeG = 135, mfOrangeB = 37;

            String clientName = null;
            String freelancerName = null;
            try {
                if (c.getClientId() != null) {
                    UserClient.UserResponse user = userClient.getUserById(c.getClientId());
                    clientName = user != null ? user.getName() : null;
                }
                if (c.getFreelancerId() != null) {
                    UserClient.UserResponse user = userClient.getUserById(c.getFreelancerId());
                    freelancerName = user != null ? user.getName() : null;
                }
            } catch (Exception ignored) {
                // Keep PDF generation robust even if User service is unavailable.
            }

            // Header band
            content.setNonStrokingColor(mfBlueR, mfBlueG, mfBlueB);
            content.addRect(0, page.getMediaBox().getHeight() - 96, page.getMediaBox().getWidth(), 96);
            content.fill();
            content.setNonStrokingColor(255, 255, 255);

            drawLogoIfAvailable(document, content, margin, page.getMediaBox().getHeight() - 84, 124, 42);

            // Title
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 21);
            content.newLineAtOffset(margin + 132, page.getMediaBox().getHeight() - 46);
            content.showText("MatchFreelance Contract");
            content.endText();
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 10);
            content.newLineAtOffset(margin + 132, page.getMediaBox().getHeight() - 63);
            content.showText("Professional freelance agreement");
            content.endText();
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            content.newLineAtOffset(pageWidth - 170, page.getMediaBox().getHeight() - 46);
            content.showText("Ref: CT-" + c.getId());
            content.endText();
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 9);
            content.newLineAtOffset(pageWidth - 170, page.getMediaBox().getHeight() - 63);
            content.showText("Date: " + formatDate(new Date()));
            content.endText();

            content.setNonStrokingColor(0, 0, 0);

            y = page.getMediaBox().getHeight() - 122;

            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 15);
            content.setNonStrokingColor(mfBlueR, mfBlueG, mfBlueB);
            content.newLineAtOffset(margin, y);
            content.showText("Contract Summary");
            content.endText();
            content.setNonStrokingColor(0, 0, 0);
            y -= leading * 1.4f;

            // Light card around identity block
            float identityTop = y + 8f;
            float identityHeight = 120f;
            content.setNonStrokingColor(248, 250, 252);
            content.addRect(margin - 6f, identityTop - identityHeight, contentWidth + 12f, identityHeight);
            content.fill();
            content.setStrokingColor(226, 232, 240);
            content.addRect(margin - 6f, identityTop - identityHeight, contentWidth + 12f, identityHeight);
            content.stroke();
            content.setNonStrokingColor(0, 0, 0);
            content.setStrokingColor(0, 0, 0);

            y = writeLabelValue(content, margin, y, leading, "Contract ID", String.valueOf(c.getId()));
            y = writeLabelValue(content, margin, y, leading, "Project ID", String.valueOf(c.getProjectId()));
            y = writeLabelValue(content, margin, y, leading, "Client",
                (clientName != null ? clientName + " " : "") + "(ID " + c.getClientId() + ")");
            y = writeLabelValue(content, margin, y, leading, "Freelancer",
                (freelancerName != null ? freelancerName + " " : "") + "(ID " + c.getFreelancerId() + ")");
            y = writeLabelValue(content, margin, y, leading, "Status", String.valueOf(c.getStatus()));
            y = writeLabelValue(content, margin, y, leading, "Timeline",
                formatDate(c.getStartDate()) + " to " + formatDate(c.getEndDate()));
            y -= leading * 0.2f;

            // Section separator
            content.setStrokingColor(203, 213, 225);
            content.moveTo(margin, y + 4f);
            content.lineTo(pageWidth - margin, y + 4f);
            content.stroke();
            content.setStrokingColor(0, 0, 0);
            y -= leading * 0.3f;

            double proposed = c.getProposedBudget() != null ? c.getProposedBudget() : 0d;
            double extra = c.getExtraTasksBudget() != null ? c.getExtraTasksBudget() : 0d;
            double total = proposed + extra;
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 13);
            content.setNonStrokingColor(mfBlueR, mfBlueG, mfBlueB);
            content.newLineAtOffset(margin, y);
            content.showText("Financial Breakdown");
            content.endText();
            content.setNonStrokingColor(0, 0, 0);
            y -= leading * 1.2f;

            y = writeLabelValue(content, margin, y, leading, "Proposed budget", formatMoney(proposed));
            y = writeLabelValue(content, margin, y, leading, "Extra tasks budget", formatMoney(extra));
            y = writeLabelValue(content, margin, y, leading, "Total budget", formatMoney(total));
            y = writeLabelValue(content, margin, y, leading, "Progress", (c.getProgressPercent() != null ? c.getProgressPercent() : 0) + "%");
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
            content.setNonStrokingColor(mfOrangeR, mfOrangeG, mfOrangeB);
            content.newLineAtOffset(margin, y);
            content.showText("Estimated payable amount: " + formatMoney(total));
            content.endText();
            content.setNonStrokingColor(0, 0, 0);
            y -= leading * 1.1f;

            if (c.getClientRating() != null) {
                y = writeLabelValue(content, margin, y, leading, "Client rating", c.getClientRating() + "/5");
            }
            y -= leading * 0.8f;

            if (c.getApplicationMessage() != null && !c.getApplicationMessage().isBlank()) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 13);
                content.setNonStrokingColor(mfBlueR, mfBlueG, mfBlueB);
                content.newLineAtOffset(margin, y);
                content.showText("Application message");
                content.endText();
                content.setNonStrokingColor(0, 0, 0);
                y -= leading * 1.2f;
                y = writeWrappedText(content, margin, y, contentWidth, leading, c.getApplicationMessage());
                y -= leading * 0.6f;
            }

            String terms = c.getTerms();
            if (terms != null && !terms.isBlank()) {
                y -= leading * 0.2f;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 13);
                content.setNonStrokingColor(mfBlueR, mfBlueG, mfBlueB);
                content.newLineAtOffset(margin, y);
                content.showText("Contract terms");
                content.endText();
                content.setNonStrokingColor(0, 0, 0);
                y -= leading * 1.2f;
                y = writeWrappedText(content, margin, y, contentWidth, leading, terms);
            }

            y -= leading * 1.2f;
            if (y < 120) {
                y = 120;
            }

            // Signature block
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 12);
            content.setNonStrokingColor(mfBlueR, mfBlueG, mfBlueB);
            content.newLineAtOffset(margin, y);
            content.showText("Signatures");
            content.endText();
            content.setNonStrokingColor(0, 0, 0);
            y -= leading;

            float leftSigX = margin;
            float rightSigX = margin + (contentWidth / 2f) + 16f;
            float sigY = y - 18f;
            float sigWidth = (contentWidth / 2f) - 24f;
            content.moveTo(leftSigX, sigY);
            content.lineTo(leftSigX + sigWidth, sigY);
            content.moveTo(rightSigX, sigY);
            content.lineTo(rightSigX + sigWidth, sigY);
            content.stroke();

            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 10);
            content.newLineAtOffset(leftSigX, sigY - 14);
            content.showText("Client signature: " + (clientName != null ? clientName : "ID " + c.getClientId()));
            content.endText();

            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 10);
            content.newLineAtOffset(rightSigX, sigY - 14);
            content.showText("Freelancer signature: " + (freelancerName != null ? freelancerName : "ID " + c.getFreelancerId()));
            content.endText();
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
            content.newLineAtOffset(margin, sigY - 30);
            content.showText("This document is generated electronically and is valid as contractual evidence.");
            content.endText();

            // Footer
            float footerY = margin - 6;
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
            content.newLineAtOffset(margin, footerY);
            content.showText("Generated by MatchFreelance Contract Service on " + formatDate(new Date()));
            content.endText();
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
            content.newLineAtOffset(pageWidth - 150, footerY);
            content.showText("Page 1/1");
            content.endText();

            content.close();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate contract PDF", e);
        }
    }

    private static float writeLabelValue(PDPageContentStream content, float x, float y, float leading,
                                         String label, String value) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x, y);
        content.showText(label + ": ");
        content.endText();

        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 11);
        content.newLineAtOffset(x + 120, y);
        content.showText(value != null ? value : "-");
        content.endText();
        return y - leading;
    }

    private static float writeWrappedText(PDPageContentStream content, float x, float y, float width,
                                          float leading, String text) throws IOException {
        if (text == null || text.isBlank()) {
            return y;
        }
        content.setFont(PDType1Font.HELVETICA, 11);
        float fontWidthFactor = 5.2f; // approximation for Helvetica 11
        int maxCharsPerLine = Math.max(25, (int) (width / fontWidthFactor));
        String[] rawLines = text.replace("\r", "").split("\n");
        for (String raw : rawLines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                y -= leading;
                continue;
            }
            while (line.length() > maxCharsPerLine) {
                int breakAt = line.lastIndexOf(' ', maxCharsPerLine);
                if (breakAt <= 0) breakAt = maxCharsPerLine;
                String chunk = line.substring(0, breakAt).trim();
                content.beginText();
                content.newLineAtOffset(x, y);
                content.showText(chunk);
                content.endText();
                y -= leading;
                line = line.substring(breakAt).trim();
            }
            if (!line.isEmpty()) {
                content.beginText();
                content.newLineAtOffset(x, y);
                content.showText(line);
                content.endText();
                y -= leading;
            }
        }
        return y;
    }

    private static String formatDate(Date date) {
        return date == null ? "-" : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static String formatMoney(double amount) {
        return String.format("%.2f TND", amount);
    }

    private static void drawLogoIfAvailable(PDDocument document, PDPageContentStream content,
                                            float x, float y, float width, float height) {
        try {
            File logoFile = resolveLogoFile();
            if (logoFile == null) return;
            PDImageXObject logo = PDImageXObject.createFromFileByContent(logoFile, document);
            content.drawImage(logo, x, y, width, height);
        } catch (Exception ignored) {
            // Keep PDF generation resilient if logo file is missing/unreadable.
        }
    }

    private static File resolveLogoFile() {
        List<Path> candidates = List.of(
                Paths.get("src/main/resources/static/matchfreelance-logo.png"),
                Paths.get("../evaluation-reports-py/app/assets/logo.png"),
                Paths.get("../../evaluation-reports-py/app/assets/logo.png"),
                Paths.get("../../../evaluation-reports-py/app/assets/logo.png")
        );
        for (Path path : candidates) {
            File f = path.toFile();
            if (f.exists() && f.isFile()) {
                return f;
            }
        }
        return null;
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
