package com.freelancing.contract.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ContractAiProperties.class)
public class ContractAiConfig {

    @Bean
    @Qualifier("ollamaRestClient")
    public RestClient ollamaRestClient(ContractAiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(
                Duration.ofSeconds(properties.getOllama().getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getOllama().getReadTimeoutSeconds()));
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getOllama().getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "http://localhost:11434";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
