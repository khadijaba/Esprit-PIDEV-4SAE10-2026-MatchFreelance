package com.freelancing.contract.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI settings for contract services.
 * Supports Ollama (local), Claude (API), and Gemini (API) for AI generation.
 */
@Data
@ConfigurationProperties(prefix = "contract.ai")
public class ContractAiProperties {

    private Ollama ollama = new Ollama();
    private Claude claude = new Claude();
    private Gemini gemini = new Gemini();

    @Data
    public static class Ollama {
        /** Set true locally after installing Ollama; keep false in shared CI/production unless Ollama is available. */
        private boolean enabled = false;
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.2:1b";
        private int maxMessages = 40;
        private int maxCharsPerMessage = 800;
        /** HTTP read timeout for LLM generation (seconds). */
        private int readTimeoutSeconds = 120;
        private int connectTimeoutSeconds = 10;
    }

    @Data
    public static class Claude {
        /** Set true to use Claude API instead of Ollama */
        private boolean enabled = false;
        private String baseUrl = "https://api.anthropic.com";
        private String apiKey = "";
        private String model = "claude-3-5-sonnet-20241022";
        private String version = "2023-06-01";
        private int maxTokens = 4096;
        /** HTTP read timeout for API calls (seconds). */
        private int readTimeoutSeconds = 120;
        private int connectTimeoutSeconds = 10;
    }

    @Data
    public static class Gemini {
        /** Set true to use Gemini API */
        private boolean enabled = false;
        private String baseUrl = "https://generativelanguage.googleapis.com";
        private String apiKey = "";
        private String model = "gemini-1.5-flash";
        private int maxTokens = 8192;
        /** HTTP read timeout for API calls (seconds). */
        private int readTimeoutSeconds = 120;
        private int connectTimeoutSeconds = 10;
    }
}
