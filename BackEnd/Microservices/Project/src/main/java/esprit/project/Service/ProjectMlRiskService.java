package esprit.project.Service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import esprit.project.Repositories.ProjectRepository;
import esprit.project.dto.ProjectMlRiskDto;
import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import esprit.project.ml.ProjectRiskFeatureVector;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProjectMlRiskService {

    public static final String MODEL_ID = "onnx-rf-risk-v1";
    private static final String ONNX_RESOURCE = "ml/project-risk.onnx";
    private static final String INPUT_NAME = "float_input";

    private final ProjectRepository projectRepository;

    private OrtEnvironment ortEnv;
    private OrtSession ortSession;
    private volatile boolean onnxReady;

    @PostConstruct
    public void init() {
        onnxReady = false;
        ortSession = null;
        ortEnv = null;
        try {
            ClassPathResource res = new ClassPathResource(ONNX_RESOURCE);
            if (!res.exists()) {
                return;
            }
            byte[] bytes;
            try (InputStream in = res.getInputStream()) {
                bytes = in.readAllBytes();
            }
            ortEnv = OrtEnvironment.getEnvironment();
            ortSession = ortEnv.createSession(bytes, new OrtSession.SessionOptions());
            onnxReady = true;
        } catch (Exception e) {
            onnxReady = false;
            closeQuietly(ortSession);
            ortSession = null;
            if (ortEnv != null) {
                try {
                    ortEnv.close();
                } catch (Exception ignored) {
                    /* ignore */
                }
                ortEnv = null;
            }
        }
    }

    @PreDestroy
    public void shutdown() throws OrtException {
        if (ortSession != null) {
            ortSession.close();
        }
        if (ortEnv != null) {
            ortEnv.close();
        }
    }

    @Transactional(readOnly = true)
    public ProjectMlRiskDto evaluate(Long projectId) {
        Project p = projectRepository.findById(projectId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
        Long ownerId = p.getProjectOwnerId();
        long ownerTotal = ownerId != null ? projectRepository.countByProjectOwnerId(ownerId) : 0;
        long ownerCompleted = ownerId != null ? projectRepository.countByProjectOwnerIdAndStatus(ownerId, ProjectStatus.COMPLETED) : 0;

        float[] features = ProjectRiskFeatureVector.build(p, ownerCompleted, ownerTotal);
        List<String> flags = ruleFlags(p, features);

        boolean usedHeuristic = !onnxReady || ortSession == null || ortEnv == null;
        float probHigh = heuristicProbabilityHigh(flags, p.getStatus());

        if (!usedHeuristic) {
            try {
                probHigh = runOnnxProbHighRisk(features);
            } catch (Exception e) {
                usedHeuristic = true;
                probHigh = heuristicProbabilityHigh(flags, p.getStatus());
            }
        }

        int score = Math.max(0, Math.min(100, Math.round(probHigh * 100)));
        String level = level(score);
        String summary = String.format(
                "Probabilité de risque élevé ~ %.0f %% (%s). %d signal(aux) métier. %s",
                probHigh * 100,
                level,
                flags.size(),
                usedHeuristic ? "Repli heuristique (ONNX absent ou erreur d’inférence)." : "Inférence ONNX."
        );

        return ProjectMlRiskDto.builder()
                .projectId(projectId)
                .riskScore0To100(score)
                .riskLevel(level)
                .probabilityHighRisk(probHigh)
                .flags(List.copyOf(flags))
                .summary(summary)
                .modelId(usedHeuristic ? "heuristic-fallback" : MODEL_ID)
                .heuristicFallback(usedHeuristic)
                .build();
    }

    private float runOnnxProbHighRisk(float[] features) throws OrtException {
        try (OnnxTensor tensor = OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(features),
                new long[]{1, ProjectRiskFeatureVector.DIMENSION})) {
            try (OrtSession.Result result = ortSession.run(Collections.singletonMap(INPUT_NAME, tensor))) {
                return extractHighRiskProbability(result);
            }
        }
    }

    private static float extractHighRiskProbability(OrtSession.Result result) throws OrtException {
        for (int i = 0; i < result.size(); i++) {
            OnnxValue v = result.get(i);
            if (!(v instanceof OnnxTensor t)) {
                continue;
            }
            Object raw = t.getValue();
            if (raw instanceof float[][] arr && arr.length > 0 && arr[0].length > 1) {
                return arr[0][1];
            }
            if (raw instanceof double[][] darr && darr.length > 0 && darr[0].length > 1) {
                return (float) darr[0][1];
            }
        }
        java.util.Optional<OnnxValue> named = result.get("probabilities");
        if (named.isPresent()) {
            return extractFromTensor(named.get());
        }
        named = result.get("output_probability");
        if (named.isPresent()) {
            return extractFromTensor(named.get());
        }
        throw new OrtException("No [N,2] probability output found");
    }

    private static float extractFromTensor(OnnxValue v) throws OrtException {
        Object raw = ((OnnxTensor) v).getValue();
        if (raw instanceof float[][] arr && arr.length > 0 && arr[0].length > 1) {
            return arr[0][1];
        }
        if (raw instanceof double[][] darr && darr.length > 0 && darr[0].length > 1) {
            return (float) darr[0][1];
        }
        throw new OrtException("Unexpected probability tensor shape");
    }

    private static String level(int score) {
        if (score < 34) {
            return "LOW";
        }
        if (score < 67) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private static List<String> ruleFlags(Project p, float[] f) {
        List<String> flags = new ArrayList<>();
        int titleLen = Optional.ofNullable(p.getTitle()).map(String::length).orElse(0);
        int words = (int) Math.round(f[1] * 200);
        int skills = (int) Math.round(f[2] * 10);
        double daily = f[3] * 2.0 * 200.0;

        if (titleLen < 12) {
            flags.add("TITRE_TROP_COURT");
        }
        if (words < 35) {
            flags.add("DESCRIPTION_INSUFFISANTE");
        }
        if (skills == 0) {
            flags.add("AUCUNE_COMPETENCE_REQUISE");
        }
        if (skills > 8) {
            flags.add("PROFIL_ELARGI_DIFFICILE_A_REMPLIR");
        }
        if (daily > 0 && daily < 12) {
            flags.add("TAUX_JOURNALIER_FAIBLE");
        }
        if (daily > 280) {
            flags.add("TAUX_JOURNALIER_ELEVE");
        }
        if (p.getStatus() == ProjectStatus.CANCELLED) {
            flags.add("PROJET_ANNULE");
        }
        return flags;
    }

    private static float heuristicProbabilityHigh(List<String> flags, ProjectStatus status) {
        float p = 0.12f + flags.size() * 0.11f;
        if (status == ProjectStatus.CANCELLED) {
            p += 0.25f;
        }
        return Math.max(0.05f, Math.min(0.95f, p));
    }

    private static void closeQuietly(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception ignored) {
            /* ignore */
        }
    }
}
