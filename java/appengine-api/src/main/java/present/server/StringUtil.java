package present.server;

import org.apache.commons.lang3.StringUtils;

/**
 * @author pat@pat.net
 */
public class StringUtil {

  public static String EMOJI_HAND_WAVE = "\uD83D\uDC4B";

  public static String quote(String text) {
    return "“" + text + "”";
  }

  public static String elide(String text, int maxLen) {
    return StringUtils.abbreviate(text, maxLen);
  }
}
