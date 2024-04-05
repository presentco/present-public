package present.server.model;

import com.google.common.base.Preconditions;
import java.util.regex.Pattern;

/**
 * Support for US 5-digit zip codes
 *
 * @author Bob Lee (bob@present.co)
 */
public class ZipCode {

  private ZipCode() {}

  private static final Pattern zipPattern = Pattern.compile("^\\d{5}$");

  public static String validate(String zip) {
    Preconditions.checkArgument(zipPattern.matcher(zip).matches(), zip);
    return zip;
  }
}
