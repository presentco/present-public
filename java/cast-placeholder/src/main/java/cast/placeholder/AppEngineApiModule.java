package cast.placeholder;

import present.gcs.GcsDevelopmentServlet;
import cast.placeholder.proto.CastService;
import com.google.apphosting.utils.remoteapi.RemoteApiServlet;
import com.google.inject.servlet.ServletModule;
import com.googlecode.objectify.ObjectifyFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.objectify.ObjectifyService.register;

public class AppEngineApiModule extends ServletModule {

  private static final Logger logger = LoggerFactory.getLogger(AppEngineApiModule.class);

  @Override protected void configureServlets() {
    // Objectify
    filter("/*").through(ObjectifyFilter.class);
    bind(ObjectifyFilter.class).in(Singleton.class);

    // Present API
    bind(CastService.class).to(AppEngineCastService.class);
    serve("/api/*").with(AppEngineRpcServlet.class);

    // App Engine Remote API
    serve("/remote_api").with(new RemoteApiServlet());

    // GCS development servlet
    serve("/gcs/*").with(new GcsDevelopmentServlet());

    // Jersey servlet
    serve("/rest/*").with(GuiceContainer.class);

    // Jersey resources
    bind(Censor.class);
    bind(CastToSlack.class);

    // Register entity types
    register(Cast.class);
    register(Client.class);
    register(Flag.class);
  }
}
