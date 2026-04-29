package tn.esprit.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class EvaluationApplication {

    private static final Logger log = LoggerFactory.getLogger(EvaluationApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EvaluationApplication.class, args);
    }

    /**
     * Repère visible dans la console : si ce message n’apparaît pas après redémarrage, ce n’est pas ce JAR / ce module qui tourne.
     */
    @Bean
    ApplicationRunner logQuestionAiRoutesRegistered() {
        return args -> log.info(
                "EVALUATION démarré — validation IA : POST /api/examens/validate-question-ai | contrôle GET .../validate-question-ai/ping");
    }
}
