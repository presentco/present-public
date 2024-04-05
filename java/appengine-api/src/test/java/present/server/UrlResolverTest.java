package present.server;

import java.io.IOException;
import java.net.URLEncoder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.ResolveUrlRequest;
import present.proto.ResolveUrlResponse;
import present.proto.UrlResolverService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UrlResolverTest {

  @Test public void resolveCategory() throws IOException {
    UrlResolverService urlResolver = new AppEngineUrlResolverService();
    String category = "Bob's Category 🦄";
    String url = "https://present.co/t/" + URLEncoder.encode(category, "UTF-8");
    ResolveUrlResponse response = urlResolver.resolveUrl(new ResolveUrlRequest(url));
    assertEquals(ResolveUrlResponse.Type.CATEGORY, response.type);
    assertEquals(category, response.category.name);
  }
}
