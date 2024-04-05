package present.server;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.ContentRequest;
import present.proto.ContentResponse;
import present.proto.ContentService;
import present.proto.ContentUploadRequest;
import present.server.model.content.Content;
import present.wire.rpc.core.ClientException;

/**
 * @author Pat Niemeyer pat@pat.net
 */
public class AppEngineContentService implements ContentService {

  private static final Logger logger = LoggerFactory.getLogger(AppEngineContentService.class);

  public AppEngineContentService() { }

  @Override public ContentResponse putContent(ContentUploadRequest request) throws IOException {
    return Content.createAndUpload(request).toResponse();
  }

  @Override public ContentResponse getContent(ContentRequest request) throws IOException {
    Content content = Content.get(request.uuid);
    if (content == null) throw new ClientException("Missing content.");
    return content.toResponse();
  }
}
