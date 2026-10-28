package rd.alzada.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class RlsSession {

  @PersistenceContext
  private EntityManager entityManager;

  public void apply() {
    Session session = entityManager.unwrap(Session.class);
    session.doWork(connection -> {
      try {
        TenantAwareDataSource.apply(connection);
      } catch (SQLException exception) {
        throw new IllegalStateException("No se pudo establecer el contexto de la base de datos", exception);
      }
    });
  }

  public Connection connection() {
    return entityManager.unwrap(Session.class).doReturningWork(connection -> connection);
  }
}
