package present.server;

import com.google.common.base.Stopwatch;
import com.google.common.geometry.S2LatLng;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.group.WelcomeGroup;
import present.server.model.util.Coordinates;

/**
 * Time utilities.
 *
 * @author Bob Lee (bob@present.co)
 */
public class Time {

  private static final Logger logger = LoggerFactory.getLogger(Time.class);

  /** One day in ms. */
  public static final long DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1);

  /** One week in ms. */
  public static final long WEEK_IN_MILLIS = TimeUnit.DAYS.toMillis(7);

  /** 30 days in ms. */
  public static final long MONTH_IN_MILLIS = TimeUnit.DAYS.toMillis(30);

  private Time() {}

  /** Resets the given stopwatch and returns the previously measured time. */
  public static String restart(Stopwatch sw) {
    try {
      return sw.toString();
    } finally {
      sw.reset().start();
    }
  }

  /** Returns the epoch time in 30-day months. */
  public static long epochMonth() {
    return System.currentTimeMillis() / MONTH_IN_MILLIS;
  }

  public static long epochWeek() { return System.currentTimeMillis() / WEEK_IN_MILLIS; }

  /** Returns the epoch time in 24-hour days. */
  public static long epochDay() {
    return System.currentTimeMillis() / DAY_IN_MILLIS;
  }

  /** Format a date as e.g. 2017-12-25 */
  public static String format_yyyy_MM_dd(long time) {
    return format_yyyy_MM_dd(new Date(time));
  }

  /** Format a date as e.g. 2017-12-25 */
  public static String format_yyyy_MM_dd(Date date) {
    return new SimpleDateFormat("yyyy-MM-dd").format(date);
  }

  /** Decorates TemporalUnit. */
  private enum Unit {
    // In increasing order:
    MILLIS(ChronoUnit.MILLIS, "ms"),
    SECONDS(ChronoUnit.SECONDS, "s"),
    MINUTES(ChronoUnit.MINUTES, "m"),
    HOURS(ChronoUnit.HOURS, "h"),
    DAYS(ChronoUnit.DAYS, "d"),
    WEEKS(ChronoUnit.WEEKS, "w"),
    MONTHS(ChronoUnit.MONTHS, "mo"),
    YEARS(ChronoUnit.YEARS, "y");

    private final TemporalUnit temporalUnit;
    private final String abbreviation;

    Unit(TemporalUnit temporalUnit, String abbreviation) {
      this.temporalUnit = temporalUnit;
      this.abbreviation = abbreviation;
    }

    private long millis() {
      return temporalUnit.getDuration().toMillis();
    }
  }

  /**
   * Returns a user friendly description of the given duration.
   */
  public static String describeDuration(long duration, TimeUnit unit) {
    long millis = unit.toMillis(duration);
    Unit best = Unit.MILLIS;
    for (Unit next : Unit.values()) {
      if (millis < next.millis()) break;
      best = next;
    }
    long scaled = millis / best.millis();
    return scaled + best.abbreviation;
  }

  private static ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

  public static ZoneId zoneFor(S2LatLng location) {
    if (location == null) return PACIFIC;
    String id = TimezoneMapper.latLngToTimezoneString(location.latDegrees(), location.lngDegrees());
    return ZoneId.of(id);
  }

  public static ZonedDateTime timeAt(S2LatLng location) {
    Instant now = Instant.now();
    return ZonedDateTime.ofInstant(now, zoneFor(location));
  }

  /** Returns true if it's between 8am and 10pm at the given location. */
  public static boolean isDaytime(S2LatLng location) {
    ZonedDateTime now = timeAt(location);
    int hour = now.get(ChronoField.CLOCK_HOUR_OF_DAY);
    return hour >= 8 && hour <= 22;
  }
}
