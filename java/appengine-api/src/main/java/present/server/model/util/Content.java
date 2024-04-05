package present.server.model.util;

import present.proto.ContentType;

// TODO: Remove after migration
/**
 * Represents a media item such as an image or video.
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Content {

  /** A unique id for this content */
  public String uuid;

  /** Type of attached content. */
  public ContentType contentType;

  public Content() { }

  public Content(String uuid, ContentType contentType) {
    this.uuid = uuid;
    this.contentType = contentType;
  }

}


