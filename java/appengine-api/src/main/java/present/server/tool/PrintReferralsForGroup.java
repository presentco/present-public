package present.server.tool;

import com.googlecode.objectify.Ref;
import java.util.Date;
import java.util.List;
import present.server.model.activity.GroupReferral;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.group.JoinedGroups;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class PrintReferralsForGroup {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      Group group = Groups.findByUuid("748810d8-72d9-4eba-8ebf-62d650429cba");
      List<GroupReferral> referrals = ofy().load()
          .type(GroupReferral.class)
          .filter("group", group)
          .list();
      System.out.println(group.title);
      for (GroupReferral referral : referrals) {
        Ref<User> from = referral.from;
        Ref<User> to = referral.to;
        JoinedGroups joinedGroups = JoinedGroups.getOrCreate(to.get());
        System.out.println(new Date(referral.createdTime));
        boolean saved = joinedGroups.groups.contains(group.getRef());
        System.out.println(from.get().fullName() + " -> " + to.get().fullName()
            + " (" + saved + ")");
      }
    });
  }
}
