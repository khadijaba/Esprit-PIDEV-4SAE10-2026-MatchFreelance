<<<<<<< HEAD
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
=======
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
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
