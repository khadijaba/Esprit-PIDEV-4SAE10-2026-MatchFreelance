package com.freelancing.interview.client;

import com.freelancing.interview.client.dto.CandidatureSnapshotDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "CANDIDATURE", contextId = "interviewCandidatureFeign", path = "/api/candidatures")
public interface CandidatureRemoteFeign {

    @GetMapping("/{id}")
    CandidatureSnapshotDto getById(@PathVariable("id") Long id);
}
