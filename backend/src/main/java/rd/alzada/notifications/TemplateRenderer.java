package rd.alzada.notifications;

import java.util.Map;

public final class TemplateRenderer {

  private TemplateRenderer() {}

  public static String render(String template, Map<String, String> variables) {
    String rendered = template == null ? "" : template;
    if (variables == null) {
      return rendered;
    }
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      rendered = rendered.replace("{{" + entry.getKey() + "}}", escape(entry.getValue()));
    }
    return rendered;
  }

  public static String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }
}
