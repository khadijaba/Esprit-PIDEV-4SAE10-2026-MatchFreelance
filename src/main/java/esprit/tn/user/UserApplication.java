package esprit.tn.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableDiscoveryClient

@SpringBootApplication(scanBasePackages = {
		"esprit.tn.user",
		"Config",
		"Security",
		"Service",
		"Controller",
		"Repository",
		"Entity"
})
@EnableJpaRepositories(basePackages = "Repository")  // ← Add this
@EntityScan(basePackages = "Entity")
public class UserApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserApplication.class, args);
	}

}
