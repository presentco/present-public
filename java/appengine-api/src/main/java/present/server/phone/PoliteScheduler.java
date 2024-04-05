package present.server.phone;

import java.util.TimeZone;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Schedules tasks to be run between 9 am and 10 pm.
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */

public class PoliteScheduler {

  private static final LocalTime LATEST_TIME  = LocalTime.parse("20:00:00");
  private static final LocalTime EARLIEST_TIME = LocalTime.parse("08:00:00");

  /**
   * Determines if provided time is within polite hours.
   * @param time LocalTime representation of time
   * @return True if within polite hours, False otherwise.
   */
  public static boolean isPolite(LocalTime time) {
    return (time.compareTo(EARLIEST_TIME) >= 0) && (time.compareTo(LATEST_TIME) <= 0);
  }

  /**
   * Determines if provided time is within polite hours.
   * @param dateTime LocalDateTime representation of time
   * @return True if within polite hours, False otherwise.
   */
  public static boolean isPolite(LocalDateTime dateTime) {
    return isPolite(dateTime.toLocalTime());
  }

  /**
   * Determines what delay is required for task to run during polite hours. Adjusts date if required.
   * @param dateTime LocalDateTime representation of time.
   * @return Delay in milliseconds.
   */
  private static long politeDelay(LocalDateTime dateTime) {
    final LocalDate date = dateTime.toLocalDate();

    // if it's after 11 pm on the current day, delay until next day at 8 am
    LocalDateTime latest = LocalDateTime.of(date, LATEST_TIME);

    // if it's before 8 am on the current day, delay until 8 am that day
    LocalDateTime earliest = LocalDateTime.of(date, EARLIEST_TIME);

    if (dateTime.isAfter(latest)) {
      // delay until next morning
      earliest = earliest.plusDays(1L);
    }
    return ChronoUnit.MILLIS.between(dateTime, earliest);
  }

  /**
   * Calculates millisecond delay needed for a scheduled task to run during polite hours.
   * Takes into consideration what date task will be run and a provided delay.
   * Currently assumes PST as user's time zone.
   * @param localDateTime Date and Time task should be run.
   * @param atLeast How long after provided date and time task should be delayed.
   * @return Updated delay time in milliseconds.
   */
  public static long delayOf(LocalDateTime localDateTime, long atLeast) {
    // TODO: Add support for different users' timezones
    LocalDateTime futureDateTime = localDateTime.plus(atLeast, ChronoUnit.MILLIS);
    if (isPolite(futureDateTime.toLocalTime())) {
      return atLeast;
    }
    return atLeast + politeDelay(futureDateTime);
  }

  /**
   * Calculates millisecond delay needed for a scheduled task to run during polite hours.
   * Takes into consideration a provided delay. Currently assumes PST as user's time zone.
   * @param atLeast How long after current date and time in PST task should be delayed.
   * @return Updated delay time in milliseconds.
   */
  public static long delayOf(long atLeast) {
    // Assume timezone is PST (America/Los_Angeles) for now
    return delayOf(LocalDateTime.now(TimeZone.getTimeZone("America/Los_Angeles").toZoneId()),
        atLeast);
  }

}
