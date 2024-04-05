package present.server.model.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.StringUtil;
import present.wire.rpc.core.ServerException;

/**
 * @author Pat Niemeyer (pat@pat.net)
 * Date: 8/24/17
 */
public class WelcomeMessageTemplate {
  private static final Logger logger = LoggerFactory.getLogger(WelcomeMessageTemplate.class);

  /**
   * Substitute the following strings:
   *
   *  TEMPLATE:SENDER_FIRST_NAME
   *  TEMPLATE:RECIPIENT_FIRST_NAME
   *  TEMPLATE:EMOJI_HAND_WAVE
   *
   * Throw an exception if any other uses of TEMPLATE appear in the body.
   */
  public static String welcomeMessageFromTo(User sender, User recipient, String body) {

    String s = body;
    s = s.replace("TEMPLATE:RECIPIENT_FIRST_NAME", recipient.firstName);
    s = s.replace("TEMPLATE:SENDER_FIRST_NAME", sender.firstName);
    s = s.replace("TEMPLATE:EMOJI_HAND_WAVE", StringUtil.EMOJI_HAND_WAVE);

    // Fail fast rather than allowing a user to see an unresolved template parameter.
    if (s.contains("TEMPLATE")) {
      logger.error("Template contains unknown replacement strings: {}", s);
      throw new ServerException("Template contains unknown replacement strings");
    }

    return s;
  }
}
