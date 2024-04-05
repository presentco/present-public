package present.server;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables cross-site requests.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS
 *
 * @author Bob Lee (bob@present.co)
 */
public class CorsFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class.getName());

  private static final Set<String> ALLOWED_ORIGINS = ImmutableSet.of(
      "http://localhost:3000",
      "http://localhost:8080",
      "http://localhost:8000",

      "http://local.present.co:3000",
      "http://local.present.co:8080",
      "http://local.present.co:8000",

      "http://staging.present.co",
      "https://staging.present.co",

      "http://present.co",
      "https://present.co",

      "https://present-staging.appspot.com",
      "https://present-production.appspot.com",

      "http://pegah.present.co:3000",

      "https://present-gabrielle.appspot.com",
      "http://gabrielle.present.co",
      "https://gabrielle.present.co"
  );

  @Override public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String origin = httpRequest.getHeader("Origin");
    if (origin != null) {
      if (ALLOWED_ORIGINS.contains(origin)) {
        httpResponse.addHeader("Access-Control-Allow-Origin", origin);
        if ("OPTIONS".equals(httpRequest.getMethod())) {
          httpResponse.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
          httpResponse.addHeader("Access-Control-Allow-Headers", "Content-Type");
          httpResponse.addHeader("Access-Control-Max-Age", "86400"); // 1 day
        }
      } else {
        logger.info("Request from disallowed origin: " + origin);
      }
    }

    filterChain.doFilter(request, response);
  }

  @Override public void init(FilterConfig filterConfig) throws ServletException {}
  @Override public void destroy() {}
}
