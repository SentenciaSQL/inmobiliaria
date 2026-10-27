package rd.alzada.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alzada")
public record AppProperties(
    String jwtSecret,
    int jwtAccessMinutes,
    int jwtRefreshDays,
    boolean cookieSecure,
    String corsOrigins,
    String publicUrl,
    Storage storage,
    Uploads uploads,
    Reservations reservations,
    Notifications notifications,
    PlatformAdmin platformAdmin) {

  public record Storage(
      String type,
      String localDir,
      String s3Endpoint,
      String s3Region,
      String s3Bucket,
      String s3AccessKey,
      String s3SecretKey,
      boolean s3PathStyle) {}

  public record Uploads(long logoMaxBytes, long imageMaxBytes, long documentMaxBytes) {}

  public record Reservations(int defaultHours, int expiringNoticeHours) {}

  public record Notifications(boolean emailEnabled, String from, boolean whatsappEnabled, int reminderDays) {}

  public record PlatformAdmin(String email, String password, String name) {}

  public List<String> corsOriginList() {
    if (corsOrigins == null || corsOrigins.isBlank()) {
      return List.of();
    }
    return Arrays.stream(corsOrigins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
  }
}
