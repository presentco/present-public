package present.server.tool.migrations;

import com.googlecode.objectify.Ref;
import java.util.ArrayList;
import java.util.List;
import present.proto.GroupMembershipState;
import present.server.KeysOnly;
import present.server.model.group.Group;
import present.server.model.group.GroupMembership;
import present.server.model.group.JoinedGroups;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Mugrates from JoinedGroups to GroupMembership.
 *
 * @author Bob Lee (bob@present.co)
 */
public class MigrateJoinedGroups {

  public static void main(String[] args) {
    against(STAGING_SERVER, () -> {
      List<JoinedGroups> list = ofy().load().type(JoinedGroups.class).list();
      List<GroupMembership> memberships = new ArrayList<>();
      for (JoinedGroups joinedGroups : list) {
        for (Ref<Group> ref : joinedGroups.groups) {
          Group group = ref.get();
          if (group == null) {
            // Group was hard deleted.
            continue;
          }

          GroupMembership membership = new GroupMembership();
          membership.id = joinedGroups.user.getKey().getName() + ":" + group.uuid();
          membership.member = joinedGroups.user;
          membership.group = ref;
          membership.state = GroupMembershipState.ACTIVE;
          memberships.add(membership);
          System.out.print(".");
        }
      }
      ofy().save().entities(memberships).now();
      System.out.println();
      System.out.println("Saved " + memberships.size() + " memberships.");
    });
  }
}
