package esprit.skill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
<<<<<<< HEAD
=======
<<<<<<< HEAD

@EnableDiscoveryClient

=======
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
<<<<<<< HEAD
=======
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
>>>>>>> b7e93fa9abcd913d3ba37913b8481d5dd480ed43
@SpringBootApplication
public class SkillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillApplication.class, args);
    }

}
