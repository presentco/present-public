package cast.placeholder;

import present.wire.rpc.core.ClientException;
import java.util.regex.Pattern;

/**
 * @author Bob Lee (bob@present.co)
 */
public class Uuids {

  private static final Pattern PATTERN = Pattern.compile(
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", Pattern.CASE_INSENSITIVE);

  /** Verifies that the given string is formatted as a UUID. */
  public static void validate(String uuid) {
    if (!PATTERN.matcher(uuid).matches()) throw new ClientException("Invalid UUID: " + uuid);
  }
}
