package present.server.model.activity;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Query;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import present.proto.EventResponse;
import present.server.model.PresentEntities;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Events {

  private static final int MAX_EVENTS = 250;

  public static Query<Event> query() { return ofy().load().type(Event.class); }
  public static Query<Event> active() { return PresentEntities.active(query()); }
  public static Iterable<Event> all() { return query().iterable(); }
  public static Stream<Event> stream() { return Streams.stream(all()); }

  public static Iterable<Event> getEventsForUser(
      User user, @Nullable Long startTime, @Nullable Long endTime) {
    Query<Event> query = Event.query().filter(Event.Fields.user.name(), user);
    if (startTime != null && endTime != null) {
      query = query
        .filter(Event.Fields.createdTimeIndex+" >=", startTime)
        .filter(Event.Fields.createdTimeIndex+" <=", endTime);
    }
    query = query.order("-"+Event.Fields.createdTimeIndex.name()); // order descending
    query = query.limit(MAX_EVENTS); // limit number of events returned
    return query.chunkAll().list().stream()
        // Early events didn't have an initiator.
        .filter(e -> e.initiator != null && user.canSee(e.initiator.get()))
        .collect(Collectors.toList());
  }

  /**
   * Render the events to EventResponses, optionally including all referenced entity content.
   */
  public static Iterable<EventResponse> toResponse(Iterable<Event> events, final boolean full) {
    Iterable<EventResponse> eventResponses = Streams.stream(events)
        .map(Event::toFullResponse)
        .collect(Collectors.toList());
    // TODO: Remove any null responses corresponding to deleted event targets.
    return Iterables.filter(eventResponses, Predicates.notNull());
  }
}
