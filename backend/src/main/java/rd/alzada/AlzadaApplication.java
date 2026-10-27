package rd.alzada;

import rd.alzada.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaAuditing
@EnableMethodSecurity
@EnableScheduling
@EnableTransactionManagement(order = 0)
public class AlzadaApplication {

  public static void main(String[] args) {
    SpringApplication.run(AlzadaApplication.class, args);
  }
}
