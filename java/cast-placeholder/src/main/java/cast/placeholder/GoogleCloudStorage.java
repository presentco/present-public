package cast.placeholder;

import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import java.io.IOException;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Google Cloud Storage utilities.
 *
 * Note: This class stores files publicly. We rely on the fact that UUIDs are nearly impossible
 * to guess for security. We should treat URLs as sensitive information.
 *
 * @author Bob Lee (bob@present.co)
 */
public class GoogleCloudStorage {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

  private GoogleCloudStorage() {}

  /** Returns the default GCS bucket. */
  public static String defaultBucket() {
    return Environment.inDevelopment() ? "development" : SystemProperty.applicationId.get();
  }

  /** Returns the GCS file name for given path. */
  public static GcsFilename fileForPath(String path) {
    return new GcsFilename(defaultBucket(), path);
  }

  /** Returns the GCS file name for given content. */
  public static GcsFilename fileForImage(String uuid) {
    return fileForPath("images/" + uuid + ".jpeg");
  }

  /** Uploads the given content and returns its public URL. */
  public static String upload(ByteString jpeg, String uuid) throws IOException {
    return upload(fileForImage(uuid), jpeg);
  }

  /** Uploads the given content and returns its public URL. */
  public static String upload(GcsFilename file, ByteString jpeg)
      throws IOException {
    logger.debug("Uploading to : "+file);
    GcsService gcsService = GcsServiceFactory.createGcsService();
    GcsFileOptions options = new GcsFileOptions.Builder()
        .acl("public_read")
        .mimeType("image/jpeg")
        .build();
    gcsService.createOrReplace(file, options, jpeg.asByteBuffer());
    return urlFor(file);
  }

  /** Returns the public URL for the given GCS file name. */
  public static String urlFor(GcsFilename file) {
    String path = file.getObjectName();
    if (Environment.inDevelopment()) return "http://localhost:8080/gcs/" + path;
    return "https://storage-download.googleapis.com/" + file.getBucketName() + "/" + path;
  }
}
