package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.AiDecomposeRequestDTO;
import com.freelancing.productivity.dto.AiDecomposeResponseDTO;
import com.freelancing.productivity.dto.ContextSuggestionDTO;
import com.freelancing.productivity.entity.ProductivityTask;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CognitiveAssistService {

    private static final Logger log = LoggerFactory.getLogger(CognitiveAssistService.class);

    private final TaskService taskService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${productivity.ai.ollama.enabled:false}")
    private boolean ollamaEnabled;

    @Value("${productivity.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${productivity.ai.ollama.model:llama3.2}")
    private String ollamaModel;

    @Value("${productivity.ai.ollama.temperature:0.2}")
    private double ollamaTemperature;

    @Value("${productivity.ai.ollama.fallback-enabled:true}")
    private boolean ollamaFallbackEnabled;

    @Transactional(readOnly = true)
    public AiDecomposeResponseDTO decomposeGoal(AiDecomposeRequestDTO request) {
        if (ollamaEnabled) {
            try {
                return ollamaDecomposeGoal(request);
            } catch (Exception ex) {
                log.warn("Ollama decompose failed, fallback enabled={}", ollamaFallbackEnabled, ex);
                if (!ollamaFallbackEnabled) {
                    throw new IllegalStateException("Ollama decomposition failed and fallback is disabled");
                }
            }
        }
        return heuristicDecomposeGoal(request, ollamaEnabled);
    }

    private AiDecomposeResponseDTO heuristicDecomposeGoal(AiDecomposeRequestDTO request, boolean fallbackMode) {
        int maxSteps = request.getMaxSteps() == null ? 5 : request.getMaxSteps();
        String goal = request.getGoalText().trim();

        List<String> steps = new ArrayList<>();
        steps.add("Define success criteria and measurable outcome for: " + goal);
        steps.add("Break the goal into deliverables and estimate each deliverable.");
        steps.add("Create first executable task and schedule it within 24 hours.");
        steps.add("Identify blockers/risks and assign mitigation actions.");
        steps.add("Run a short review checkpoint and adjust the plan.");

        if (goal.toLowerCase(Locale.ROOT).contains("portfolio") || goal.toLowerCase(Locale.ROOT).contains("project")) {
            steps.add("Prepare demo artifacts and final acceptance checklist.");
        }

        if (steps.size() > maxSteps) {
            steps = steps.subList(0, maxSteps);
        }

        AiDecomposeResponseDTO dto = new AiDecomposeResponseDTO();
        dto.setInputGoal(goal);
        dto.setSuggestedSteps(steps);
        dto.setRationale((fallbackMode ? "Heuristic fallback: " : "") +
                "Heuristic decomposition generated from a goal-to-action template with risk and review checkpoints.");
        dto.setAiSource(fallbackMode ? "HEURISTIC_FALLBACK" : "HEURISTIC");
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ContextSuggestionDTO> contextSuggestions(Long ownerId) {
        List<ProductivityTask> tasks = taskService.findAllTasks(ownerId);
        List<ProductivityTask> completed = tasks.stream().filter(t -> t.getStatus() == ProductivityTaskStatus.DONE && t.getCompletedAt() != null).toList();

        Map<DayOfWeek, Integer> doneByDay = new EnumMap<>(DayOfWeek.class);
        for (ProductivityTask task : completed) {
            DayOfWeek day = task.getCompletedAt().atZone(ZoneId.systemDefault()).getDayOfWeek();
            doneByDay.put(day, doneByDay.getOrDefault(day, 0) + 1);
        }

        if (ollamaEnabled) {
            try {
                return ollamaContextSuggestions(doneByDay);
            } catch (Exception ex) {
                log.warn("Ollama context suggestions failed, fallback enabled={}", ollamaFallbackEnabled, ex);
                if (!ollamaFallbackEnabled) {
                    throw new IllegalStateException("Ollama context suggestions failed and fallback is disabled");
                }
            }
        }

        return heuristicContextSuggestions(doneByDay, ollamaEnabled);
    }

    private List<ContextSuggestionDTO> heuristicContextSuggestions(Map<DayOfWeek, Integer> doneByDay, boolean fallbackMode) {

        List<ContextSuggestionDTO> suggestions = new ArrayList<>();
        if (!doneByDay.isEmpty()) {
            DayOfWeek best = doneByDay.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(DayOfWeek.WEDNESDAY);
            DayOfWeek worst = doneByDay.entrySet().stream().min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(DayOfWeek.MONDAY);

            ContextSuggestionDTO tip1 = new ContextSuggestionDTO();
            tip1.setCategory("Energy pattern");
            tip1.setMessage("You complete more tasks on " + best + ". Schedule deep work there.");
            tip1.setConfidence(0.74);
            suggestions.add(tip1);

            ContextSuggestionDTO tip2 = new ContextSuggestionDTO();
            tip2.setCategory("Risk pattern");
            tip2.setMessage("Completion tends to dip on " + worst + ". Keep that day for lighter tasks and admin work.");
            tip2.setConfidence(0.68);
            suggestions.add(tip2);
        } else {
            ContextSuggestionDTO baseline = new ContextSuggestionDTO();
            baseline.setCategory("Startup guidance");
            baseline.setMessage((fallbackMode ? "(Fallback) " : "") +
                    "Complete 5 tasks across different weekdays to unlock personalized context patterns.");
            baseline.setConfidence(0.55);
            suggestions.add(baseline);
        }

        return suggestions;
    }

    private AiDecomposeResponseDTO ollamaDecomposeGoal(AiDecomposeRequestDTO request) throws Exception {
        int maxSteps = request.getMaxSteps() == null ? 5 : request.getMaxSteps();
        String goal = request.getGoalText().trim();
        String prompt = "You are a productivity planning assistant. " +
                "Return strict JSON object only with keys rationale (string) and steps (array of strings). " +
                "No markdown, no explanation outside JSON. " +
                "Goal: \"" + goal + "\". " +
                "Max steps: " + maxSteps + ". Keep steps actionable and concise.";

        String raw = generateWithOllama(prompt);
        JsonNode parsed = objectMapper.readTree(extractFirstJsonObject(raw));

        JsonNode stepsNode = parsed.path("steps");
        if (!stepsNode.isArray() || stepsNode.isEmpty()) {
            throw new IllegalStateException("Ollama returned invalid steps payload");
        }

        List<String> steps = new ArrayList<>();
        for (JsonNode node : stepsNode) {
            if (node.isTextual()) {
                String step = node.asText().trim();
                if (!step.isEmpty()) {
                    steps.add(step);
                }
            }
            if (steps.size() >= maxSteps) {
                break;
            }
        }

        if (steps.isEmpty()) {
            throw new IllegalStateException("Ollama returned empty steps list");
        }

        AiDecomposeResponseDTO dto = new AiDecomposeResponseDTO();
        dto.setInputGoal(goal);
        dto.setSuggestedSteps(steps);
        String rationale = parsed.path("rationale").asText("Generated by Ollama model " + ollamaModel + ".");
        dto.setRationale(rationale);
        dto.setAiSource("OLLAMA");
        return dto;
    }

    private List<ContextSuggestionDTO> ollamaContextSuggestions(Map<DayOfWeek, Integer> doneByDay) throws Exception {
        String signal = doneByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ":" + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("no completion history");

        String prompt = "You are a productivity analyst. " +
                "Given completion counts by weekday, return strict JSON object only with key suggestions. " +
                "suggestions must be an array of 1 to 3 objects with keys category, message, confidence (0..1). " +
                "No markdown. Input: " + signal;

        String raw = generateWithOllama(prompt);
        JsonNode parsed = objectMapper.readTree(extractFirstJsonObject(raw));
        JsonNode suggestionsNode = parsed.path("suggestions");
        if (!suggestionsNode.isArray() || suggestionsNode.isEmpty()) {
            throw new IllegalStateException("Ollama returned invalid suggestions payload");
        }

        List<ContextSuggestionDTO> suggestions = new ArrayList<>();
        for (JsonNode node : suggestionsNode) {
            String category = node.path("category").asText("").trim();
            String message = node.path("message").asText("").trim();
            double confidence = node.path("confidence").isNumber() ? node.path("confidence").asDouble() : 0.6;
            if (message.isEmpty()) {
                continue;
            }
            ContextSuggestionDTO dto = new ContextSuggestionDTO();
            dto.setCategory(category.isEmpty() ? "Ollama insight" : category);
            dto.setMessage(message);
            dto.setConfidence(Math.max(0.0, Math.min(1.0, confidence)));
            suggestions.add(dto);
        }

        if (suggestions.isEmpty()) {
            throw new IllegalStateException("Ollama suggestions were empty after normalization");
        }
        return suggestions;
    }

    private String generateWithOllama(String prompt) {
        String endpoint = ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl + "api/generate" : ollamaBaseUrl + "/api/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", ollamaTemperature);

        Map<String, Object> body = new HashMap<>();
        body.put("model", ollamaModel);
        body.put("prompt", prompt);
        body.put("stream", false);
        body.put("options", options);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Ollama returned non-success response");
            }
            Object text = response.getBody().get("response");
            if (text == null) {
                throw new IllegalStateException("Ollama response field is missing");
            }
            return String.valueOf(text);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to call Ollama endpoint: " + endpoint, ex);
        }
    }

    private String extractFirstJsonObject(String value) {
        String trimmed = value == null ? "" : value.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("No JSON object found in Ollama response");
        }
        return trimmed.substring(start, end + 1);
    }
}

