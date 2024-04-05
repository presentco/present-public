package present.server;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.CategoryResponse;
import present.proto.ResolveUrlRequest;
import present.proto.ResolveUrlResponse;
import present.proto.UrlResolverService;
import present.wire.rpc.core.ClientException;

import static com.google.common.base.Preconditions.checkArgument;

public class AppEngineUrlResolverService implements UrlResolverService {

  private static Logger logger = LoggerFactory.getLogger(AppEngineUrlResolverService.class);

  @Override public ResolveUrlResponse resolveUrl(ResolveUrlRequest request)
      throws IOException {
    URI url = URI.create(request.url);
    String path = url.getRawPath();

    // Apps some times try to resolve: https://present.co/.
    if (path.equals("/")) throw new ClientException("Not found.");

    checkArgument(path.charAt(0) == '/'
            && path.charAt(2) == '/'
            && path.length() > 3,
        "url: %s", request.url);

    if (path.startsWith("/t/")) {
      String category = URLDecoder.decode(path.substring(3), "UTF-8");
      // TODO: Validate category.
      return new ResolveUrlResponse.Builder()
          .type(ResolveUrlResponse.Type.CATEGORY)
          .category(new CategoryResponse(category))
          .build();
    }

    return ShortLinks.resolve(url);
  }
}
