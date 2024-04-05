package present.web;

import com.google.common.io.ByteStreams;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton public class MobileAppServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger(MobileAppServlet.class);

  private static final String APP_PATH = "/m/index.html";

  private byte[] html;

  @Override public void init(ServletConfig config) throws ServletException {
    try {
      InputStream in = config.getServletContext().getResourceAsStream(APP_PATH);
      if (in == null) {
        logger.error("Missing mobile web app");
        return;
      }
      this.html = ByteStreams.toByteArray(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/html; charset=utf-8");
    try (ServletOutputStream out = response.getOutputStream()) {
      out.write(html);
    }
  }
}
