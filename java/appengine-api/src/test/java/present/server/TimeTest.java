package present.server;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeTest {

  @Test public void describeDuration() {
    assertEquals("0ms", Time.describeDuration(0, TimeUnit.MILLISECONDS));
    assertEquals("1s", Time.describeDuration(1001, TimeUnit.MILLISECONDS));
    assertEquals("2m", Time.describeDuration(121, TimeUnit.SECONDS));
    assertEquals("3h", Time.describeDuration(180, TimeUnit.MINUTES));
    assertEquals("4d", Time.describeDuration(96, TimeUnit.HOURS));
    assertEquals("3w", Time.describeDuration(22, TimeUnit.DAYS));
    assertEquals("6mo", Time.describeDuration(185, TimeUnit.DAYS));
    assertEquals("7y", Time.describeDuration(2600, TimeUnit.DAYS));
  }
}
