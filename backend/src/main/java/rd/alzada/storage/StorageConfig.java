package rd.alzada.storage;

import rd.alzada.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

  @Bean
  public ObjectStorage objectStorage(AppProperties properties) {
    if ("local".equalsIgnoreCase(properties.storage().type())) {
      return new LocalObjectStorage(properties);
    }
    S3ObjectStorage storage = new S3ObjectStorage(properties);
    storage.ensureBucket();
    return storage;
  }
}
