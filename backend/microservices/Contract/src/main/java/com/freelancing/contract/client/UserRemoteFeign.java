package com.freelancing.contract.client;

import com.freelancing.contract.client.dto.UserRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "USER", contextId = "contractUserFeign", path = "/api/users")
public interface UserRemoteFeign {

    @GetMapping("/{id}")
    UserRemoteDto getUserById(@PathVariable("id") Long id);
}
