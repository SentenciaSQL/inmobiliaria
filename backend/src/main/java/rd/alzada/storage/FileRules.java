package rd.alzada.storage;

import rd.alzada.common.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public final class FileRules {

  private FileRules() {}

  public static byte[] read(MultipartFile file, long maxBytes) {
    if (file == null || file.isEmpty()) {
      throw ApiException.badRequest("Seleccione un archivo");
    }
    try (InputStream input = file.getInputStream()) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      long total = 0;
      int read;
      while ((read = input.read(buffer)) >= 0) {
        total += read;
        if (total > maxBytes) {
          throw ApiException.tooLarge("El archivo supera el tamaño permitido");
        }
        output.write(buffer, 0, read);
      }
      if (total == 0) {
        throw ApiException.badRequest("El archivo está vacío");
      }
      return output.toByteArray();
    } catch (IOException exception) {
      throw ApiException.badRequest("No se pudo leer el archivo");
    }
  }

  public static StoredFile image(byte[] content) {
    if (content.length >= 8
        && (content[0] & 0xFF) == 0x89
        && content[1] == 0x50
        && content[2] == 0x4E
        && content[3] == 0x47) {
      return new StoredFile(content, "image/png", "png");
    }
    if (content.length >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF) {
      return new StoredFile(content, "image/jpeg", "jpg");
    }
    if (content.length >= 12
        && content[0] == 'R'
        && content[1] == 'I'
        && content[2] == 'F'
        && content[3] == 'F'
        && content[8] == 'W'
        && content[9] == 'E'
        && content[10] == 'B'
        && content[11] == 'P') {
      return new StoredFile(content, "image/webp", "webp");
    }
    throw ApiException.badRequest("El logo o la imagen debe ser PNG, JPEG o WebP. SVG no está permitido");
  }

  public static StoredFile document(byte[] content) {
    try {
      return image(content);
    } catch (ApiException ignored) {
      if (content.length >= 5
          && content[0] == '%'
          && content[1] == 'P'
          && content[2] == 'D'
          && content[3] == 'F'
          && content[4] == '-') {
        return new StoredFile(content, "application/pdf", "pdf");
      }
      throw ApiException.badRequest("El documento debe ser PDF, PNG, JPEG o WebP");
    }
  }
}
