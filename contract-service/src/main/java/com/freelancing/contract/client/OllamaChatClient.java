package com.freelancing.contract.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freelancing.contract.config.ContractAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Minimal Ollama /api/chat client (local LLM). Expects JSON in assistant message when format=json.
 */
@Component
@RequiredArgsConstructor
public class OllamaChatClient {

    private final @Qualifier("ollamaRestClient") RestClient ollamaRestClient;
    private final ContractAiProperties contractAiProperties;
    private final ObjectMapper objectMapper;

    /**
     * @param systemPrompt system message
     * @param userPrompt   user message (JSON context)
     * @return raw assistant message content (should be JSON when format=json)
     */
    public String chatJson(String systemPrompt, String userPrompt) {
        ContractAiProperties.Ollama o = contractAiProperties.getOllama();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", o.getModel());
        body.put("stream", false);
        body.put("format", "json");

        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userPrompt);

        try {
            String payload = objectMapper.writeValueAsString(body);
            String raw = ollamaRestClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Empty Ollama response");
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("Ollama response missing message.content");
            }
            return content.asText();
        } catch (RestClientException e) {
            throw new IllegalStateException("Ollama request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Chat without forcing JSON format - for HTML generation
     * @param systemPrompt system message
     * @param userPrompt   user message
     * @return raw assistant message content (plain text/HTML)
     */
    public String chat(String systemPrompt, String userPrompt) {
        ContractAiProperties.Ollama o = contractAiProperties.getOllama();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", o.getModel());
        body.put("stream", false);
        // No format specified - allows free-form text/HTML response

        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userPrompt);

        try {
            String payload = objectMapper.writeValueAsString(body);
            String raw = ollamaRestClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Empty Ollama response");
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("Ollama response missing message.content");
            }
            return content.asText();
        } catch (RestClientException e) {
            throw new IllegalStateException("Ollama request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
