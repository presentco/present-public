package present.server.tool;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import present.proto.ResolveUrlRequest;
import present.proto.ResolveUrlResponse;
import present.proto.UrlResolverService;
import present.server.log.RpcLog;
import present.server.model.activity.GroupReferral;
import present.server.model.activity.GroupReferrals;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.Client;
import present.server.model.user.User;

public class BackfillGroupReferrals {
  public static void main(String[] args) throws Exception {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      long now = System.currentTimeMillis();
      long start = now - TimeUnit.DAYS.toMillis(30);
      Iterable<RpcLog> rpcs = RpcLog.search(UrlResolverService.class, "resolveUrl", start, now);
      Pattern pattern = Pattern.compile("^https?://present.co/g/.*");
      for (RpcLog rpc : rpcs) {
        // Filter out group URLs.
        // Note: Some of the URLs got truncated in the logs because they had long query strings.
        ResolveUrlRequest request = (ResolveUrlRequest) rpc.argument();
        if (!pattern.matcher(request.url).matches()) continue;

        // Current user.
        Client client = Client.get(rpc.header().clientUuid);
        if (client == null || client.user == null) continue;
        User to = client.user.get();

        // Resolve the URL.
        ResolveUrlResponse response;
        try {
          response = (ResolveUrlResponse) rpc.replay();
        } catch (Exception e) {
          System.err.println(e.getMessage());
          continue;
        }

        // Record the referral.
        if (response.referrer != null && !to.uuid.equals(response.referrer.id)) {
          System.out.println(new Date(rpc.timestamp()));
          User from = User.get(response.referrer.id);
          Group group = Groups.findByUuid(response.group.uuid);
          GroupReferral referral = GroupReferrals.get(to, group);
          if (referral == null) {
            referral = new GroupReferral(from, to, group);
            referral.createdTime = rpc.timestamp();
            System.out.println(referral);
            referral.save().now();
          } else {
            if (!referral.referrers.contains(from.getRef())) {
              referral.referrers.add(from.getRef());
              System.out.println(referral);
              referral.save().now();
            }
          }
        }
      }
    });
  }
}
