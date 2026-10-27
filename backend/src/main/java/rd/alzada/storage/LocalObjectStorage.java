package rd.alzada.storage;

import rd.alzada.common.ApiException;
import rd.alzada.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalObjectStorage implements ObjectStorage {

  private final Path root;

  public LocalObjectStorage(AppProperties properties) {
    this.root = Path.of(properties.storage().localDir()).toAbsolutePath().normalize();
  }

  @Override
  public void put(String key, byte[] content, String contentType) {
    Path path = resolve(key);
    try {
      Files.createDirectories(path.getParent());
      Files.write(path, content);
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo guardar el archivo");
    }
  }

  @Override
  public byte[] get(String key) {
    Path path = resolve(key);
    if (!Files.exists(path)) {
      throw ApiException.notFound("El archivo no está disponible");
    }
    try {
      return Files.readAllBytes(path);
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo leer el archivo");
    }
  }

  @Override
  public void delete(String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo eliminar el archivo");
    }
  }

  private Path resolve(String key) {
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root)) {
      throw ApiException.badRequest("La referencia del archivo no es válida");
    }
    return path;
  }
}
