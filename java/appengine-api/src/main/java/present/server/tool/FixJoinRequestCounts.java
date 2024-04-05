package present.server.tool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import present.proto.GroupMembershipState;
import present.server.model.group.Group;
import present.server.model.group.GroupMembership;
import present.server.model.group.Groups;
import present.server.model.user.Friendship;
import present.server.model.user.User;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

public class FixJoinRequestCounts {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      List<GroupMembership> allRequests = GroupMembership.load()
          .filter("state", GroupMembershipState.REQUESTED)
          .list();
      Multimap<Group, GroupMembership> byGroup
          = Multimaps.index(allRequests, GroupMembership::group);
      for (Group group : Groups.all()) {
        Collection<GroupMembership> requests = byGroup.get(group);
        System.out.println(group.title + ": ");
        System.out.print(group.joinRequests + " -> " + requests.size());
        if (requests.size() == group.joinRequests) {
          System.out.println();
          continue;
        }
        group.inTransaction(g -> {
          g.joinRequests = requests.size();
          return true;
        });
        System.out.println(" *");
      }
    });
  }
}
