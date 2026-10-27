package rd.alzada.common;

import java.util.UUID;

public final class TenantContext {

  public enum Mode {
    TENANT("tenant"),
    MARKETPLACE("marketplace"),
    SYSTEM("system");

    private final String dbValue;

    Mode(String dbValue) {
      this.dbValue = dbValue;
    }

    public String dbValue() {
      return dbValue;
    }
  }

  public record State(UUID userId, UUID tenantId, Mode mode, boolean bypass) {}

  private static final ThreadLocal<State> STATE = new ThreadLocal<>();

  private TenantContext() {}

  public static State state() {
    State current = STATE.get();
    return current == null ? new State(null, null, Mode.TENANT, false) : current;
  }

  public static UUID userId() {
    return state().userId();
  }

  public static UUID tenantId() {
    return state().tenantId();
  }

  public static boolean bypass() {
    return state().bypass();
  }

  public static void asTenant(UUID userId, UUID tenantId) {
    STATE.set(new State(userId, tenantId, Mode.TENANT, false));
  }

  public static void asMarketplace(UUID userId) {
    STATE.set(new State(userId, null, Mode.MARKETPLACE, false));
  }

  public static void asSystem(UUID userId) {
    STATE.set(new State(userId, null, Mode.SYSTEM, true));
  }

  public static void setUser(UUID userId) {
    State current = state();
    STATE.set(new State(userId, current.tenantId(), current.mode(), current.bypass()));
  }

  public static void clear() {
    STATE.remove();
  }
}
