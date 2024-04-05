package present.server;

import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import java.io.IOException;
import okio.ByteString;
import present.proto.ContentType;
import present.proto.ContentUploadRequest;
import present.server.environment.Environment;

/**
 * Google Cloud Storage utilities.
 *
 * Note: This class stores files publicly. We rely on the fact that UUIDs are nearly impossible
 * to guess for security. We should treat URLs as sensitive information.
 *
 * @author Bob Lee (bob@present.co)
 */
public class GoogleCloudStorage {

  private GoogleCloudStorage() {}

  /** The default path prefix for uploaded photos and images within the bucket */
  public static String DEFAULT_CONTENT_PATH = "content";

  /** Uploads the given content and returns its public URL. */
  public static String upload(ContentUploadRequest content) throws IOException {
    return upload(content.type, content.uuid, DEFAULT_CONTENT_PATH, content.content, content.contentThumbnail);
  }

  /**
   * Uploads the given content and returns its public URL.
   * @param contentBytes The bytes of the content image or video
   * @param contentThumbnailBytes The bytes of a JPG image representation of the content
   * */
  public static String upload(ContentType type, String uuid, String pathPrefix,
      ByteString contentBytes, ByteString contentThumbnailBytes ) throws IOException {
    if (contentThumbnailBytes != null) {
      upload(ContentType.JPEG, fileForContentThumbnail(type, uuid, pathPrefix), contentThumbnailBytes);
    }
    return upload(type, fileForContent(type, uuid, pathPrefix), contentBytes);
  }

  public static String urlForContent(ContentType type, String uuid, String pathPrefix) {
    return urlFor(fileForContent(type, uuid, pathPrefix));
  }

  public static String urlForContentThumbnail(ContentType type, String uuid, String pathPrefix) {
    return urlFor(fileForContentThumbnail(type, uuid, pathPrefix));
  }

  /** Returns the public URL for the given GCS file name. */
  public static String urlFor(GcsFilename file) {
    String path = file.getObjectName();
    if (Environment.isDevelopment()) return "http://localhost:8081/gcs/" + path;
    return "https://storage-download.googleapis.com/" + file.getBucketName() + "/" + path;
  }

  /**
   * Get the GCS file name for given content based on a type, uuid, and path.
   * This method is idempotent (does not allocate resources).
   **/
  private static GcsFilename fileForContent(ContentType type, String uuid, String pathPrefix) {
    String ext;
    switch(type) {
      case JPEG:
        ext = "jpeg";
        break;
      case MP4:
        ext = "mp4";
        break;
      default:
        throw new AssertionError();
    }
    return fileForPath(pathPrefix + "/" + uuid + "." + ext);
  }

  /**
   * Returns the GCS file name for a JPG thumbnail representing the content.
   * @param type the type of the full content (the original video or full size photo).
   * */
  private static GcsFilename fileForContentThumbnail(ContentType type, String uuid, String pathPrefix) {
    return fileForPath(pathPrefix + "/" + uuid + "-thumb.jpeg");
  }

  /** Returns the GCS file name for given path in the default, per-servter bucket. */
  public static GcsFilename fileForPath(String path) {
    return new GcsFilename(defaultBucket(), path);
  }

  /** Returns the default GCS bucket. */
  private static String defaultBucket() {
    return Environment.applicationId();
  }

  /** Uploads the given content and returns its public URL. */
  private static String upload(ContentType type, GcsFilename file, ByteString bytes) throws IOException {
    GcsService gcsService = GcsServiceFactory.createGcsService();
    GcsFileOptions options = new GcsFileOptions.Builder()
        .acl("public_read")
        .mimeType(mimeType(type))
        .build();
    gcsService.createOrReplace(
        file,
        options,
        bytes.asByteBuffer());
    return urlFor(file);
  }

  /** Returns the standard MIME type for the given content type enum. */
  private static String mimeType(ContentType type) {
    switch(type) {
        case JPEG:
          return "image/jpeg";
        case MP4:
          return "video/mp4";
      default:
        throw new AssertionError();
    }
  }

}
