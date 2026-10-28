package rd.alzada.config;

import rd.alzada.common.TenantContext;
import java.util.UUID;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaTenantConfig {

  public static final UUID ROOT_TENANT = new UUID(0L, 0L);

  @Bean
  public CurrentTenantIdentifierResolver<UUID> tenantIdentifierResolver() {
    return new CurrentTenantIdentifierResolver<>() {
      @Override
      public UUID resolveCurrentTenantIdentifier() {
        UUID tenantId = TenantContext.tenantId();
        return tenantId == null ? ROOT_TENANT : tenantId;
      }

      @Override
      public boolean validateExistingCurrentSessions() {
        return true;
      }

      @Override
      public boolean isRoot(UUID tenantId) {
        return ROOT_TENANT.equals(tenantId);
      }
    };
  }

  @Bean
  public HibernatePropertiesCustomizer tenantHibernateCustomizer(CurrentTenantIdentifierResolver<UUID> resolver) {
    return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
  }
}
