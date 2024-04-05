package present.server.tool;

import com.googlecode.objectify.Ref;
import java.io.IOException;
import java.util.List;
import present.server.model.activity.GroupReferral;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class BackfillReferrers {
  public static void main(String[] args) throws IOException {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      System.out.println("Backfilling referrers for all referrals...");

      List<GroupReferral> referrals = ofy().load().type(GroupReferral.class).list();

      for (GroupReferral r : referrals) {
        ofy().transact(() -> {
          r.reload();
          System.out.println("#" + referrals.indexOf(r) + " Referral:" + r);
          if (!r.referrers.contains(r.from)) {
            r.referrers.add(r.from);
            r.save();
            System.out.println("\tReferrers: " + r.referrers());
          }
        });
      }
    });
  }
}
