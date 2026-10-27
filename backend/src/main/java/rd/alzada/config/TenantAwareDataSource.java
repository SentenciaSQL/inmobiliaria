package rd.alzada.config;

import rd.alzada.common.TenantContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Aplica las variables de sesión que usan las políticas de fila de PostgreSQL
 * en cada conexión y las limpia al devolverla al pool.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

  public TenantAwareDataSource(DataSource delegate) {
    super(delegate);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return wrap(super.getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return wrap(super.getConnection(username, password));
  }

  private Connection wrap(Connection connection) throws SQLException {
    try {
      apply(connection);
    } catch (SQLException exception) {
      connection.close();
      throw exception;
    }
    return (Connection) Proxy.newProxyInstance(
        Connection.class.getClassLoader(),
        new Class<?>[] {Connection.class},
        (proxy, method, args) -> {
          if ("close".equals(method.getName()) && (args == null || args.length == 0)) {
            try {
              clear(connection);
            } catch (SQLException ignored) {
              // La conexión debe volver al pool aunque la limpieza falle.
            }
            connection.close();
            return null;
          }
          try {
            return method.invoke(connection, args);
          } catch (InvocationTargetException exception) {
            throw exception.getCause();
          }
        });
  }

  public static void apply(Connection connection) throws SQLException {
    TenantContext.State state = TenantContext.state();
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT set_config('app.user_id', ?, false), set_config('app.tenant_id', ?, false), set_config('app.bypass_rls', ?, false), set_config('app.access_mode', ?, false)")) {
      statement.setString(1, state.userId() == null ? "" : state.userId().toString());
      statement.setString(2, state.tenantId() == null ? "" : state.tenantId().toString());
      statement.setString(3, state.bypass() ? "true" : "false");
      statement.setString(4, state.mode().dbValue());
      statement.execute();
    }
  }

  private static void clear(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT set_config('app.user_id', '', false), set_config('app.tenant_id', '', false), set_config('app.bypass_rls', 'false', false), set_config('app.access_mode', '', false)")) {
      statement.execute();
    }
  }
}
