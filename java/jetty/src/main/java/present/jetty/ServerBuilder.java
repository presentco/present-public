package present.jetty;

import com.google.common.base.Preconditions;
import com.google.inject.servlet.GuiceFilter;
import java.security.KeyStore;
import java.util.EnumSet;
import javax.inject.Inject;
import javax.servlet.ServletException;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import static org.eclipse.jetty.http.HttpVersion.*;

/**
 * Builds a Jetty Server configured with Guice. Instantiated using Guice.
 *
 * @author Bob Lee (bob@present.co)
 */
public class ServerBuilder {

  /*
   * See http://www.eclipse.org/jetty/documentation/current/embedding-jetty.html.
   */

  private static int DISABLED = -1;

  private final KeyStore keyStore;
  private final GuiceFilter guiceFilter;

  @Inject private ServerBuilder(KeyStore keyStore, GuiceFilter guiceFilter)
      throws ServletException {
    this.keyStore = keyStore;
    this.guiceFilter = guiceFilter;
  }

  private int httpPort = DISABLED;

  public ServerBuilder httpPort(int httpPort) {
    this.httpPort = httpPort;
    return this;
  }

  private int httpsPort = DISABLED;

  public ServerBuilder httpsPort(int httpsPort) {
    this.httpsPort = httpsPort;
    return this;
  }

  /** Creates the server. */
  public Server build() {
    Preconditions.checkState(httpsPort != DISABLED, "HTTPS port required");

    Server server = new Server();

    addHttpConnector(server);
    addHttpsConnector(server);
    addHandlers(server);

    return server;
  }

  private void addHttpConnector(Server delegate) {
    if (this.httpPort == DISABLED) return;

    ServerConnector connector =
        new ServerConnector(delegate, new HttpConnectionFactory(getHttpConfiguration()));
    connector.setPort(this.httpPort);
    delegate.addConnector(connector);
  }

  private void addHttpsConnector(Server delegate) {
    SslContextFactory contextFactory = new SslContextFactory();
    contextFactory.setKeyStore(keyStore);
    SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(contextFactory,
        HTTP_1_1.toString());
    HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(getHttpConfiguration());
    ServerConnector connector = new ServerConnector(delegate, sslConnectionFactory,
        httpConnectionFactory);
    connector.setPort(this.httpsPort);
    delegate.addConnector(connector);
  }

  private void addHandlers(Server delegate) {
    HandlerList handlers = new HandlerList();

    // Redirects to HTTPS.
    handlers.addHandler(new SecuredRedirectHandler());

    ServletContextHandler context
        = new ServletContextHandler(handlers, "/", 0);
    context.addFilter(new FilterHolder(guiceFilter), "/*",
        EnumSet.of(javax.servlet.DispatcherType.REQUEST, javax.servlet.DispatcherType.ASYNC));
    context.addServlet(DefaultServlet.class, "/*");

    delegate.setHandler(handlers);
  }

  private HttpConfiguration getHttpConfiguration() {
    HttpConfiguration config = new HttpConfiguration();
    config.setSecureScheme("https");
    config.setSecurePort(this.httpsPort);
    config.addCustomizer(new SecureRequestCustomizer());
    return config;
  }
}
