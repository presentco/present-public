package present.server.model.group;

import com.google.common.base.MoreObjects;
import com.googlecode.objectify.annotation.AlsoLoad;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;
import present.wire.rpc.core.ClientException;

/**
 * Class used to represent a time and date for an event belonging to circle.
 *
 * @author Gabrielle A. Taylor {gabrielle@present.co}
 */
public class Schedule {

  // Timestamp representing time of event in milliseconds
  public long startTime;

  // Timestamp representing the ending time of event in milliseconds or zero if not specified.
  @AlsoLoad("duration") public long endTime;

  public Schedule() {}

  public Schedule(long startTime, long endTime) {
    if (endTime < startTime) {
      throw new IllegalArgumentException("End time can not be before start time.");
    }
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public Schedule(@Nullable present.proto.Schedule proto) {
    // If an end time was provided, check that it is valid.
    if (proto.endTime != null && proto.endTime < proto.startTime) {
        throw new ClientException("End time can not be before start time.");
    }
    this.startTime = proto.startTime;
    this.endTime = (proto.endTime == null ? proto.startTime : proto.endTime);
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("startTime", startTime)
        .add("endTime", endTime)
        .add("Date string", dateString())
        .toString();
  }

  public String dateString() {
    DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");
    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("h:mm a");

    LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
    LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault());

    String timeString = start.format(dateTimeFormat);

    if (hasEndTime()) {
      if (start.toLocalDate().equals(end.toLocalDate())) {
        timeString = timeString + "-" + end.format(timeFormat);
      } else {
        timeString = timeString + "-" + end.format(dateTimeFormat);
      }
    }
    return timeString;
  }

  public boolean hasEndTime() {
    return this.startTime != this.endTime;
  }

  public present.proto.Schedule toProto() {
    return new present.proto.Schedule(startTime, this.hasEndTime() ? endTime : null);
  }
}
