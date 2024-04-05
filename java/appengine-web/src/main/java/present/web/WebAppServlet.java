package present.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.UUID;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.GroupResponse;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.proto.ResolveUrlRequest;
import present.proto.ResolveUrlResponse;
import present.proto.UrlResolverService;
import present.proto.UserResponse;
import present.server.environment.Environment;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.ClientException;

@Singleton public class WebAppServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger(WebAppServlet.class);

  private static final String APP_PATH = "/app/wrapper.html";
  private static final String DEFAULT_PREVIEW = "https://present.co/images/featured-image.jpg";

  private static final Parameters DEFAULT_PARAMETERS = new Parameters(
      "Find Your Circle",
      DEFAULT_PREVIEW,
      "https://present.co/",
      "present://",
      ""
  );

  private Mustache mustache;

  @Override public void init(ServletConfig config) throws ServletException {
    MustacheFactory mf = new DefaultMustacheFactory();
    mustache = mf.compile(new InputStreamReader(
        config.getServletContext().getResourceAsStream(APP_PATH), Charsets.UTF_8), APP_PATH);
  }

  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    boolean mobile = isMobile(request);
    String url = request.getRequestURL().toString();
    if (request.getServerName().toLowerCase().startsWith("app.")) {
      if (!mobile) {
        // The user opened an "app." url on desktop. Redirect to the normal domain.
        response.sendRedirect(url.replace("://app.", "://"));
        return;
      }
    } else {
      if (mobile) {
        // Serve mobile links from "app." domain. This forces the phone to open links in the app
        // instead of the browser.
        response.sendRedirect(url.replace("://", "://app."));
        return;
      }
    }

    response.setContentType("text/html; charset=utf-8");
    try {
      Parameters parameters = parametersFor(request.getRequestURI());
      mustache.execute(response.getWriter(), parameters);
    } catch (ClientException e) {
      logger.warn("Error resolving URL.", e);
      response.sendError(400);
    }
  }

  private static boolean isMobile(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) return false;
    userAgent = userAgent.toLowerCase();
    return userAgent.contains("android") || userAgent.contains("iphone");
  }

  private Parameters parametersFor(String path) throws IOException {
    String iosUrl = "present:/" + path;
    if (path.startsWith("/g/")) {
      ResolveUrlResponse response = UrlResolvers.resolve(path);
      GroupResponse group = response.group;
      return new Parameters(
          group.title,
          group.cover == null ? DEFAULT_PREVIEW : group.cover.content,
          Environment.current().webUrl() + path,
          iosUrl,
          group.locationName
      );
    } else if (path.startsWith("/u/")) {
      ResolveUrlResponse response = UrlResolvers.resolve(path);
      UserResponse user = response.user;
      return new Parameters(
          user.name,
          user.photo,
          Environment.current().webUrl() + path,
          iosUrl,
          null
      );
    } else if (path.startsWith("/t/")) {
      String category = URLDecoder.decode(path.substring(3), "UTF-8");
      return new Parameters(
          category,
          DEFAULT_PREVIEW,
          Environment.current().webUrl() + path,
          iosUrl,
          null
      );
    } else if (path.startsWith("/v/")) {
      return new Parameters(
          "Log in to Present",
          DEFAULT_PREVIEW,
          Environment.current().webUrl() + path,
          iosUrl,
          null
      );
    }

    return DEFAULT_PARAMETERS;
  }

  static class Parameters {

    final String title;
    final String photo;
    final String url;
    final String iosUrl;
    final String locationName;
    final String basePath = "/app";

    public Parameters(String title, String photo, String url, String iosUrl,
        String locationName) {
      this.title = title;
      this.photo = photo;
      this.url = url;
      this.iosUrl = iosUrl;
      this.locationName = locationName;
    }
  }
}
