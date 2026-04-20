package esprit.tn.blog.controller;

import esprit.tn.blog.service.GeminiCorrectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI Text Correction Controller
 * Provides endpoints for AI-powered text correction using Gemini API
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AICorrectionController {

    private final GeminiCorrectionService geminiCorrectionService;

    /**
     * Correct text using Gemini AI
     * 
     * @param request Map containing "text" field
     * @return Map with correctedText, hasCorrections, and explanation
     */
    @PostMapping("/correct-text")
    public ResponseEntity<Map<String, Object>> correctText(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Text is required",
                            "correctedText", "",
                            "hasCorrections", false,
                            "explanation", "No text provided"
                    ));
        }

        log.info("Received text correction request, length: {}", text.length());
        
        Map<String, Object> result = geminiCorrectionService.correctText(text);
        
        log.info("Correction completed. Has corrections: {}", result.get("hasCorrections"));
        
        return ResponseEntity.ok(result);
    }
}
