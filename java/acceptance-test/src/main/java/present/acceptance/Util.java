package present.acceptance;

/**
 * @author pat@pat.net
 */
public class Util {

  public static long sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return millis;
  }
}
