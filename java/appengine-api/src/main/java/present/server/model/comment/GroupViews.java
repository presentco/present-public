package present.server.model.comment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.googlecode.objectify.Key;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import present.server.model.group.Group;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class GroupViews {

  /** Loads views for the given user and groups. The returned map is loaded on demand. */
  public static Map<Group, GroupView> viewsFor(User user, Iterable<Group> groups) {
    // We have some null saved groups.
    Set<Group> filtered = Streams.stream(groups).filter(Objects::nonNull).collect(Collectors.toSet());
    Key<User> userKey = user.getKey();
    Iterable<Key<GroupView>> keys = Iterables.transform(filtered,
        container -> container.viewKeyFor(userKey));
    // Don't touch this map in this method. It'll block!
    Map<Key<GroupView>, GroupView> viewsByKey = ofy().load().keys(keys);
    return Maps.asMap(filtered, group -> {
      GroupView view = viewsByKey.get(group.viewKeyFor(userKey));
      return view == null ? new GroupView(user, group) : view;
    });
  }

  /** Loads views for the given users and containers. The returned map is loaded on demand. */
  public static Map<Key<User>, GroupView> viewsFor(Group group, Iterable<Key<User>> users) {
    Iterable<Key<GroupView>> viewKeys = Iterables.transform(users, group::viewKeyFor);
    // Don't touch this map in this method. It'll block!
    Map<Key<GroupView>, GroupView> views = ofy().load().keys(viewKeys);
    Set<Key<User>> userKeys = Sets.newHashSet(users);
    return Maps.asMap(userKeys, userKey -> views.get(group.viewKeyFor(userKey)));
  }
}
