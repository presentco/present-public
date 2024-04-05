package present.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Forwards /.well-known/* to /*. Works around fact that App Engine can't serve up /.well-known.
 *
 * @author Bob Lee (bob@present.co)
 */
public class WellKnownServlet extends HttpServlet {
  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.getRequestDispatcher(request.getPathInfo()).forward(request, response);
  }
}
