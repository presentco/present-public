package present.server.model.group;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Query;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.GroupMembershipState;
import present.proto.MembershipRequest;
import present.server.KeysOnly;
import present.server.MoreObjectify;
import present.server.Uuids;
import present.server.model.BasePresentEntity;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.stream.Collectors.toList;

/**
 * A user's membership in a group.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class GroupMembership extends BasePresentEntity<GroupMembership> {

  private static final Logger logger = LoggerFactory.getLogger(GroupMembership.class);

  /** "[Member ID]:[Group ID]" */
  @Id public String id;

  /** Group member. */
  @Load(unless = KeysOnly.class) @Index public Ref<User> member;

  /** Joined group. */
  @Load(unless = KeysOnly.class) @Index public Ref<Group> group;

  /** Membership status. */
  @Index public GroupMembershipState state = GroupMembershipState.NONE;

  /** Member who set status. */
  public Ref<User> host;

  public User member() {
    return this.member.get();
  }

  public Group group() {
    return this.group.get();
  }

  public User host() {
    return MoreObjectify.get(this.host);
  }

  public static GroupMembership get(User member, Group group) {
    return load().id(idFor(member, group)).now();
  }

  public static GroupMembership getOrCreate(User member, Group group) {
    String id = idFor(member, group);
    GroupMembership request = ofy().load().type(GroupMembership.class).id(id).now();
    if (request == null) request = newInstance(member, group);
    return request;
  }

  public static GroupMembership newInstance(User member, Group group) {
    GroupMembership membership;
    membership = new GroupMembership();
    membership.id = idFor(member, group);
    membership.member = member.getRef();
    membership.group = group.getRef();
    return membership;
  }

  public static String idFor(User member, Group group) {
    return member.uuid + ":" + group.uuid();
  }

  public static Key<GroupMembership> keyFor(User member, Group group) {
    return Key.create(GroupMembership.class, idFor(member, group));
  }

  public static Key<User> toUser(Key<GroupMembership> key) {
    String id = key.getName();
    id = id.substring(0, Uuids.LENGTH);
    return User.keyFor(id);
  }

  public static Key<Group> toGroup(Key<GroupMembership> key) {
    String id = key.getName();
    id = id.substring(Uuids.LENGTH + 1);
    return Group.keyFor(id);
  }

  public static List<GroupMembership> requestsFor(Group group) {
    return query(group, GroupMembershipState.REQUESTED)
        .list()
        .stream()
        // Reverse chronological order.
        .sorted((a, b) -> Long.compare(b.updatedTime, a.updatedTime))
        .collect(toList());
  }

  public MembershipRequest toMembershipRequest() {
    return new MembershipRequest(member().toResponse(), updatedTime);
  }

  public static LoadType<GroupMembership> load() {
    return ofy().load().type(GroupMembership.class);
  }

  /** Finds group memberships for the given user. */
  public static Query<GroupMembership> query(User member) {
    return load().filter(Fields.member.getName(), member.getKey());
  }

  /** Finds group memberships for the given group in the given state. */
  public static Query<GroupMembership> query(Group group, GroupMembershipState state) {
    return load()
        .filter(Fields.group.getName(), group.getKey())
        .filter(Fields.state.getName(), state);
  }

  /**
   * Loads memberships for the given users in the given group. Creates entries for
   * all users whether they have membership in the datastore or not.
   */
  public static Map<User, GroupMembership> load(Group group, Iterable<User> users) {
    Iterable<Key<GroupMembership>> keys = Iterables.transform(users, u -> keyFor(u, group));
    Map<Key<GroupMembership>, GroupMembership> memberships = ofy().load().keys(keys);
    Map<User, GroupMembership> byUser = new HashMap<>();
    for (User user : users) {
      GroupMembership membership = memberships.get(keyFor(user, group));
      if (membership == null) membership = newInstance(user, group);
      byUser.put(user, membership);
    }
    return byUser;
  }

  public void changeState(GroupMembershipState to) {
    this.state = to;
    this.save();
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(GroupMembership.class)
        .add("id", id)
        .add("member", member)
        .add("group", group)
        .add("state", state)
        .add("host", host)
        .toString();
  }

  /** Creates a copy of this membership for the given user. */
  public GroupMembership copyFor(User member) {
    GroupMembership copy = new GroupMembership();
    copy.id = idFor(member, group());
    copy.group = this.group;
    copy.member = member.getRef();
    copy.state = this.state;
    copy.host = this.host;
    return copy;
  }

  public static class Fields {

    private Fields() {}

    public static Field member = get("member");
    public static Field group = get("group");
    public static Field state = get("state");

    private static Field get(String fieldName) {
      try {
        return GroupMembership.class.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override protected GroupMembership getThis() {
    return this;
  }

  @Override public int hashCode() {
    return id.hashCode();
  }

  @Override public boolean equals(Object other) {
    return other instanceof GroupMembership && id.equals(((GroupMembership) other).id);
  }
}
