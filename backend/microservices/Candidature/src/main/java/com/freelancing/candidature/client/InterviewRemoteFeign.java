package com.freelancing.candidature.client;

import com.freelancing.candidature.dto.InterviewMetricsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "INTERVIEW", contextId = "candidatureInterviewFeign", path = "/api/interviews")
public interface InterviewRemoteFeign {

    @GetMapping("/candidature/{candidatureId}/metrics")
    InterviewMetricsDto getMetrics(
            @PathVariable("candidatureId") Long candidatureId,
            @RequestParam("requestingUserId") Long requestingUserId);
}
