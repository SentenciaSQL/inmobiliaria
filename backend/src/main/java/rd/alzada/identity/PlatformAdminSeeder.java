package rd.alzada.identity;

import rd.alzada.common.Texts;
import rd.alzada.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlatformAdminSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PlatformAdminSeeder.class);

  private final AppProperties properties;
  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  public PlatformAdminSeeder(AppProperties properties, UserRepository users, PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    String email = Texts.lowerEmail(properties.platformAdmin().email());
    String password = properties.platformAdmin().password();
    if (email == null || password == null || password.isBlank()) {
      return;
    }
    if (users.existsByPlatformAdminTrue() || users.existsByEmailIgnoreCase(email)) {
      return;
    }
    User user = new User();
    user.setEmail(email);
    user.setFullName(properties.platformAdmin().name());
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setPlatformAdmin(true);
    user.setStatus(User.Status.ACTIVE);
    users.save(user);
    log.info("Se creó el administrador de la plataforma {}", email);
  }
}
