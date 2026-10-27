package rd.alzada.storage;

import rd.alzada.common.ApiException;
import rd.alzada.config.AppProperties;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3ObjectStorage implements ObjectStorage {

  private static final Logger log = LoggerFactory.getLogger(S3ObjectStorage.class);

  private final S3Client client;
  private final String bucket;

  public S3ObjectStorage(AppProperties properties) {
    AppProperties.Storage storage = properties.storage();
    this.bucket = storage.s3Bucket();
    this.client = S3Client.builder()
        .endpointOverride(URI.create(storage.s3Endpoint()))
        .region(Region.of(storage.s3Region()))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(storage.s3AccessKey(), storage.s3SecretKey())))
        .forcePathStyle(storage.s3PathStyle())
        .build();
  }

  public void ensureBucket() {
    try {
      client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        return;
      }
      log.warn("No se pudo comprobar el bucket de archivos");
    }
  }

  @Override
  public void put(String key, byte[] content, String contentType) {
    client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(), RequestBody.fromBytes(content));
  }

  @Override
  public byte[] get(String key) {
    try {
      return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    } catch (NoSuchKeyException exception) {
      throw ApiException.notFound("El archivo no está disponible");
    }
  }

  @Override
  public void delete(String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    try {
      client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    } catch (S3Exception exception) {
      log.warn("No se pudo eliminar un archivo almacenado");
    }
  }
}
