package present.phone;

import present.wire.rpc.core.ClientException;
import java.util.regex.Pattern;

/**
 * Phone number utilities.
 *
 * @author Bob Lee (bob@present.co)
 */
public class PhoneNumbers {

  /** We currently support only US numbers. */
  private static final Pattern US_PATTERN = Pattern.compile("^\\+?1\\d{10}$");

  /**
   * Verifies that the given number is a valid U.S. number in E.164 format. Returns the
   * phone number without a + prefix.
   */
  public static String validateUsPhone(String phoneNumber) {
    if (!US_PATTERN.matcher(phoneNumber).matches()) {
      throw new ClientException("Invalid phone: " + phoneNumber);
    }
    return phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;
  }

  private static final Pattern E164_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

  /**
   * Verifies that the given number follows the E.164 format. Returns the
   * phone number without a + prefix.
   */
  public static String validateE164(String phoneNumber) {
    if (!E164_PATTERN.matcher(phoneNumber).matches()) {
      throw new ClientException("Invalid phone: " + phoneNumber);
    }
    return phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;
  }

  public static boolean isTestNumber(String phone) {
    return phone.substring(4, 7).equals("555");
  }

  public static void main(String[] args) {
    System.out.println(isTestNumber("1314"));
    System.out.println(isTestNumber("1314"));
  }
}
