package com.freelancing.contract.controller;

import com.freelancing.contract.dto.PreviewFeedbackDTO;
import com.freelancing.contract.dto.PreviewResponseDTO;
import com.freelancing.contract.dto.RegeneratePreviewRequestDTO;
import com.freelancing.contract.service.ContractPreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts/{contractId}/preview")
@RequiredArgsConstructor
public class ContractPreviewController {

    private final ContractPreviewService previewService;

    /**
     * Generate a new preview for a contract
     */
    @PostMapping
    public ResponseEntity<PreviewResponseDTO> generatePreview(
            @PathVariable Long contractId,
            @RequestParam(required = false, defaultValue = "modern") String designStyle
    ) {
        PreviewResponseDTO response = previewService.generatePreview(contractId, designStyle);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all previews for a contract
     */
    @GetMapping
    public ResponseEntity<List<PreviewResponseDTO>> getContractPreviews(
            @PathVariable Long contractId
    ) {
        List<PreviewResponseDTO> previews = previewService.getContractPreviews(contractId);
        return ResponseEntity.ok(previews);
    }

    /**
     * Get preview HTML content (for iframe rendering)
     */
    @GetMapping(value = "/{previewId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPreviewHtml(
            @PathVariable Long contractId,
            @PathVariable Long previewId
    ) {
        String html = previewService.getPreviewHtml(contractId, previewId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    /**
     * Submit client feedback on a preview
     */
    @PostMapping("/{previewId}/feedback")
    public ResponseEntity<Void> submitFeedback(
            @PathVariable Long contractId,
            @PathVariable Long previewId,
            @RequestBody PreviewFeedbackDTO feedback
    ) {
        previewService.submitFeedback(contractId, previewId, feedback);
        return ResponseEntity.ok().build();
    }

    /**
     * Regenerate preview with client feedback
     */
    @PostMapping("/{previewId}/regenerate")
    public ResponseEntity<PreviewResponseDTO> regenerateWithFeedback(
            @PathVariable Long contractId,
            @PathVariable Long previewId,
            @RequestBody RegeneratePreviewRequestDTO request
    ) {
        PreviewResponseDTO response = previewService.regenerateWithFeedback(contractId, previewId, request);
        return ResponseEntity.ok(response);
    }
}
