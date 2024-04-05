package present.server.tool;

import java.util.List;
import present.proto.GroupReferralResponse;
import present.proto.GroupService;
import present.server.Protos;

/**
 * @author Bob Lee (bob@present.co)
 */
public class CountReferrals {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      GroupService gs = StagingUsers.janete().rpcClient(GroupService.class);
      System.out.println(gs.countGroupReferrals(Protos.EMPTY).membersReferred);
      List<GroupReferralResponse> referrals = gs.getGroupReferrals(Protos.EMPTY).referrals;
      System.out.println(referrals);
      for (GroupReferralResponse referral : referrals) {
        System.out.println(referral);
      }
    });
  }
}
