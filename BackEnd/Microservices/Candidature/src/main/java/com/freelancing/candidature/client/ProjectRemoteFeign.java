package com.freelancing.candidature.client;

import com.freelancing.candidature.client.dto.ProjectRemotePayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "PROJECT", contextId = "candidatureProjectFeign", path = "/projects")
public interface ProjectRemoteFeign {

    @GetMapping("/{id}")
    ProjectRemotePayload getById(@PathVariable("id") Long id);

    @PutMapping("/{id}")
    ProjectRemotePayload update(@PathVariable("id") Long id, @RequestBody ProjectRemotePayload body);
}
