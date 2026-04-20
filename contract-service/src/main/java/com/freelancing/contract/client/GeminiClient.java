package com.freelancing.contract.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freelancing.contract.config.ContractAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Google Gemini API client for AI-powered content generation
 */
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final RestClient.Builder restClientBuilder;
    private final ContractAiProperties contractAiProperties;
    private final ObjectMapper objectMapper;

    /**
     * Generate content using Gemini API
     * @param systemPrompt system instructions
     * @param userPrompt user message
     * @return generated content
     */
    public String chat(String systemPrompt, String userPrompt) {
        ContractAiProperties.Gemini gemini = contractAiProperties.getGemini();
        
        if (!gemini.isEnabled()) {
            throw new IllegalStateException("Gemini AI is disabled");
        }

        try {
            // Build request body for Gemini API
            ObjectNode body = objectMapper.createObjectNode();
            
            // Combine system and user prompts into contents
            ArrayNode contents = body.putArray("contents");
            ObjectNode part = contents.addObject();
            ArrayNode parts = part.putArray("parts");
            ObjectNode textPart = parts.addObject();
            
            // Combine system prompt and user prompt
            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;
            textPart.put("text", combinedPrompt);
            
            // Generation config
            ObjectNode generationConfig = body.putObject("generationConfig");
            generationConfig.put("maxOutputTokens", gemini.getMaxTokens());
            generationConfig.put("temperature", 0.7);

            RestClient geminiClient = restClientBuilder
                    .baseUrl(gemini.getBaseUrl())
                    .build();

            String payload = objectMapper.writeValueAsString(body);
            
            // Gemini API uses query parameter for API key
            String uri = String.format("/v1beta/models/%s:generateContent?key=%s", 
                gemini.getModel(), gemini.getApiKey());
            
            String raw = geminiClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Empty Gemini response");
            }

            // Parse Gemini response
            JsonNode root = objectMapper.readTree(raw);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode responseParts = content.path("parts");
                
                if (responseParts.isArray() && responseParts.size() > 0) {
                    String text = responseParts.get(0).path("text").asText();
                    if (text != null && !text.isBlank()) {
                        return text;
                    }
                }
            }

            throw new IllegalStateException("Gemini response missing content");

        } catch (RestClientException e) {
            throw new IllegalStateException("Gemini request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Gemini error: " + e.getMessage(), e);
        }
    }
}
