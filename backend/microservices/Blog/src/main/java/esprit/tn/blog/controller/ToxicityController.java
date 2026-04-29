package esprit.tn.blog.controller;

import esprit.tn.blog.service.ToxicityDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Toxicity Detection Controller
 * Provides endpoints for checking toxic/inappropriate content
 */
@RestController
@RequestMapping("/api/toxicity")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ToxicityController {

    private final ToxicityDetectionService toxicityDetectionService;

    /**
     * Check if text contains toxic content
     * 
     * @param request Map containing "text" field
     * @return Map with isToxic, confidence, reason, and detectedWords
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkToxicity(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Text is required",
                            "isToxic", false,
                            "confidence", 0.0,
                            "reason", "No text provided",
                            "detectedWords", java.util.List.of()
                    ));
        }

        log.info("Received toxicity check request, length: {}", text.length());
        
        Map<String, Object> result = toxicityDetectionService.checkToxicity(text);
        
        log.info("Toxicity check completed. Is toxic: {}", result.get("isToxic"));
        
        return ResponseEntity.ok(result);
    }
}
