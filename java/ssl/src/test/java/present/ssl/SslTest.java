package present.ssl;

import com.google.inject.Guice;
import com.google.inject.servlet.ServletModule;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Test;
import present.jetty.ServerBuilder;
import present.jetty.Servers;

public class SslTest {

  private static final int PORT = Servers.randomPort();

  @Test public void testSsl() throws Exception {
    Server server = Guice.createInjector(new TestModule())
        .getInstance(ServerBuilder.class)
        .httpsPort(PORT)
        .build();
    server.start();

    test("https://local.test.bubble.network:" + PORT + "/test");
    test("https://local.present.co:" + PORT + "/test");

    server.stop();
  }

  private void test(String url) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    Assert.assertEquals(200, c.getResponseCode());
    c.disconnect();
  }

  private static class TestModule extends ServletModule {
    @Override protected void configureServlets() {
      install(new SslModule());
      serve("/test").with(TestServlet.class);
    }
  }

  @Singleton private static class TestServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      response.setStatus(200);
    }
  }
}

