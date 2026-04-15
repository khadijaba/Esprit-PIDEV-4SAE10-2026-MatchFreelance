package tn.esprit.evaluation.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.evaluation.dto.ExamenDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Appelle le service Python (evaluation-reports-py) pour construire le squelette d'examen.
 * URL absolue recommandée (ex. http://localhost:8090/api/evaluation-reports/exam/auto-generate).
 */
@Component
@Slf4j
public class ExamenPythonGeneratorClient {

    private final RestTemplate restTemplate;

    @Value("${app.examen.python-generator-url:}")
    private String pythonGeneratorUrl;

    public ExamenPythonGeneratorClient(@Qualifier("externalRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return pythonGeneratorUrl != null && !pythonGeneratorUrl.isBlank();
    }

    /**
     * @return corps d'examen prêt pour {@code examenService.create}, ou {@code null} si échec / non configuré
     */
    public ExamenDto generate(
            Long formationId,
            String titreFormation,
            List<Map<String, Object>> modules,
            String suffixeTitre,
            Integer seuilReussi,
            Boolean useLlm) {
        if (!isConfigured()) {
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("formationId", formationId);
        body.put("titreFormation", titreFormation != null ? titreFormation : "");
        body.put("modules", modules);
        if (suffixeTitre != null && !suffixeTitre.isBlank()) {
            body.put("suffixeTitre", suffixeTitre);
        }
        body.put("seuilReussi", seuilReussi != null ? seuilReussi : 60);
        if (useLlm != null) {
            body.put("useLlm", useLlm);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ExamenDto dto = restTemplate.postForObject(pythonGeneratorUrl.trim(), entity, ExamenDto.class);
            if (dto != null) {
                dto.setFormationId(formationId);
            }
            return dto;
        } catch (RestClientException e) {
            log.warn("Génération examen Python indisponible ({}): {}", pythonGeneratorUrl, e.getMessage());
            return null;
        }
    }
}
