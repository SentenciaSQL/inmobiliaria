package rd.alzada.common;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class HttpFiles {

  private HttpFiles() {}

  public static ResponseEntity<byte[]> inlineImage(byte[] content, String contentType) {
    return ResponseEntity.ok()
        .contentType(safeImage(contentType))
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
        .body(content);
  }

  public static ResponseEntity<byte[]> download(byte[] content, String contentType, String fileName) {
    ContentDisposition disposition = ContentDisposition.attachment()
        .filename(safeName(fileName), StandardCharsets.UTF_8)
        .build();
    return ResponseEntity.ok()
        .contentType(safeType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.noStore())
        .body(content);
  }

  private static MediaType safeImage(String contentType) {
    if ("image/png".equals(contentType) || "image/jpeg".equals(contentType) || "image/webp".equals(contentType)) {
      return MediaType.parseMediaType(contentType);
    }
    return MediaType.APPLICATION_OCTET_STREAM;
  }

  private static MediaType safeType(String contentType) {
    if ("image/png".equals(contentType) || "image/jpeg".equals(contentType) || "image/webp".equals(contentType) || "application/pdf".equals(contentType)) {
      return MediaType.parseMediaType(contentType);
    }
    return MediaType.APPLICATION_OCTET_STREAM;
  }

  private static String safeName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "archivo";
    }
    return fileName.replaceAll("[\\r\\n\"]", "").trim();
  }
}
