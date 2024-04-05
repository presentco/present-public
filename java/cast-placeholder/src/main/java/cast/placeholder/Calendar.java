package cast.placeholder;

/**
 * @author Bob Lee (bob@present.co)
 */
public class Calendar {

  private Calendar() {}

  /** Milliseconds in a day. */
  public static final long DAY_MS = 24 * 60 * 60 * 1000;

  /** Returns the current day in days since the Unix epoch. */
  public static long today() {
    return System.currentTimeMillis() / DAY_MS;
  }
}
