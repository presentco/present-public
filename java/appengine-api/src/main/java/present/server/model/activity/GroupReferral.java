package present.server.model.activity;

import com.google.common.base.MoreObjects;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import present.server.KeysOnly;
import present.server.model.BasePresentEntity;
import present.server.model.group.Group;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Gabrielle Taylor {gabrielle@present.co}
 */
@Entity @Cache public class GroupReferral extends BasePresentEntity<GroupReferral> {

  @Id public String id;

  @Load(unless = KeysOnly.class) @Index public Ref<User> to;

  @Load(unless = KeysOnly.class) @Index @Deprecated public Ref<User> from;

  @Load(unless = KeysOnly.class) @Index public List<Ref<User>> referrers = new ArrayList<>();

  @Load(unless = KeysOnly.class) @Index public Ref<Group> group;

  private GroupReferral() {}

  public GroupReferral(User from, User to, Group group) {
    this.referrers.add(from.getRef());
    this.to = to.getRef();
    this.group = group.getRef();
    this.id = GroupReferrals.id(to, group);
  }

  public Key<GroupReferral> keyFor() {
    return Key.create(GroupReferral.class, id);
  }

  @Override public Ref<GroupReferral> getRef() {
    return super.getRef();
  }

  @Override protected GroupReferral getThis() {
    return this;
  }

  public String id() { return this.id; }

  public User to() { return to.get(); }

  @Deprecated public User from() { return from.get(); }

  public List<User> referrers() {
    return this.referrers.stream()
        .filter(Objects::nonNull)
        .map(Ref::get)
        .collect(Collectors.toList());
  }

  public Group group() {
    return group.get();
  }

  public GroupReferral addFrom(User from) {
    if (!this.referrers.contains(from.getRef())) {
      this.referrers.add(from.getRef());
    }
    return this;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("referrers", (this.referrers == null) ? null : referrers().stream().map(User::fullName).collect(Collectors.toList()))
        .add("to", (this.to == null) ? null : to().fullName())
        .add("group", (this.group == null) ? null : group().title)
        .toString();
  }

  public enum Fields {id, to, from, referrers, group, saved, saveTimestamp}
}
