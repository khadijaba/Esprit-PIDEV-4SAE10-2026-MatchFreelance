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
public class AiCorrectionService {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Takes raw text and returns correction suggestions.
     * Response map keys:
     *   - originalText: the original input
     *   - correctedText: the AI-corrected version
     *   - hasCorrections: boolean
     *   - explanation: explanation of what was wrong
     */
    public Map<String, Object> correctText(String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("originalText", text);

        System.out.println("[AI Correction] Processing text: " + text);
        System.out.println("[AI Correction] OpenAI API Key configured: " + (openaiApiKey != null && !openaiApiKey.isBlank()));

        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            System.out.println("[AI Correction] No API key configured");
            // No API key configured — return original text unchanged
            result.put("correctedText", text);
            result.put("hasCorrections", false);
            result.put("explanation", "");
            return result;
        }

        try {
            String systemPrompt = """
                You are a helpful writing assistant that corrects grammar, spelling, and language errors.
                The user will give you text that may be in English, French, or other languages.
                Your job:
                1. Detect the language of the text
                2. Check for grammar, spelling, punctuation, and word choice errors
                3. Provide the corrected version if there are errors
                4. If the text is already correct, say so
                
                For the text "je sommes dzl par la retard" the correct version should be "je suis désolé pour le retard"
                
                Always reply in this exact JSON format (no markdown, no code fences):
                {"correctedText": "the corrected text", "hasCorrections": true or false, "explanation": "brief explanation of what was fixed"}
                
                Be thorough in detecting errors - even informal abbreviations like "dzl" should be expanded to proper words.
                """;

            String userPrompt = "Please check and correct this text for grammar, spelling, and proper language usage: \"" + text + "\"";

            // Build the OpenAI API request body
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.1); // Lower temperature for more consistent corrections
            requestBody.put("max_tokens", 300);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            System.out.println("[AI Correction] Sending request to OpenAI API...");
            System.out.println("[AI Correction] Request body: " + jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("[AI Correction] OpenAI API response status: " + response.statusCode());
            System.out.println("[AI Correction] OpenAI API response body: " + response.body());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String content = root.path("choices").get(0).path("message").path("content").asText();

                // Parse the AI JSON response
                JsonNode aiResponse = objectMapper.readTree(content);
                result.put("correctedText", aiResponse.path("correctedText").asText(text));
                result.put("hasCorrections", aiResponse.path("hasCorrections").asBoolean(false));
                result.put("explanation", aiResponse.path("explanation").asText(""));
            } else {
                System.err.println("[AI Correction] OpenAI API error: " + response.statusCode() + " - " + response.body());
                
                // Handle specific error cases
                if (response.statusCode() == 429) {
                    result.put("correctedText", text);
                    result.put("hasCorrections", false);
                    result.put("explanation", "AI correction temporarily unavailable (rate limit). Please try again later.");
                } else if (response.statusCode() == 401) {
                    result.put("correctedText", text);
                    result.put("hasCorrections", false);
                    result.put("explanation", "AI correction unavailable (invalid API key).");
                } else {
                    result.put("correctedText", text);
                    result.put("hasCorrections", false);
                    result.put("explanation", "AI correction service error. Please try again later.");
                }
            }
        } catch (Exception e) {
            System.err.println("[AI Correction] Error: " + e.getMessage());
            e.printStackTrace();
            result.put("correctedText", text);
            result.put("hasCorrections", false);
            result.put("explanation", "");
        }

        return result;
    }
}
