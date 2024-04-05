package present.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Servlets {

  /** Creates a servlet using a lambda. */
  public static HttpServlet get(final Handler handler) {
    return new HttpServlet() {
      @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
        handler.handle(request, response);
      }
    };
  }

  public interface Handler {
    void handle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
  }
}
