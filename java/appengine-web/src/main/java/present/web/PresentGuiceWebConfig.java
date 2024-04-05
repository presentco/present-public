package present.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

/**
 * @author Bob Lee (bob@present.co)
 */
public class PresentGuiceWebConfig extends GuiceServletContextListener {

  @Override protected Injector getInjector() {
    return Guice.createInjector(new AppEngineWebModule());
  }
}
