package esprit.tn.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Post Summary Service
 * Uses Gemini AI to generate summaries of long posts/messages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostSummaryService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final int MIN_LENGTH_FOR_SUMMARY = 500; // Characters

    /**
     * Check if content needs a summary
     */
    public boolean needsSummary(String content) {
        return content != null && content.length() >= MIN_LENGTH_FOR_SUMMARY;
    }

    /**
     * Generate AI summary for long content
     */
    public String generateSummary(String content) {
        if (!needsSummary(content)) {
            return null;
        }

        try {
            String prompt = buildSummaryPrompt(content);
            String summary = callGeminiAPI(prompt);
            
            log.info("Generated summary for content of length: {}", content.length());
            return summary;
            
        } catch (Exception e) {
            log.error("Failed to generate summary: {}", e.getMessage());
            return generateFallbackSummary(content);
        }
    }

    /**
     * Build prompt for Gemini API
     */
    private String buildSummaryPrompt(String content) {
        return String.format(
            "Please provide a concise summary (2-3 sentences) of the following text. " +
            "Focus on the main points and key information:\n\n%s",
            content
        );
    }

    /**
     * Call Gemini API
     */
    private String callGeminiAPI(String prompt) {
        try {
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                throw new IllegalStateException("Gemini API key not configured");
            }
            String url = GEMINI_API_URL + "?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            }

            throw new RuntimeException("Failed to get response from Gemini API");

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate summary", e);
        }
    }

    /**
     * Extract text from Gemini API response
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract text from response: {}", e.getMessage());
        }
        return "Summary generation failed";
    }

    /**
     * Generate fallback summary (first few sentences)
     */
    private String generateFallbackSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // Take first 200 characters and add ellipsis
        int endIndex = Math.min(200, content.length());
        String summary = content.substring(0, endIndex);
        
        // Try to end at a sentence boundary
        int lastPeriod = summary.lastIndexOf('.');
        int lastQuestion = summary.lastIndexOf('?');
        int lastExclamation = summary.lastIndexOf('!');
        
        int lastSentenceEnd = Math.max(lastPeriod, Math.max(lastQuestion, lastExclamation));
        
        if (lastSentenceEnd > 50) {
            summary = summary.substring(0, lastSentenceEnd + 1);
        } else {
            summary += "...";
        }

        return summary;
    }

    /**
     * Generate summary asynchronously (for batch processing)
     */
    public void generateSummaryAsync(String content, SummaryCallback callback) {
        new Thread(() -> {
            try {
                String summary = generateSummary(content);
                callback.onSuccess(summary);
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    /**
     * Callback interface for async summary generation
     */
    public interface SummaryCallback {
        void onSuccess(String summary);
        void onError(Exception e);
    }
}
