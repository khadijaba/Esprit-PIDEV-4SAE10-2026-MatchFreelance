package tn.esprit.evaluation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.evaluation.dto.QuestionDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * Génération de QCM via une API compatible OpenAI (OpenAI, Azure, Ollama /v1, etc.).
 */
@Component
@Slf4j
public class ExamenLlmClient {

    /** Lots pour API cloud (JSON plus stable). */
    private static final int CHUNK_CLOUD = 4;

    /** Ollama : une question par appel = JSON valide beaucoup plus souvent avec llama3.x. */
    private static final int CHUNK_OLLAMA = 1;

    private static final String SYSTEM_LLM = String.join(" ",
            "Tu es un concepteur d'évaluations certifiantes en français.",
            "Tu réponds uniquement par un tableau JSON (liste d'objets), sans markdown, sans texte avant ou après le tableau.",
            "Chaque objet décrit une question QCM avec les clés ordre, enonce, optionA..D, bonneReponse, explication.");

    /** Validation de qualité d'une question avant publication (Ollama / OpenAI). */
    private static final String SYSTEM_QUESTION_VALIDATOR = String.join(" ",
            "Tu es un expert pédagogique et technique en français.",
            "Tu évalues une question QCM unique avant sa publication.",
            "Réponds UNIQUEMENT par un seul objet JSON (pas de markdown, pas de texte hors JSON) avec exactement ces clés :",
            "\"clairScore\" (entier 1-5, 5 = très clair),",
            "\"correctTechniqueScore\" (entier 1-5, 5 = factuellement cohérent avec les options et la bonne réponse déclarée),",
            "\"ambigu\" (booléen : vrai si l'énoncé ou les options prêtent à confusion ou plusieurs lectures),",
            "\"ambiguDetails\" (chaîne courte, vide si non ambigu),",
            "\"publishable\" (booléen : vrai seulement si la question est exploitable en examen sans révision majeure),",
            "\"summary\" (2-4 phrases de synthèse pour le formateur),",
            "\"suggestions\" (tableau de chaînes : améliorations concrètes, peut être vide).",
            "Sois exigeant sur les QCM vagues, les distracteurs non exclusifs, ou les bonnes réponses discutables.");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ExamenLlmClient(
            @Qualifier("externalRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${app.examen.llm.enabled:false}")
    private boolean enabled;

    @Value("${app.examen.llm.use-by-default:false}")
    private boolean useByDefault;

    @Value("${app.examen.llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.examen.llm.api-key:}")
    private String apiKey;

    @Value("${app.examen.llm.model:gpt-4o-mini}")
    private String model;

    @PostConstruct
    void logConfiguration() {
        log.info(
                "Examen LLM: enabled={}, useByDefault={}, availableForCalls={}, baseUrl={}, model={}, apiKeyConfigured={}",
                enabled,
                useByDefault,
                isAvailable(),
                baseUrl,
                model,
                apiKey != null && !apiKey.isBlank());
    }

    public record QuestionGenMeta(
            int ordre,
            String modTitre,
            String desc,
            String inclusion,
            String niveauDifficulte,
            String theme) {
    }

    /**
     * Ollama expose une API compatible OpenAI sur {@code .../v1} sans clé réelle ;
     * un jeton factice {@code ollama} suffit pour les clients qui envoient {@code Authorization}.
     */
    private boolean ollamaCompatibleBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        String u = baseUrl.toLowerCase(Locale.ROOT);
        return u.contains(":11434") || u.contains("ollama");
    }

    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        if (apiKey != null && !apiKey.isBlank()) {
            return true;
        }
        return ollamaCompatibleBaseUrl();
    }

    public boolean isUseByDefault() {
        return useByDefault;
    }

    /**
     * Appel LLM pour validation de question (prompt utilisateur = contenu métier construit par le service).
     *
     * @return texte assistant (JSON attendu), ou {@code null} si indisponible / erreur
     */
    public String chatCompletionValidator(String userPrompt) {
        if (!isAvailable() || userPrompt == null || userPrompt.isBlank()) {
            return null;
        }
        return chatCompletion(SYSTEM_QUESTION_VALIDATOR, userPrompt.trim(), 0.22);
    }

    /**
     * @return une question par meta, ou {@code null} si échec (appelant fera le repli heuristique)
     */
    public List<QuestionDto> generateQuestions(String titreFormation, List<QuestionGenMeta> metas) {
        if (!isAvailable() || metas == null || metas.isEmpty()) {
            return null;
        }
        String titre = titreFormation != null ? titreFormation.trim() : "";
        int chunk = ollamaCompatibleBaseUrl() ? CHUNK_OLLAMA : CHUNK_CLOUD;
        log.info("LLM generateQuestions: {} meta(s), taille de lot={}, ollama={}", metas.size(), chunk, ollamaCompatibleBaseUrl());
        List<QuestionDto> merged = new ArrayList<>();
        int maxAttempts = ollamaCompatibleBaseUrl() ? 3 : 1;
        for (int i = 0; i < metas.size(); i += chunk) {
            List<QuestionGenMeta> sub = metas.subList(i, Math.min(i + chunk, metas.size()));
            List<QuestionDto> part = null;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                if (attempt > 0) {
                    try {
                        Thread.sleep(400L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    log.warn("LLM: nouvel essai pour le lot meta {}–{} ({}/{})", i, i + sub.size() - 1, attempt + 1, maxAttempts);
                }
                part = generateChunkOnce(titre, sub);
                if (part != null && part.size() == sub.size()) {
                    break;
                }
            }
            if (part == null || part.size() != sub.size()) {
                log.error("LLM: échec définitif du lot après {} tentative(s), meta index {}–{}", maxAttempts, i, i + sub.size() - 1);
                return null;
            }
            merged.addAll(part);
        }
        return merged;
    }

    private List<QuestionDto> generateChunkOnce(String titreFormation, List<QuestionGenMeta> chunk) {
        String userPrompt = buildUserPrompt(titreFormation, chunk);
        String content = chatCompletion(SYSTEM_LLM, userPrompt, 0.42);
        if (content == null) {
            log.warn("LLM: pas de texte assistant pour un lot de {} question(s)", chunk.size());
            return null;
        }
        JsonNode arr;
        try {
            arr = parseQuestionArrayPayload(content, chunk.size());
        } catch (Exception e) {
            log.warn("LLM: JSON questions invalide: {} — extrait: {}", e.getMessage(), shorten(content, 800));
            return null;
        }
        if (arr == null || !arr.isArray() || arr.size() != chunk.size()) {
            log.warn(
                    "LLM: taille tableau finale attendue {}, obtenue {} (type={})",
                    chunk.size(),
                    arr == null ? -1 : arr.size(),
                    arr == null ? "null" : arr.getNodeType().name());
            return null;
        }
        List<QuestionDto> out = new ArrayList<>();
        for (int j = 0; j < chunk.size(); j++) {
            QuestionGenMeta meta = chunk.get(j);
            JsonNode raw = arr.get(j);
            QuestionDto q = normalizeQuestion(raw, meta);
            if (q == null) {
                log.warn("LLM: normalisation impossible ordre={} (voir log champs ci-dessus)", meta.ordre());
                return null;
            }
            out.add(q);
        }
        return out;
    }

    /**
     * Accepte ce que les petits modèles renvoient souvent : un seul objet au lieu de {@code [...]},
     * ou {@code {"questions":[...]}} , ou plus d’éléments que demandé (troncature).
     */
    private JsonNode parseQuestionArrayPayload(String assistantContent, int expectedSize) throws Exception {
        if (assistantContent == null || assistantContent.isBlank()) {
            throw new IllegalArgumentException("assistant vide");
        }
        String t = stripBom(stripMarkdownFence(assistantContent.trim()));
        JsonNode root;
        try {
            root = objectMapper.readTree(t);
        } catch (Exception first) {
            root = objectMapper.readTree(extractJsonArray(assistantContent));
        }
        JsonNode arr = unwrapToQuestionArray(root);
        if (arr == null || !arr.isArray()) {
            throw new IllegalArgumentException("pas un tableau de questions après normalisation");
        }
        if (arr.size() < expectedSize) {
            throw new IllegalArgumentException("trop peu d'éléments: " + arr.size() + " < " + expectedSize);
        }
        if (arr.size() > expectedSize) {
            log.info("LLM: le modèle a renvoyé {} question(s), conservation des {} premières", arr.size(), expectedSize);
            ArrayNode slim = objectMapper.createArrayNode();
            for (int i = 0; i < expectedSize; i++) {
                slim.add(arr.get(i));
            }
            return slim;
        }
        return arr;
    }

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static String stripMarkdownFence(String t) {
        if (!t.startsWith("```")) {
            return t;
        }
        int firstNl = t.indexOf('\n');
        if (firstNl > 0) {
            t = t.substring(firstNl + 1);
        }
        int fenceEnd = t.indexOf("```");
        if (fenceEnd >= 0) {
            t = t.substring(0, fenceEnd).trim();
        }
        return t.trim();
    }

    private JsonNode unwrapToQuestionArray(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return node;
        }
        if (!node.isObject()) {
            return null;
        }
        for (String key : List.of("questions", "items", "qcm", "data", "result", "reponses")) {
            JsonNode inner = node.get(key);
            if (inner != null && inner.isArray()) {
                return inner;
            }
        }
        if (looksLikeQuestionObject(node)) {
            ArrayNode one = objectMapper.createArrayNode();
            one.add(node);
            return one;
        }
        return null;
    }

    private static boolean looksLikeQuestionObject(JsonNode n) {
        return n.has("enonce")
                || n.has("énonce")
                || n.has("question")
                || n.has("optionA")
                || n.has("option_a");
    }

    private static String buildUserPrompt(String titreFormation, List<QuestionGenMeta> chunk) {
        StringBuilder sb = new StringBuilder();
        sb.append("Formation : ").append(titreFormation.isBlank() ? "—" : titreFormation).append("\n\n");
        sb.append("Pour chaque bloc ci-dessous, rédige UNE question QCM en français, comme dans un vrai examen professionnalisant.\n");
        sb.append("Exigences de qualité :\n");
        sb.append("- Énoncé : une question fermée sur une notion concrète tirée du descriptif (objectif, étape, outil, règle, définition), pas une méta-question du type « quelle affirmation est cohérente avec le texte ».\n");
        sb.append("- N'inclus pas de longue citation du descriptif dans l'énoncé ; reformule pour tester la compréhension.\n");
        sb.append("- Une seule bonne réponse ; options A–D courtes, mutuellement exclusives, sans doublon sémantique.\n");
        sb.append("- Distracteurs plausibles mais faux ou contredits par le descriptif du module.\n");
        sb.append("- Varie les formulations (pourquoi, comment, lequel, dans quel cas, quelle conséquence, etc.).\n");
        sb.append("- Niveau FACILE : rappel direct dans le texte. MOYEN : mise en relation. DIFFICILE : nuance ou piège raisonnable.\n");
        sb.append("- Jamais de mention de « bloc », « consigne », « descriptif fourni » ou du processus de génération.\n\n");
        int i = 1;
        for (QuestionGenMeta m : chunk) {
            String desc = m.desc() != null ? m.desc().trim() : "";
            if (desc.length() > 1800) {
                desc = desc.substring(0, 1797) + "...";
            }
            sb.append("Bloc ").append(i).append(" — ordre=").append(m.ordre())
                    .append(", module=\"").append(escapeQuotes(m.modTitre())).append("\", ")
                    .append("lot_parcours=").append(m.inclusion())
                    .append(", difficulté=").append(m.niveauDifficulte())
                    .append(", thème=\"").append(escapeQuotes(m.theme())).append("\"\n");
            if (desc.isBlank()) {
                sb.append("Contexte pédagogique : aucun descriptif n'est renseigné pour ce module. ")
                        .append("À partir du titre « ").append(escapeQuotes(m.modTitre())).append(" » ")
                        .append("et de la formation « ")
                        .append(escapeQuotes(titreFormation.isBlank() ? "—" : titreFormation))
                        .append(" », rédige UNE question QCM réaliste sur les notions enseignées ")
                        .append("habituellement sous ce thème (ex. pour le routage IP : tables de routage, masques, passerelles, protocoles). ")
                        .append("Distracteurs plausibles. Ne mentionne jamais qu'il manque un descriptif.\n\n");
            } else {
                sb.append("Descriptif module : ").append(desc).append("\n\n");
            }
            i++;
        }
        sb.append("Réponds UNIQUEMENT avec un tableau JSON (sans texte avant ou après) : ");
        sb.append("[{\"ordre\": nombre, \"enonce\": \"...\", \"optionA\": \"...\", \"optionB\": \"...\", ");
        sb.append("\"optionC\": \"...\", \"optionD\": \"...\", \"bonneReponse\": \"A\"|\"B\"|\"C\"|\"D\", ");
        sb.append("\"explication\": \"...\"}, ...] ");
        sb.append("Le tableau doit contenir exactement ").append(chunk.size());
        sb.append(" objets, dans le même ordre que les blocs, avec les mêmes valeurs « ordre » que dans les blocs.");
        return sb.toString();
    }

    private static String escapeQuotes(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\"", "'");
    }

    private void setLlmAuthHeaders(HttpHeaders headers) {
        String token = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : null;
        if (token == null && ollamaCompatibleBaseUrl()) {
            token = "ollama";
        }
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
    }

    private Map<String, Object> buildChatCompletionBody(String systemContent, String userContent, double temperature) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("stream", Boolean.FALSE);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemContent));
        messages.add(Map.of("role", "user", "content", userContent));
        body.put("messages", messages);
        return body;
    }

    /**
     * Ollama : {@link HttpClient} natif (UTF-8 explicite, pas d’intercepteur Spring) — évite les écarts avec curl.
     * Autres fournisseurs : {@link RestTemplate}.
     */
    private String chatCompletion(String systemPrompt, String userContent, double temperature) {
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        Map<String, Object> body = buildChatCompletionBody(systemPrompt, userContent, temperature);
        try {
            if (ollamaCompatibleBaseUrl()) {
                return chatCompletionOllamaHttp(url, body);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            setLlmAuthHeaders(headers);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            String rawJson = resp.getBody();
            if (rawJson == null || rawJson.isBlank()) {
                return null;
            }
            return parseAssistantContentFromResponseJson(rawJson);
        } catch (RestClientException e) {
            log.warn("LLM chat/completions (RestTemplate): {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("LLM chat/completions: {}", e.getMessage(), e);
            return null;
        }
    }

    private String chatCompletionOllamaHttp(String url, Map<String, Object> body) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(body);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(12))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        String token = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : "ollama";
        rb.header("Authorization", "Bearer " + token);
        HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            log.warn("LLM Ollama HTTP {} : {}", resp.statusCode(), shorten(resp.body(), 1500));
            return null;
        }
        String rawJson = resp.body();
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("LLM Ollama: corps de réponse vide");
            return null;
        }
        return parseAssistantContentFromResponseJson(rawJson);
    }

    private String parseAssistantContentFromResponseJson(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);
        String content = extractAssistantText(root);
        if (content.isBlank()) {
            log.warn(
                    "LLM: réponse sans texte assistant exploitable. Extrait brut: {}",
                    shorten(rawJson, 1200));
            return null;
        }
        return content;
    }

    /**
     * Compatible OpenAI / Ollama : {@code message.content} peut être une chaîne ou un tableau de segments
     * {@code [{type,text}, ...]} — {@link JsonNode#asText()} sur un tableau renvoie vide, ce qui cassait toute la génération.
     */
    private static String extractAssistantText(JsonNode root) {
        JsonNode choice0 = root.path("choices").path(0);
        JsonNode msg = choice0.path("message");
        if (msg.isMissingNode() || msg.isNull()) {
            msg = choice0.path("delta");
        }
        String fromMessage = flattenMessageContent(msg.path("content"));
        if (!fromMessage.isBlank()) {
            return fromMessage.trim();
        }
        if (choice0.has("text") && choice0.get("text").isTextual()) {
            return choice0.get("text").asText("").trim();
        }
        return "";
    }

    private static String flattenMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull() || contentNode.isMissingNode()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part == null || part.isNull()) {
                    continue;
                }
                if (part.has("text")) {
                    sb.append(part.get("text").asText(""));
                } else if (part.isTextual()) {
                    sb.append(part.asText(""));
                } else if (part.has("content")) {
                    sb.append(flattenMessageContent(part.get("content")));
                }
            }
            return sb.toString();
        }
        return contentNode.asText("");
    }

    /**
     * Extrait le premier tableau JSON top-level en respectant les chaînes (un {@code ]} dans une option ne doit pas tronquer).
     */
    private static String extractJsonArray(String text) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("empty");
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int fenceEnd = t.indexOf("```");
            if (fenceEnd >= 0) {
                t = t.substring(0, fenceEnd).trim();
            }
        }
        int start = t.indexOf('[');
        if (start < 0) {
            throw new IllegalArgumentException("no array");
        }
        int end = indexOfMatchingTopLevelArrayEnd(t, start);
        if (end < 0) {
            throw new IllegalArgumentException("unbalanced");
        }
        return t.substring(start, end + 1);
    }

    private static int indexOfMatchingTopLevelArrayEnd(String t, int openBracketIdx) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = openBracketIdx; i < t.length(); i++) {
            char c = t.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private QuestionDto normalizeQuestion(JsonNode raw, QuestionGenMeta meta) {
        if (raw == null || raw.isNull()) {
            return null;
        }
        if (raw.isTextual()) {
            try {
                raw = objectMapper.readTree(raw.asText());
            } catch (Exception e) {
                return null;
            }
        }
        if (!raw.isObject()) {
            return null;
        }
        int ordre = meta.ordre();
        int ordreLlm = raw.path("ordre").asInt(ordre);
        if (ordreLlm != ordre) {
            log.debug("LLM: ordre modèle {} corrigé en {}", ordreLlm, ordre);
        }
        String enonce = fieldText(raw, "enonce", "énonce", "question", "intitule", "titre");
        String a = fieldText(raw, "optionA", "option_a", "a", "A");
        String b = fieldText(raw, "optionB", "option_b", "b", "B");
        String c = fieldText(raw, "optionC", "option_c", "c", "C");
        String d = fieldText(raw, "optionD", "option_d", "d", "D");
        if (enonce.length() < 10 || a.length() < 4 || b.length() < 4 || c.length() < 4 || d.length() < 4) {
            log.warn(
                    "LLM: champs trop courts (ordre={} len enonce={} A={} B={} C={} D={})",
                    ordre,
                    enonce.length(),
                    a.length(),
                    b.length(),
                    c.length(),
                    d.length());
            return null;
        }
        String brRaw = fieldText(raw, "bonneReponse", "bonne_reponse", "reponseCorrecte", "answer", "correct", "reponse");
        String br = normalizeBonneReponse(brRaw);
        String expl = fieldText(raw, "explication", "justification", "feedback");
        return QuestionDto.builder()
                .ordre(ordre)
                .enonce(shorten(enonce, 1000))
                .optionA(shorten(a, 500))
                .optionB(shorten(b, 500))
                .optionC(shorten(c, 500))
                .optionD(shorten(d, 500))
                .bonneReponse(br)
                .parcoursInclusion(meta.inclusion())
                .niveauDifficulte(meta.niveauDifficulte())
                .theme(meta.theme())
                .skill(meta.theme())
                .explication(shorten(expl, 2000))
                .build();
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return nodeToQuizString(v);
    }

    private static String nodeToQuizString(JsonNode v) {
        if (v == null || v.isNull() || v.isMissingNode()) {
            return "";
        }
        if (v.isTextual()) {
            return v.asText("").trim();
        }
        if (v.isNumber() || v.isBoolean()) {
            return v.asText().trim();
        }
        return "";
    }

    /** Clés exactes puis correspondance sans tenir compte de la casse (OptionA, optiona…). */
    private static String fieldText(JsonNode obj, String... preferredKeys) {
        if (obj == null || !obj.isObject()) {
            return "";
        }
        for (String k : preferredKeys) {
            if (obj.has(k)) {
                String s = nodeToQuizString(obj.get(k));
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        for (Iterator<String> it = obj.fieldNames(); it.hasNext(); ) {
            String fn = it.next();
            for (String k : preferredKeys) {
                if (fn.equalsIgnoreCase(k)) {
                    String s = nodeToQuizString(obj.get(fn));
                    if (!s.isEmpty()) {
                        return s;
                    }
                }
            }
        }
        return "";
    }

    private static String normalizeBonneReponse(String brRaw) {
        if (brRaw == null || brRaw.isBlank()) {
            return "A";
        }
        String u = brRaw.toUpperCase().trim();
        for (int i = 0; i < u.length(); i++) {
            char c = u.charAt(i);
            if (c >= 'A' && c <= 'D') {
                return String.valueOf(c);
            }
        }
        return "A";
    }

    private static String shorten(String s, int max) {
        if (s == null || s.length() <= max) {
            return s != null ? s : "";
        }
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
}
