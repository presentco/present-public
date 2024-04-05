package present.server;

import co.present.unblock.Unblock;
import co.present.unblock.UnblockFilter;
import com.google.apphosting.utils.remoteapi.RemoteApiServlet;
import com.google.common.base.Stopwatch;
import com.google.inject.Guice;
import com.google.inject.servlet.ServletModule;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.ObjectifyService;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import java.io.IOException;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.gcs.GcsDevelopmentServlet;
import present.phone.SmsGateway;
import present.phone.TwilioGateway;
import present.server.email.SendSummaries;
import present.server.environment.Environment;
import present.server.model.PresentEntities;
import present.server.model.console.users.CreateVerificationRequest;
import present.server.model.console.users.UsersCsv;
import present.server.model.console.users.UsersResource;
import present.server.model.console.whitelist.WhitelistResource;
import present.server.model.console.whitelist.geofence.WhitelistGeofenceService;
import present.server.notification.Notifications;
import present.server.phone.SmsListener;

public class AppEngineApiModule extends ServletModule {

  private static final Logger logger = LoggerFactory.getLogger(AppEngineApiModule.class);

  @Override protected void configureServlets() {
    Stopwatch stopwatch = Stopwatch.createStarted();

    // Don't install Unblock locally. com.google.appengine.tools.cloudstorage.dev.LocalRawGcsService
    // depends on the the actual API delegate.
    if (!Environment.isDevelopment()) Unblock.install();

    bind(SmsGateway.class).to(TwilioGateway.class);

    // Reset datastore call counters.
    filter("/*").through(new UnblockFilter());

    // Enables access from the wen app.
    filter("/*").through(CorsFilter.class);
    bind(CorsFilter.class).in(Singleton.class);

    // Objectify
    filter("/*").through(ObjectifyFilter.class);
    bind(ObjectifyFilter.class).in(Singleton.class);

    // Present API
    serve("/api/*").with(AppEngineRpcServlet.class);

    // App Engine Remote API
    serve("/remote_api").with(new RemoteApiServlet());

    // GCS development servlet
    serve("/gcs/*").with(new GcsDevelopmentServlet());

    // Jersey servlet
    serve("/rest/*").with(GuiceContainer.class);

    // Jersey resources
    bind(UsersResource.class);
    bind(WhitelistResource.class);
    bind(WhitelistGeofenceService.class);
    bind(BackupService.class);
    bind(SendSummaries.class);
    bind(BigQueryService.class);
    bind(UsersCsv.class);
    bind(CreateVerificationRequest.class);
    bind(SmsListener.class);

    // Invoked by Appengine Cron
    serve("/hourly", "/nightly", "/warmdb", "/test", "/testMessage").with(new CronServlet());

    ObjectifyService.setFactory(new PresentObjectifyFactory());

    PresentEntities.registerAll();

    requestStaticInjection(Notifications.class);
    logger.info("Deployment environment: " + Environment.current());
    logger.info("Configured in {}.", stopwatch);
  }

  public static void main(String[] args) throws IOException {
    // Pause and wait for a performance profiler to connect. Useful for optimizing startup time.
    System.in.read();
    Guice.createInjector(new AppEngineApiModule());
  }
}
