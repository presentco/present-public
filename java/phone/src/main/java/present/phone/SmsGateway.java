package present.phone;

/**
 * Sends text messages.
 *
 * @author Bob Lee (bob@present.co)
 */
public interface SmsGateway {

  /** Sends a text message to the given number. */
  void sms(String phoneNumber, String message);
}
