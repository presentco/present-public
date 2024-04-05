package present.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates internal API calls.
 *
 * @author Bob Lee (bob@present.co)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Internal {

  /** Use in {@link present.proto.RequestHeader#authorizationKey}. */
  public static final String AUTHORIZATION_KEY = "xxx";
}
