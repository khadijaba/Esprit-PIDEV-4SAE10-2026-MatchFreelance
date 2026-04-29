package esprit.tn.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Toxicity Detection Service using Gemini AI
 * Detects toxic, offensive, or inappropriate content
 */
@Service
@Slf4j
public class ToxicityDetectionService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Fallback bad words list (used if API fails)
    private static final Set<String> BAD_WORDS = Set.of(
        "fuck", "shit", "bitch", "asshole", "damn", "crap", "bastard",
        "idiot", "stupid", "dumb", "hate", "kill", "die", "racist",
        "sexist", "nazi", "terrorist", "violence", "abuse"
    );

    /**
     * Check if text contains toxic content using Gemini AI
     * Falls back to keyword matching if API fails
     * 
     * @param text Text to check
     * @return Map with isToxic, confidence, reason, and detectedWords
     */
    public Map<String, Object> checkToxicity(String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (text == null || text.trim().isEmpty()) {
            result.put("isToxic", false);
            result.put("confidence", 0.0);
            result.put("reason", "");
            result.put("detectedWords", List.of());
            return result;
        }

        log.info("[Toxicity Check] Analyzing text: {}", text.substring(0, Math.min(50, text.length())));

        // Try Gemini AI first
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                Map<String, Object> aiResult = checkWithGemini(text);
                if (aiResult != null) {
                    log.info("[Toxicity Check] Gemini result: {}", aiResult);
                    return aiResult;
                }
            } catch (Exception e) {
                log.warn("[Toxicity Check] Gemini API failed, falling back to keyword matching: {}", e.getMessage());
            }
        }

        // Fallback to keyword matching
        return checkWithKeywords(text);
    }

    /**
     * Check toxicity using Gemini AI
     */
    private Map<String, Object> checkWithGemini(String text) {
        try {
            String prompt = String.format("""
                Analyze this text for toxic, offensive, or inappropriate content.
                
                Text: "%s"
                
                Respond ONLY with valid JSON (no markdown, no ```):
                {
                  "isToxic": true/false,
                  "confidence": 0.0-1.0,
                  "reason": "brief explanation",
                  "detectedWords": ["word1", "word2"]
                }
                
                Consider toxic:
                - Hate speech, racism, sexism
                - Threats, violence, harassment
                - Profanity, insults, offensive language
                - Bullying, discrimination
                
                Respond with JSON only.
                """, text);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            Map<String, Object> content = new LinkedHashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            parts.add(Map.of("text", prompt));
            content.put("parts", parts);
            requestBody.put("contents", List.of(content));
            
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("temperature", 0.1);
            generationConfig.put("maxOutputTokens", 200);
            requestBody.put("generationConfig", generationConfig);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + geminiApiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                
                if (candidates.isArray() && candidates.size() > 0) {
                    String responseText = candidates.get(0)
                            .path("content")
                            .path("parts")
                            .get(0)
                            .path("text")
                            .asText();
                    
                    // Extract JSON from response
                    String cleanResponse = responseText.trim();
                    if (cleanResponse.contains("```json")) {
                        int start = cleanResponse.indexOf("```json") + 7;
                        int end = cleanResponse.indexOf("```", start);
                        if (end > start) {
                            cleanResponse = cleanResponse.substring(start, end).trim();
                        }
                    } else if (cleanResponse.contains("```")) {
                        int start = cleanResponse.indexOf("```") + 3;
                        int end = cleanResponse.indexOf("```", start);
                        if (end > start) {
                            cleanResponse = cleanResponse.substring(start, end).trim();
                        }
                    }
                    
                    int jsonStart = cleanResponse.indexOf("{");
                    int jsonEnd = cleanResponse.lastIndexOf("}");
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        cleanResponse = cleanResponse.substring(jsonStart, jsonEnd + 1);
                    }
                    
                    JsonNode aiResponse = objectMapper.readTree(cleanResponse);
                    
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("isToxic", aiResponse.path("isToxic").asBoolean(false));
                    result.put("confidence", aiResponse.path("confidence").asDouble(0.0));
                    result.put("reason", aiResponse.path("reason").asText(""));
                    
                    List<String> detectedWords = new ArrayList<>();
                    JsonNode wordsNode = aiResponse.path("detectedWords");
                    if (wordsNode.isArray()) {
                        wordsNode.forEach(word -> detectedWords.add(word.asText()));
                    }
                    result.put("detectedWords", detectedWords);
                    
                    return result;
                }
            } else {
                log.warn("[Toxicity Check] Gemini API error: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("[Toxicity Check] Gemini API exception: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Check toxicity using keyword matching (fallback)
     */
    private Map<String, Object> checkWithKeywords(String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> detectedWords = new ArrayList<>();
        
        String lowerText = text.toLowerCase();
        
        for (String badWord : BAD_WORDS) {
            if (lowerText.contains(badWord)) {
                detectedWords.add(badWord);
            }
        }
        
        boolean isToxic = !detectedWords.isEmpty();
        double confidence = isToxic ? 0.7 : 0.0; // Lower confidence for keyword matching
        
        result.put("isToxic", isToxic);
        result.put("confidence", confidence);
        result.put("reason", isToxic ? "Contains inappropriate language" : "No toxic content detected");
        result.put("detectedWords", detectedWords);
        
        log.info("[Toxicity Check] Keyword result: toxic={}, words={}", isToxic, detectedWords);
        
        return result;
    }
}
