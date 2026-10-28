package rd.alzada.common;

import java.text.Normalizer;
import java.util.Locale;

public final class Texts {

  private Texts() {}

  public static String fold(String value) {
    if (value == null) {
      return "";
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }

  public static String slug(String value) {
    String folded = fold(value).replace(' ', '-');
    folded = folded.replaceAll("^-+|-+$", "");
    if (folded.isBlank()) {
      return "item";
    }
    return folded.length() > 150 ? folded.substring(0, 150) : folded;
  }

  public static String like(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String escaped = fold(value).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped.replace(' ', '%') + "%";
  }

  public static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static String lowerEmail(String email) {
    String value = blankToNull(email);
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }

  public static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() <= max ? value : value.substring(0, max);
  }
}
