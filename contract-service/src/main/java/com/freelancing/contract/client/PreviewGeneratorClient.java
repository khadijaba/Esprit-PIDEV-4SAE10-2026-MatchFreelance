package com.freelancing.contract.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for Node.js preview generator service that uses Anthropic SDK
 */
@Component
@RequiredArgsConstructor
public class PreviewGeneratorClient {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Generate content using Node.js service with Anthropic SDK
     * @param systemPrompt system message
     * @param userPrompt user message
     * @return generated content
     */
    public String chat(String systemPrompt, String userPrompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("systemPrompt", systemPrompt);
            body.put("userPrompt", userPrompt);
            body.put("designStyle", "modern");

            RestClient previewClient = restClientBuilder
                    .baseUrl("http://localhost:3001")
                    .build();

            String payload = objectMapper.writeValueAsString(body);
            
            String raw = previewClient.post()
                    .uri("/generate-preview")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Empty preview generator response");
            }

            JsonNode root = objectMapper.readTree(raw);
            
            if (root.path("success").asBoolean()) {
                String htmlContent = root.path("htmlContent").asText();
                if (htmlContent != null && !htmlContent.isBlank()) {
                    return htmlContent;
                }
            }

            String error = root.path("error").asText("Unknown error");
            throw new IllegalStateException("Preview generator error: " + error);

        } catch (RestClientException e) {
            throw new IllegalStateException("Preview generator request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Preview generator error: " + e.getMessage(), e);
        }
    }
}