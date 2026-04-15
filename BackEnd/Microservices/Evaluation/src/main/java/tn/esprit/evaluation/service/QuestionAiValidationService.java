package tn.esprit.evaluation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.evaluation.client.ExamenLlmClient;
import tn.esprit.evaluation.dto.QuestionValidationRequest;
import tn.esprit.evaluation.dto.QuestionValidationResultDto;
import tn.esprit.evaluation.util.QcmReponseNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation de qualité d'une question QCM via le LLM configuré (ex. Ollama local).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionAiValidationService {

    private final ExamenLlmClient examenLlmClient;
    private final ObjectMapper objectMapper;

    public QuestionValidationResultDto validate(QuestionValidationRequest req) {
        if (!examenLlmClient.isAvailable()) {
            return QuestionValidationResultDto.builder()
                    .llmConfigured(false)
                    .parseOk(false)
                    .errorMessage(
                            "LLM désactivé ou non configuré. Activez app.examen.llm.enabled et renseignez base-url / modèle (ex. Ollama).")
                    .suggestions(List.of())
                    .build();
        }
        if (req == null || req.getEnonce() == null || req.getEnonce().isBlank()) {
            return QuestionValidationResultDto.builder()
                    .llmConfigured(true)
                    .parseOk(false)
                    .errorMessage("Énoncé vide : rien à valider.")
                    .suggestions(List.of())
                    .build();
        }

        String user = buildUserPrompt(req);
        String raw;
        try {
            raw = examenLlmClient.chatCompletionValidator(user);
        } catch (Exception e) {
            log.warn("Question validator LLM: {}", e.getMessage());
            return QuestionValidationResultDto.builder()
                    .llmConfigured(true)
                    .parseOk(false)
                    .errorMessage("Erreur d'appel au LLM : " + e.getMessage())
                    .suggestions(List.of())
                    .build();
        }
        if (raw == null || raw.isBlank()) {
            return QuestionValidationResultDto.builder()
                    .llmConfigured(true)
                    .parseOk(false)
                    .errorMessage("Réponse LLM vide ou invalide.")
                    .suggestions(List.of())
                    .build();
        }

        try {
            String json = extractFirstJsonObject(raw);
            if (json == null) {
                return failParse(raw, "Aucun objet JSON détecté dans la réponse.");
            }
            JsonNode n = objectMapper.readTree(json);
            List<String> sugg = new ArrayList<>();
            if (n.has("suggestions") && n.get("suggestions").isArray()) {
                for (JsonNode s : n.get("suggestions")) {
                    if (s.isTextual() && !s.asText().isBlank()) {
                        sugg.add(s.asText().trim());
                    }
                }
            }
            return QuestionValidationResultDto.builder()
                    .llmConfigured(true)
                    .parseOk(true)
                    .clairScore(clampScore(n.get("clairScore")))
                    .correctTechniqueScore(clampScore(n.get("correctTechniqueScore")))
                    .ambigu(n.has("ambigu") && n.get("ambigu").isBoolean() ? n.get("ambigu").asBoolean() : null)
                    .ambiguDetails(textOrNull(n.get("ambiguDetails")))
                    .publishable(n.has("publishable") && n.get("publishable").isBoolean()
                            ? n.get("publishable").asBoolean()
                            : null)
                    .summary(textOrNull(n.get("summary")))
                    .suggestions(sugg)
                    .build();
        } catch (Exception e) {
            log.warn("Question validator parse: {} — extrait: {}", e.getMessage(), shorten(raw, 500));
            return failParse(raw, "JSON invalide : " + e.getMessage());
        }
    }

    private static QuestionValidationResultDto failParse(String raw, String msg) {
        return QuestionValidationResultDto.builder()
                .llmConfigured(true)
                .parseOk(false)
                .errorMessage(msg)
                .suggestions(List.of())
                .build();
    }

    private static Integer clampScore(JsonNode n) {
        if (n == null || !n.isNumber()) {
            return null;
        }
        int v = n.asInt();
        if (v < 1) {
            return 1;
        }
        if (v > 5) {
            return 5;
        }
        return v;
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isNull() || !n.isTextual()) {
            return null;
        }
        String t = n.asText("").trim();
        return t.isEmpty() ? null : t;
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String buildUserPrompt(QuestionValidationRequest r) {
        String br = QcmReponseNormalizer.extraireLettreBonneReponse(r.getBonneReponse());
        if (br.isEmpty()) {
            br = "?";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse cette question QCM avant publication.\n\n");
        if (r.getContexteFormation() != null && !r.getContexteFormation().isBlank()) {
            String ctx = r.getContexteFormation().trim();
            if (ctx.length() > 4000) {
                ctx = ctx.substring(0, 3997) + "...";
            }
            sb.append("Contexte formation / cours (peut aider à juger la justesse) :\n")
                    .append(ctx)
                    .append("\n\n");
        }
        sb.append("Énoncé :\n").append(r.getEnonce().trim()).append("\n\n");
        sb.append("A) ").append(nullToDash(r.getOptionA())).append("\n");
        sb.append("B) ").append(nullToDash(r.getOptionB())).append("\n");
        sb.append("C) ").append(nullToDash(r.getOptionC())).append("\n");
        sb.append("D) ").append(nullToDash(r.getOptionD())).append("\n\n");
        sb.append("Bonne réponse déclarée par le formateur : ").append(br).append("\n");
        if (r.getSkill() != null && !r.getSkill().isBlank()) {
            sb.append("Compétence ciblée : ").append(r.getSkill().trim()).append("\n");
        }
        if (r.getTheme() != null && !r.getTheme().isBlank()) {
            sb.append("Thème pédagogique : ").append(r.getTheme().trim()).append("\n");
        }
        sb.append("\nRéponds uniquement avec l'objet JSON demandé dans ta consigne système.");
        return sb.toString();
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    /**
     * Extrait le premier objet JSON équilibré (chaînes et échappements pris en compte).
     */
    static String extractFirstJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int fence = t.indexOf("```");
            if (fence >= 0) {
                t = t.substring(0, fence).trim();
            }
        }
        int start = t.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inStr = false;
        boolean esc = false;
        for (int i = start; i < t.length(); i++) {
            char c = t.charAt(i);
            if (esc) {
                esc = false;
                continue;
            }
            if (inStr) {
                if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return t.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
