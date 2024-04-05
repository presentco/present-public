package present.live.server;

import present.jetty.ServerBuilder;
import present.proto.LiveService;
import present.ssl.SslModule;
import com.google.inject.servlet.ServletModule;

public class LiveModule extends ServletModule {

  @Override protected void configureServlets() {
    install(new SslModule());
    requireBinding(ServerBuilder.class);
    bind(LiveService.class).to(LiveServiceImpl.class);

    // TODO: Serve these off two different ports?
    serve(LiveServer.WEB_SOCKET_PATH).with(LiveCommentsServlet.class);
    serve("/api", "/api/*").with(LiveServiceServlet.class);
  }
}
