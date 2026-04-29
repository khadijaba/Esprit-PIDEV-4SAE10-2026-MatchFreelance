package com.freelancing.candidature;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.freelancing.candidature.client")
public class CandidatureServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CandidatureServiceApplication.class, args);
    }
}
