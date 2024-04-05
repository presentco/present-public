package present.server;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.ActivityService;
import present.proto.ActivityType;
import present.proto.EventResponse;
import present.proto.PastActivityRequest;
import present.proto.PastActivityResponse;
import present.proto.Platform;
import present.server.model.activity.Event;
import present.server.model.activity.Events;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.User;
import present.server.model.user.Users;

import static present.proto.ActivityType.*;

/**
 * App Engine implementation of ActivityService.
 *
 * @author Pat Niemer (pat@present.co)
 */
public class AppEngineActivityService implements ActivityService {

  private static final Set<ActivityType> ORIGINAL_TYPES = EnumSet.of(
      UNKNOWN,
      FRIEND_JOINED_PRESENT,
      USER_SENT_MESSAGE,
      USER_COMMENTED_ON_GROUP,
      USER_JOINED_GROUP,
      USER_INVITED_TO_GROUP
  );

  @Override public PastActivityResponse getPastActivity(PastActivityRequest request ) throws IOException {
    Iterable<Event> events = Events.getEventsForUser(Users.current(), request.startTime, request.endTime);

    // If true we to render the event responses with all related entity data included.
    // If false the event responses include only the ids of related entities.
    boolean fullContent = true;

    Iterable<EventResponse> elements = Events.toResponse(events, fullContent);

    // Older Android clients can't handle unrecognized enums.
    Client client = Clients.current();
    if (client.platform() == Platform.ANDROID && client.androidVersion() < 141) {
      elements = Iterables.filter(Iterables.transform(elements, e -> {
        if (!ORIGINAL_TYPES.contains(e.type)) return null;
        return e;
      }), Objects::nonNull);
    }

    return new PastActivityResponse(Lists.newArrayList(elements));
  }
}

