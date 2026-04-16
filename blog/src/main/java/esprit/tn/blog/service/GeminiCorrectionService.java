package esprit.tn.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class GeminiCorrectionService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Takes raw text and returns correction suggestions using Google Gemini API.
     * Response map keys:
     *   - originalText: the original input
     *   - correctedText: the AI-corrected version
     *   - hasCorrections: boolean
     *   - explanation: explanation of what was wrong
     */
    public Map<String, Object> correctText(String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("originalText", text);

        System.out.println("[Gemini Correction] Processing text: " + text);
        System.out.println("[Gemini Correction] API Key configured: " + (geminiApiKey != null && !geminiApiKey.isBlank()));

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            System.out.println("[Gemini Correction] No API key configured");
            result.put("correctedText", text);
            result.put("hasCorrections", false);
            result.put("explanation", "Gemini API key not configured");
            return result;
        }

        try {
            // Simplified prompt for better results and lower token usage
            String prompt = String.format("""
                Fix all grammar and spelling errors in this text. Return ONLY the corrected text, nothing else.
                
                Text: %s
                
                Corrected text:""", text);

            // Build the Gemini API request body
            Map<String, Object> requestBody = new LinkedHashMap<>();
            
            Map<String, Object> content = new LinkedHashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            parts.add(Map.of("text", prompt));
            content.put("parts", parts);
            
            requestBody.put("contents", List.of(content));
            
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("temperature", 0.1);
            generationConfig.put("maxOutputTokens", 500);
            requestBody.put("generationConfig", generationConfig);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            System.out.println("[Gemini Correction] Sending request to Gemini API...");

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("[Gemini Correction] Gemini API response status: " + response.statusCode());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode responseContent = firstCandidate.path("content");
                    JsonNode responseParts = responseContent.path("parts");
                    
                    if (responseParts.isArray() && responseParts.size() > 0) {
                        String correctedText = responseParts.get(0).path("text").asText().trim();
                        
                        // Clean up the response
                        correctedText = correctedText.replace("Corrected text:", "").trim();
                        correctedText = correctedText.replaceAll("^[\"']|[\"']$", ""); // Remove quotes
                        
                        // Ensure we have valid corrected text
                        if (correctedText.isEmpty()) {
                            System.err.println("[Gemini Correction] Empty response from API");
                            correctedText = text;
                        }
                        
                        boolean hasCorrections = !correctedText.equals(text);
                        
                        result.put("correctedText", correctedText);
                        result.put("hasCorrections", hasCorrections);
                        result.put("explanation", hasCorrections ? "Text corrected by Gemini AI" : "No corrections needed");
                        
                        System.out.println("[Gemini Correction] Success - hasCorrections: " + hasCorrections);
                        System.out.println("[Gemini Correction] Corrected text length: " + correctedText.length());
                        return result;
                    }
                }
            } else if (response.statusCode() == 429) {
                System.err.println("[Gemini Correction] Rate limit exceeded - Please wait 60 seconds");
                result.put("correctedText", text);
                result.put("hasCorrections", false);
                result.put("rateLimitError", true);
                result.put("explanation", "Rate limit exceeded. Free tier allows 60 requests per minute. Please wait 60 seconds and try again.");
                return result;
            } else {
                System.err.println("[Gemini Correction] API error: " + response.statusCode() + " - " + response.body());
                result.put("correctedText", text);
                result.put("hasCorrections", false);
                result.put("explanation", "AI correction service error. Please try again later.");
                return result;
            }
        } catch (Exception e) {
            System.err.println("[Gemini Correction] Error: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback
        result.put("correctedText", text);
        result.put("hasCorrections", false);
        result.put("explanation", "AI correction service temporarily unavailable.");
        return result;
    }
}