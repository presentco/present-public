package present.web;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import present.proto.GroupResponse;
import present.proto.ResolveUrlResponse;
import present.wire.rpc.core.ClientException;

/**
 * Used to share groups via SMS.
 *
 * @author Bob Lee (bob@present.co)
 */
@Singleton
public class SmsServlet extends HttpServlet {

  static Pattern urlPattern = Pattern.compile("https?://(staging.)?present.co/[gu]/.*");

  // Copied from UrlEscapers. Don't use + for space—our iOS decoder doesn't support it.
  private static final Escaper URL_FORM_PARAMETER_ESCAPER =
      new PercentEscaper("-_.*", false);

  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String url = request.getParameter("url");
    if (url == null || !urlPattern.matcher(url).matches()) {
      response.sendError(400, "Invalid url");
      return;
    }
    ResolveUrlResponse resolved;
    try {
      // We don't really need to resolve users but it does reduce the potential attack surface.
      resolved = UrlResolvers.resolve(url);
    } catch (ClientException e) {
      response.sendError(404, "Not found");
      return;
    }

    String body;
    if (resolved.group != null) {
      body = "Join me in the '" + resolved.group.title + "' circle on Present! " + url;
    } else if (resolved.user != null) {
      body = "Join me on Present! " + url;
    } else {
      throw new AssertionError();
    }

    String encodedBody = URL_FORM_PARAMETER_ESCAPER.escape(body);
    String agent = request.getHeader("User-Agent").toLowerCase();
    // See https://stackoverflow.com/questions/6480462/how-to-pre-populate-the-sms-body-text-via-an-html-link
    if (agent.contains("iphone")) {
      response.sendRedirect("sms:&body=" + encodedBody);
    } else if (agent.contains("android")) {
      response.sendRedirect("sms:?body=" + encodedBody);
    } else {
      // Redirect to the circle on desktop.
      response.sendRedirect(url);
    }
  }
}
