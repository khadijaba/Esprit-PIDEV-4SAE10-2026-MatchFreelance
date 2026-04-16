package esprit.project.client;

import esprit.project.dto.ApplicationCountResponse;
import esprit.project.dto.candidature.CandidatureSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "CANDIDATURE", contextId = "projectCandidatureApi", path = "/api/candidatures")
public interface CandidatureClient {

    @GetMapping("/freelancer/{freelancerId}")
    List<CandidatureSummaryDto> listByFreelancer(@PathVariable("freelancerId") Long freelancerId);

    @GetMapping("/project/{projectId}/application-count")
    ApplicationCountResponse countApplications(
            @PathVariable("projectId") Long projectId,
            @RequestParam("clientId") Long clientId);
}
