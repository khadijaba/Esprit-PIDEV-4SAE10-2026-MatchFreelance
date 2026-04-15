package com.freelancing.candidature.client;

import com.freelancing.candidature.client.dto.ContractApiResponse;
import com.freelancing.candidature.client.dto.ContractCreatePayload;
import com.freelancing.candidature.client.dto.CommunicationScorePayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "CONTRACT", contextId = "candidatureContractFeign", path = "/api/contracts")
public interface ContractRemoteFeign {

    @PostMapping
    ContractApiResponse create(@RequestBody ContractCreatePayload body);

    @GetMapping("/{id}")
    ContractApiResponse getById(@PathVariable("id") Long id);

    @PutMapping("/{id}/pay")
    ContractApiResponse pay(@PathVariable("id") Long id);

    @PutMapping("/{id}/cancel")
    ContractApiResponse cancel(@PathVariable("id") Long id);

    @GetMapping("/freelancer/{freelancerId}/communication-score")
    CommunicationScorePayload communicationScore(@PathVariable("freelancerId") Long freelancerId);
}
