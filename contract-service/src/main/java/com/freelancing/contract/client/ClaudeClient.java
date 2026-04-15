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
 * Claude API client for AI-powered content generation
 */
@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private final RestClient.Builder restClientBuilder;
    private final ContractAiProperties contractAiProperties;
    private final ObjectMapper objectMapper;

    /**
     * Generate content using Claude API
     * @param systemPrompt system message
     * @param userPrompt user message
     * @return generated content
     */
    public String chat(String systemPrompt, String userPrompt) {
        ContractAiProperties.Claude claude = contractAiProperties.getClaude();
        
        if (!claude.isEnabled()) {
            throw new IllegalStateException("Claude AI is disabled");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", claude.getModel());
        body.put("max_tokens", claude.getMaxTokens());
        body.put("system", systemPrompt);

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        try {
            RestClient claudeClient = restClientBuilder
                    .baseUrl(claude.getBaseUrl())
                    .defaultHeader("x-api-key", claude.getApiKey())
                    .defaultHeader("anthropic-version", claude.getVersion())
                    .build();

            String payload = objectMapper.writeValueAsString(body);
            
            String raw = claudeClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Empty Claude response");
            }

            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("content");
            
            if (content.isArray() && content.size() > 0) {
                JsonNode firstContent = content.get(0);
                String text = firstContent.path("text").asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }

            throw new IllegalStateException("Claude response missing content");

        } catch (RestClientException e) {
            throw new IllegalStateException("Claude request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Claude error: " + e.getMessage(), e);
        }
    }
}
