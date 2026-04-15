package tn.esprit.evaluation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.evaluation.domain.TypeParcours;
import tn.esprit.evaluation.dto.AjouterQuestionsModeleRequest;
import tn.esprit.evaluation.dto.AdaptatifDemarrageDto;
import tn.esprit.evaluation.dto.AdaptatifEtapeReponseDto;
import tn.esprit.evaluation.dto.AdaptatifRepondreRequest;
import tn.esprit.evaluation.dto.AmenagementTempsDto;
import tn.esprit.evaluation.dto.AmenagementTempsUpsertRequest;
import tn.esprit.evaluation.dto.AutoGenerateExamenRequest;
import tn.esprit.evaluation.dto.DemarrerAdaptatifRequest;
import tn.esprit.evaluation.dto.ExamenDto;
import tn.esprit.evaluation.dto.ObjectifThemeDto;
import tn.esprit.evaluation.dto.ObjectifThemeRequest;
import tn.esprit.evaluation.dto.PassageExamenDto;
import tn.esprit.evaluation.dto.QuestionValidationRequest;
import tn.esprit.evaluation.dto.QuestionValidationResultDto;
import tn.esprit.evaluation.dto.RemediationPlanDto;
import tn.esprit.evaluation.dto.ReponseExamenRequest;
import tn.esprit.evaluation.dto.RiskEvaluationDto;
import tn.esprit.evaluation.dto.SuccessPredictionDto;
import tn.esprit.evaluation.service.ExamenAdaptatifService;
import tn.esprit.evaluation.service.ExamenGenerationService;
import tn.esprit.evaluation.service.ExamenService;
import tn.esprit.evaluation.service.FreelancerAmenagementTempsService;
import tn.esprit.evaluation.service.FreelancerObjectifThemeService;
import tn.esprit.evaluation.service.ParcoursApprenantService;
import tn.esprit.evaluation.service.PassageExamenService;
import tn.esprit.evaluation.service.QuestionAiValidationService;

import java.util.List;
import java.util.Map;

/**
 * API Examens et passages - Consommable via Gateway.
 */
@RestController
@RequestMapping("/api/examens")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExamenController {

    private final ExamenService examenService;
    private final PassageExamenService passageExamenService;
    private final ParcoursApprenantService parcoursApprenantService;
    private final ExamenGenerationService examenGenerationService;
    private final ExamenAdaptatifService examenAdaptatifService;
    private final FreelancerAmenagementTempsService freelancerAmenagementTempsService;
    private final FreelancerObjectifThemeService freelancerObjectifThemeService;
    private final QuestionAiValidationService questionAiValidationService;

    /**
     * Validation IA (Ollama / OpenAI) d'une question QCM avant publication : clarté, justesse, ambiguïté.
     * Deux chemins équivalents : {@code /validate-question-ai} (recommandé) et {@code /questions/validate-ai}.
     */
    @PostMapping({"/validate-question-ai", "/questions/validate-ai"})
    public ResponseEntity<QuestionValidationResultDto> validateQuestionAi(
            @RequestBody(required = false) QuestionValidationRequest body) {
        if (body == null) {
            body = new QuestionValidationRequest();
        }
        return ResponseEntity.ok(questionAiValidationService.validate(body));
    }

    /**
     * Contrôle déploiement : en GET, sans corps. Si 404 ici, l'instance sur ce port n'exécute pas ce code (ancien JAR ou autre appli).
     */
    @GetMapping("/validate-question-ai/ping")
    public ResponseEntity<Map<String, String>> validateQuestionAiPing() {
        return ResponseEntity.ok(Map.of("validateQuestionAi", "deployed"));
    }

    // ---------- CRUD Examens ----------
    @GetMapping
    public ResponseEntity<List<ExamenDto>> getAll() {
        return ResponseEntity.ok(examenService.findAll());
    }

    @GetMapping("/formation/{formationId:\\d+}")
    public ResponseEntity<List<ExamenDto>> getByFormation(@PathVariable Long formationId) {
        return ResponseEntity.ok(examenService.findByFormationId(formationId));
    }

    /**
     * Génération auto sans ID dans l’URL : la formation est indiquée dans le corps.
     * <p>Deux chemins équivalents : {@code /auto-generate} et {@code /generation/auto-generate}
     * (le second évite tout conflit de routage si un environnement renvoie encore 405 sur le premier).
     */
    @PostMapping({"/auto-generate", "/generation/auto-generate"})
    public ResponseEntity<ExamenDto> genererAutoDepuisCorps(@RequestBody(required = false) AutoGenerateExamenRequest body) {
        if (body == null || body.getFormationId() == null) {
            throw new IllegalArgumentException("formationId est requis dans le corps de la requête.");
        }
        ExamenDto out = examenGenerationService.genererDepuisFormation(body.getFormationId(), body);
        boolean preview = Boolean.TRUE.equals(body.getPreview());
        return ResponseEntity.status(preview ? HttpStatus.OK : HttpStatus.CREATED).body(out);
    }

    /**
     * Génération auto : chemin prioritaire (évite tout conflit avec /{id} côté proxy ou anciennes versions).
     * POST /api/examens/formation/{formationId}/auto-generate
     */
    @PostMapping("/formation/{formationId:\\d+}/auto-generate")
    public ResponseEntity<ExamenDto> genererDepuisFormationParFormation(
            @PathVariable Long formationId,
            @RequestBody(required = false) AutoGenerateExamenRequest body) {
        AutoGenerateExamenRequest clean = sanitizeBodyForPath(body);
        ExamenDto out = examenGenerationService.genererDepuisFormation(formationId, clean);
        boolean preview = Boolean.TRUE.equals(clean.getPreview());
        return ResponseEntity.status(preview ? HttpStatus.OK : HttpStatus.CREATED).body(out);
    }

    /** {@code \\d+} : évite que des segments littéraux (ex. {@code auto-generate}) matchent {@code /{id}} et provoquent un 405 sur POST. */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ExamenDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(examenService.findById(id));
    }

    @GetMapping("/{id:\\d+}/passage")
    public ResponseEntity<ExamenDto> getPourPassage(
            @PathVariable Long id,
            @RequestParam(value = "parcours", required = false, defaultValue = "STANDARD") String parcours) {
        TypeParcours tp = TypeParcours.valueOf(parcours.trim().toUpperCase());
        return ResponseEntity.ok(examenService.getExamenPourPassage(id, tp));
    }

    /**
     * Révision ciblée : uniquement les questions déjà ratées au moins une fois par ce freelancer (mode entraînement).
     * GET /api/examens/examen/{examenId}/revision?freelancerId=&parcours=STANDARD
     */
    @GetMapping("/examen/{examenId}/revision")
    public ResponseEntity<ExamenDto> getPourRevision(
            @PathVariable Long examenId,
            @RequestParam Long freelancerId,
            @RequestParam(value = "parcours", required = false, defaultValue = "STANDARD") String parcours) {
        TypeParcours tp = TypeParcours.valueOf(parcours.trim().toUpperCase());
        return ResponseEntity.ok(examenService.getExamenPourRevision(examenId, tp, freelancerId));
    }

    @GetMapping("/freelancer/{freelancerId}/amenagement-temps")
    public ResponseEntity<AmenagementTempsDto> getAmenagementTemps(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(freelancerAmenagementTempsService.getPourFreelancer(freelancerId));
    }

    @PostMapping("/freelancer/{freelancerId}/amenagement-temps")
    public ResponseEntity<AmenagementTempsDto> postAmenagementTemps(
            @PathVariable Long freelancerId,
            @RequestBody(required = false) AmenagementTempsUpsertRequest body) {
        return ResponseEntity.ok(freelancerAmenagementTempsService.enregistrer(freelancerId, body));
    }

    @GetMapping("/freelancer/{freelancerId}/objectifs-theme")
    public ResponseEntity<List<ObjectifThemeDto>> listObjectifsTheme(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(freelancerObjectifThemeService.listerPourFreelancer(freelancerId));
    }

    @PostMapping("/freelancer/{freelancerId}/objectifs-theme")
    public ResponseEntity<ObjectifThemeDto> creerObjectifTheme(
            @PathVariable Long freelancerId,
            @Valid @RequestBody ObjectifThemeRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(freelancerObjectifThemeService.creer(freelancerId, body));
    }

    @DeleteMapping("/freelancer/{freelancerId}/objectifs-theme/{objectifId}")
    public ResponseEntity<Void> supprimerObjectifTheme(
            @PathVariable Long freelancerId,
            @PathVariable Long objectifId) {
        freelancerObjectifThemeService.supprimer(freelancerId, objectifId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Prédiction de risque d'échec (règles + historique Formation / Skill) et parcours suggéré.
     */
    @GetMapping("/{examenId:\\d+}/parcours/risque/freelancer/{freelancerId}")
    public ResponseEntity<RiskEvaluationDto> getEvaluationRisque(
            @PathVariable Long examenId,
            @PathVariable Long freelancerId) {
        return ResponseEntity.ok(parcoursApprenantService.evaluerRisque(freelancerId, examenId));
    }

    /**
     * Simulateur avant passage : probabilité de réussite selon historique, compétences et tempo de préparation.
     */
    @GetMapping("/{examenId:\\d+}/freelancer/{freelancerId}/simulateur-reussite")
    public ResponseEntity<SuccessPredictionDto> simulerReussiteAvantPassage(
            @PathVariable Long examenId,
            @PathVariable Long freelancerId) {
        return ResponseEntity.ok(parcoursApprenantService.simulerReussiteAvantPassage(freelancerId, examenId));
    }

    /**
     * Plan de remédiation personnalisé (learning path daté/séquencé) après échec ou risque élevé.
     */
    @GetMapping("/{examenId:\\d+}/freelancer/{freelancerId}/plan-remediation")
    public ResponseEntity<RemediationPlanDto> getPlanRemediation(
            @PathVariable Long examenId,
            @PathVariable Long freelancerId) {
        return ResponseEntity.ok(parcoursApprenantService.construirePlanRemediation(freelancerId, examenId));
    }

    @PostMapping
    public ResponseEntity<ExamenDto> create(@Valid @RequestBody ExamenDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examenService.create(dto));
    }

    /**
     * Génère un examen QCM à partir des modules de la formation (appel au microservice Formation).
     * Les questions sont des modèles à faire valider par le formateur.
     */
    @PostMapping("/generation/formation/{formationId}")
    public ResponseEntity<ExamenDto> genererDepuisFormation(
            @PathVariable Long formationId,
            @RequestBody(required = false) AutoGenerateExamenRequest body) {
        AutoGenerateExamenRequest clean = sanitizeBodyForPath(body);
        ExamenDto out = examenGenerationService.genererDepuisFormation(formationId, clean);
        boolean preview = clean != null && Boolean.TRUE.equals(clean.getPreview());
        return ResponseEntity.status(preview ? HttpStatus.OK : HttpStatus.CREATED).body(out);
    }

    /** Pour les routes avec formationId en path : ne pas laisser un formationId du corps écraser la logique ou prêter à confusion. */
    private static AutoGenerateExamenRequest sanitizeBodyForPath(AutoGenerateExamenRequest body) {
        if (body == null) {
            return null;
        }
        return AutoGenerateExamenRequest.builder()
                .formationId(null)
                .seuilReussi(body.getSeuilReussi() != null ? body.getSeuilReussi() : 60)
                .suffixeTitre(body.getSuffixeTitre())
                .useLlm(body.getUseLlm())
                .preview(body.getPreview())
                .build();
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ExamenDto> update(@PathVariable Long id, @Valid @RequestBody ExamenDto dto) {
        return ResponseEntity.ok(examenService.update(id, dto));
    }

    /**
     * Ajoute des questions squelette (lot Commun par défaut) pour compléter un examen existant.
     * Segment fixe {@code examen/} comme pour {@code formation/.../auto-generate}, pour éviter tout conflit
     * de routage proxy / Gateway avec {@code /{id}}.
     * POST /api/examens/examen/{examenId}/questions-modele — corps : { "nombre": 3, "parcoursInclusion": "COMMUN" }
     */
    @PostMapping("/examen/{examenId}/questions-modele")
    public ResponseEntity<ExamenDto> ajouterQuestionsModele(
            @PathVariable Long examenId,
            @RequestBody(required = false) AjouterQuestionsModeleRequest body) {
        return ResponseEntity.ok(examenService.appendModeleQuestions(examenId, body));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examenService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Passer un examen / Résultats ----------
    @PostMapping("/{examenId:\\d+}/passer")
    public ResponseEntity<PassageExamenDto> passerExamen(
            @PathVariable Long examenId,
            @Valid @RequestBody ReponseExamenRequest request) {
        return ResponseEntity.ok(passageExamenService.soumettreExamen(examenId, request));
    }

    /** Démarre une session d'examen adaptatif (une question à la fois, difficulté ajustée). */
    @PostMapping("/{examenId:\\d+}/adaptatif/demarrer")
    public ResponseEntity<AdaptatifDemarrageDto> demarrerAdaptatif(
            @PathVariable Long examenId,
            @Valid @RequestBody DemarrerAdaptatifRequest body) {
        return ResponseEntity.ok(examenAdaptatifService.demarrer(examenId, body));
    }

    @PostMapping("/{examenId:\\d+}/adaptatif/session/{token}/repondre")
    public ResponseEntity<AdaptatifEtapeReponseDto> repondreAdaptatif(
            @PathVariable Long examenId,
            @PathVariable String token,
            @Valid @RequestBody AdaptatifRepondreRequest body) {
        return ResponseEntity.ok(examenAdaptatifService.repondre(examenId, token, body));
    }

    @GetMapping("/resultats/freelancer/{freelancerId}")
    public ResponseEntity<List<PassageExamenDto>> getResultatsByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(passageExamenService.findByFreelancer(freelancerId));
    }

    @GetMapping("/{examenId:\\d+}/resultats")
    public ResponseEntity<List<PassageExamenDto>> getResultatsByExamen(@PathVariable Long examenId) {
        return ResponseEntity.ok(passageExamenService.findByExamen(examenId));
    }

    @GetMapping("/{examenId:\\d+}/freelancer/{freelancerId}")
    public ResponseEntity<PassageExamenDto> getPassage(@PathVariable Long examenId, @PathVariable Long freelancerId) {
        return ResponseEntity.ok(passageExamenService.getPassageByFreelancerAndExamen(examenId, freelancerId));
    }
}
