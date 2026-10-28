package rd.alzada.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DataSourceConfig {

  @Bean
  @FlywayDataSource
  public DataSource flywayDataSource(Environment environment) {
    return hikari(environment, environment.getRequiredProperty("spring.flyway.user"), environment.getRequiredProperty("spring.flyway.password"), 2);
  }

  @Bean
  @Primary
  public DataSource dataSource(Environment environment) {
    int poolSize = environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 10);
    HikariDataSource hikari = hikari(
        environment,
        environment.getRequiredProperty("spring.datasource.username"),
        environment.getRequiredProperty("spring.datasource.password"),
        poolSize);
    return new TenantAwareDataSource(hikari);
  }

  private static HikariDataSource hikari(Environment environment, String username, String password, int poolSize) {
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(environment.getRequiredProperty("spring.datasource.url"));
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setMaximumPoolSize(poolSize);
    return dataSource;
  }
}
