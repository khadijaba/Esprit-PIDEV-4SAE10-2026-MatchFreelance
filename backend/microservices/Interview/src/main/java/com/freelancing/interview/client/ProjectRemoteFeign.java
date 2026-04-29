package com.freelancing.interview.client;

import com.freelancing.interview.client.dto.ProjectRemotePayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PROJECT", contextId = "interviewProjectFeign", path = "/projects")
public interface ProjectRemoteFeign {

    @GetMapping("/{id}")
    ProjectRemotePayload getById(@PathVariable("id") Long id);
}
