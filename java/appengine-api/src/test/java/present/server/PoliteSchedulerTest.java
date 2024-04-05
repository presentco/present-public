package present.server;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import present.server.phone.PoliteScheduler;

import static org.junit.Assert.*;

/**
 * Tests polite scheduling of tasks.
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */
public class PoliteSchedulerTest {

  private static final Logger logger = LoggerFactory.getLogger(PoliteSchedulerTest.class);

  @Test public void testPoliteScheduler() throws IOException, InterruptedException {

    // Sample dates and times
    LocalDateTime elevenFifteenPm = LocalDateTime.parse("2017-12-03T21:15:30");
    LocalDateTime threeTwentyAm = LocalDateTime.parse("2018-02-27T03:20:11");
    LocalDateTime elevenOhSixAm = LocalDateTime.parse("2017-04-30T11:06:32");
    LocalDateTime currentTimePst =
        LocalDateTime.now(TimeZone.getTimeZone("America/Los_Angeles").toZoneId());

    // Check that 11:15 pm and 3:20 am are not considered within polite hours.
    assertFalse(PoliteScheduler.isPolite(elevenFifteenPm));
    assertFalse(PoliteScheduler.isPolite(threeTwentyAm));
    // Check that 11:06 am is considered within polite hours.
    assertTrue(PoliteScheduler.isPolite(elevenOhSixAm));

    // Check that the polite scheduler correctly determines a delay that will make test times
    // within polite hours, with no additional delay provided.
    assertTrue(PoliteScheduler.isPolite(
        elevenFifteenPm.plus(PoliteScheduler.delayOf(elevenFifteenPm, 0L), ChronoUnit.MILLIS)));
    assertTrue(PoliteScheduler.isPolite(
        threeTwentyAm.plus(PoliteScheduler.delayOf(threeTwentyAm, 0L), ChronoUnit.MILLIS)));
    assertTrue(PoliteScheduler.isPolite(
        elevenOhSixAm.plus(PoliteScheduler.delayOf(elevenOhSixAm, 0L), ChronoUnit.MILLIS)));
    assertTrue(PoliteScheduler.isPolite(
        currentTimePst.plus(PoliteScheduler.delayOf(currentTimePst, 0L), ChronoUnit.MILLIS)));

    // Convert test values to milliseconds
    long sixDays = TimeUnit.MILLISECONDS.convert(6L, TimeUnit.DAYS);
    long twentySevenHours = TimeUnit.MILLISECONDS.convert(27L, TimeUnit.HOURS);
    long ninetyMinutes = TimeUnit.MILLISECONDS.convert(90L, TimeUnit.MINUTES);
    long fiftySevenSeconds = TimeUnit.MILLISECONDS.convert(57L, TimeUnit.SECONDS);

    // Check that the polite scheduler correctly determines a delay that will make test times
    // within polite hours, with an additional delay provided.
    assertTrue(PoliteScheduler.isPolite(
        elevenFifteenPm.plus(PoliteScheduler.delayOf(elevenFifteenPm, sixDays),
            ChronoUnit.MILLIS)));
    assertTrue(PoliteScheduler.isPolite(
        threeTwentyAm.plus(PoliteScheduler.delayOf(threeTwentyAm, twentySevenHours),
            ChronoUnit.MILLIS)));
    assertTrue(PoliteScheduler.isPolite(
        elevenOhSixAm.plus(PoliteScheduler.delayOf(elevenOhSixAm, ninetyMinutes),
            ChronoUnit.MILLIS)));
    assertTrue(PoliteScheduler.isPolite(
        currentTimePst.plus(PoliteScheduler.delayOf(currentTimePst, fiftySevenSeconds),
            ChronoUnit.MILLIS)));
  }
}
