package rd.alzada.common;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public boolean tryAcquire(String key, int limit, Duration window) {
    Instant now = Instant.now();
    Window current = windows.compute(key, (ignored, existing) -> {
      if (existing == null || existing.expiresAt.isBefore(now)) {
        return new Window(1, now.plus(window));
      }
      existing.count++;
      return existing;
    });
    return current.count <= limit;
  }

  private static final class Window {
    private int count;
    private final Instant expiresAt;

    private Window(int count, Instant expiresAt) {
      this.count = count;
      this.expiresAt = expiresAt;
    }
  }
}
