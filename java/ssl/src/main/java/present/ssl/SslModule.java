package present.ssl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.security.KeyStore;
import javax.inject.Singleton;

/**
 * SSL dependencies.
 *
 * @author Bob Lee
 */
public class SslModule extends AbstractModule {

  @Override protected void configure() {}

  /** Loads our private key and certificate. */
  @Provides @Singleton KeyStore keyStore() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(getClass().getResourceAsStream("/present.co.pkcs12"), new char[0]);
    return keyStore;
  }
}
