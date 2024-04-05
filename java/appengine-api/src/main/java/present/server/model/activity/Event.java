package present.server.model.activity;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.cmd.Query;
import java.util.Date;
import javax.annotation.Nullable;
import present.proto.ActivityType;
import present.proto.EventResponse;
import present.proto.ReferenceResponse;
import present.server.KeysOnly;
import present.server.Uuids;
import present.server.model.BasePresentEntity;
import present.server.model.PresentEntity;
import present.server.model.content.Content;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.model.activity.ReferenceResponses.toFullReferenceResponse;
import static present.server.model.activity.ReferenceResponses.toShortReferenceResponse;

/**
 * Describe events pertaining to a user.
 * e.g. Events in a user's activity stream.
 *
 * @author Pat Niemeyer (pat@pat.net)
 */
@Entity @Cache public class Event extends BasePresentEntity<Event> implements Cloneable {
  // Field names that may be used in filter queries as strings
  public enum Fields { uuid, user, createdTimeIndex, defaultTarget, initiator }

  // Present id for this entity
  @Id public String uuid = Uuids.newUuid();

  // An indexed copy of the entity create time.
  @Index public long createdTimeIndex = createdTime;

  // User who initiated this event.
  @Load(unless = KeysOnly.class) @Index public Ref<User> initiator;

  // TODO: Make this an array and write one Event for N users.
  // The user to whom this event belongs
  @Load(unless = KeysOnly.class) @Index public Ref<User> user;

  // The unique activity type
  public ActivityType type;

  // A image to be displayed with a summary
  @Load(unless = KeysOnly.class) public Ref<Content> icon;

  // Summary text suitable for presentation to the user
  public String summary = ""; // todo: localize

  // A default entity for display when the user interacts with this event (e.g. notification)
  @Load(unless = KeysOnly.class) @Index public Ref<? extends PresentEntity> defaultTarget;

  public Event() {}

  public Event(
      User initiator,
      Ref<User> to,
      ActivityType type,
      Ref<Content> icon,
      String summary,
      Ref<? extends PresentEntity> defaultTarget
    ) {
    this.initiator = Ref.create(initiator);
    this.user = to;
    this.type = type;
    this.icon = icon;
    this.summary = summary;
    this.defaultTarget = defaultTarget;
  }

  public Event(
      ActivityType type,
      String summary,
      Ref<? extends PresentEntity<?>> defaultTarget
  ) {
    this.type = type;
    this.summary = summary;
    this.defaultTarget = defaultTarget;
  }

  @OnSave public void validate() {
    Preconditions.checkState(this.initiator != null);
    Preconditions.checkState(this.user != null);
  }

  // Entity Management

  public static Query<Event> query() {
    return ofy().load().type(Event.class);
  }

  public static Event get(Key<Event> key) {
    return ofy().load().key(key).now();
  }

  public static Key<Event> keyFor(String uuid) {
    return Key.create(Event.class, uuid);
  }

  // Proto response

  /**
   * Render reference responses including entity ids only.
   */
  public EventResponse toShortResponse() {

    // Render to responses
    ReferenceResponse defaultTargetResponse = toShortReferenceResponse(defaultTarget);

    String iconUrl = icon != null ? icon.get().url() : null;

    return new EventResponse(uuid, type, createdTime, iconUrl, summary, defaultTargetResponse);
  }

  /**
   * Render the full response including referenced entities.
   */
  // TODO: Currenty this may return a null EventResponse for the Event if the target entity
  // TODO: has been deleted.
  public @Nullable EventResponse toFullResponse() {
    // Render to responses
    PresentEntity entity = defaultTarget.get();

    // TODO: Return null in the case of a deleted target entity.
    // TODO: How should we handle this?
    // TODO: We could make the defaultTarget in the response optional.
    // TODO: Or we could sub a pre-defined "deleted entity" of the correct type?
    // TODO: Note that the problem applies in the short response version above as well,
    // TODO: but we will not detect it there unless the client tries to fetch the entity by the uuid.
    if (entity == null) { return null; }

    ReferenceResponse defaultTargetResponse = toFullReferenceResponse(entity);

    String iconUrl = icon != null ? icon.get().url() : null;
    return new EventResponse(uuid, type, createdTime, iconUrl, summary, defaultTargetResponse);
  }

  public String toShortString() {
    return "Event{" + "createdTimeIndex=" + new Date(createdTimeIndex) + ", summary='" + summary + '\'' + '}';
  }

  @Override public String toString() {
    return "Event{"
        + "uuid='"
        + uuid
        + '\''
        + ", createdTimeIndex="
        + createdTimeIndex
        + ", user="
        + user
        + ", type="
        + type
        + ", icon="
        + icon
        + ", summary='"
        + summary
        + '\''
        + ", defaultTarget="
        + defaultTarget
        + '}';
  }

  @Override protected Event getThis() {
    return this;
  }

  @Override public Event clone() {
    try {
      return (Event) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}


