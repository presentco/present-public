package present.server.facebook;

import javax.annotation.Nullable;

/**
 * Thrown when the Facebook API returns an error.
 *
 * @author Bob Lee (bob@present.co)
 */
public class FacebookException extends Exception {

  public final int httpCode;
  @Nullable public final Facebook.Error error;

  public FacebookException(int httpCode, Facebook.Error error) {
    super("Error returned by Facebook API. HTTP code: " + httpCode + ", Facebook error: " + error);
    this.httpCode = httpCode;
    this.error = error;
  }

  private static String toString(Facebook.Error error) {
    return error == null ? "null" : error.toString();
  }

  /** Returns true if we should ask the user to log in again. */
  public boolean requestLogin() {
    // If error is null, Facebook is probably having problems.
    return error != null && error.requestLogin();
  }
}
