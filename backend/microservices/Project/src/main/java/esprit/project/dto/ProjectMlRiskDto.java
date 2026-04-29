package esprit.project.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Réponse du score de risque projet via modèle ML (ONNX) ou repli heuristique. */
@Value
@Builder
public class ProjectMlRiskDto {
    long projectId;
    /** 0 = faible risque perçu, 100 = risque élevé. */
    int riskScore0To100;
    /** LOW / MEDIUM / HIGH */
    String riskLevel;
    /** Probabilité classe « risque élevé » (sortie calibrée du modèle). */
    float probabilityHighRisk;
    List<String> flags;
    String summary;
    String modelId;
    /** true si le fichier ONNX est absent ou l’inférence a échoué. */
    boolean heuristicFallback;
}
