package tn.esprit.evaluation.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Eureka / noms de service ({@code http://FORMATION/...}). Ne pas utiliser pour des URLs absolues
     * ({@code http://127.0.0.1:11434}) : le load-balancer interpréterait l’hôte comme un serviceId.
     */
    @Bean
    @Primary
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Appels HTTP directs : Ollama (inférence parfois lente), microservices Python, etc.
     * Délais longs pour éviter des coupures silencieuses côté client HTTP.
     */
    @Bean(name = "externalRestTemplate")
    public RestTemplate externalRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(600_000);
        return new RestTemplate(factory);
    }
}

