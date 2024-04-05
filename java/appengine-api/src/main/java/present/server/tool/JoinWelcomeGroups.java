package present.server.tool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.googlecode.objectify.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import present.proto.GroupMembershipState;
import present.server.model.comment.GroupView;
import present.server.model.group.Group;
import present.server.model.group.GroupMembership;
import present.server.model.group.WelcomeGroup;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

public class JoinWelcomeGroups {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      List<User> allMembers = Users.members();

      Multimap<Group, User> byGroup = HashMultimap.create();

      for (User user : allMembers) {
        if (user.signupLocation == null) continue;
        Key<Group> groupKey = WelcomeGroup.nearestTo(user.signupLocation);
        Group group = ofy().load().key(groupKey).now();
        byGroup.put(group, user);
      }

      List<Key<GroupMembership>> gmKeys = new ArrayList<>();
      byGroup.asMap().forEach((group, groupMembers) -> {
        for (User groupMember : groupMembers) {
          gmKeys.add(GroupMembership.keyFor(groupMember, group));
        }
      });

      Map<Key<GroupMembership>, GroupMembership> memberships = ofy().load().keys(gmKeys);

      List<GroupView> newViews = new ArrayList<>();
      List<GroupMembership> newMemberships = new ArrayList<>();
      Multiset<Group> newMembers = HashMultiset.create();

      byGroup.asMap().forEach((group, groupMembers) -> {
        for (User groupMember : groupMembers) {
          Key<GroupMembership> key = GroupMembership.keyFor(groupMember, group);
          if (!memberships.containsKey(key)) {
            newViews.add(new GroupView(groupMember, group).mute());
            GroupMembership membership = GroupMembership.newInstance(groupMember, group);
            membership.state = GroupMembershipState.ACTIVE;
            newMemberships.add(membership);
            newMembers.add(group);
          }
        }
      });

      System.out.println("New views: " + newViews.size());
      ofy().save().entities(newViews);

      System.out.println("New memberships: " + newMemberships.size());
      ofy().save().entities(newMemberships);

      newMembers.forEachEntry((g, size) -> {
        System.out.println(g.title + ": " + size);
        g.inTransaction(g2 -> {
          g2.memberCount += size;
        });
      });
    });
  }
}
