package present.server.tool;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.List;
import present.server.model.user.Friendship;
import present.server.model.user.User;
import present.server.model.user.Users;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

public class FixIncomingFriendRequestCounts {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      List<Friendship> friendships = Friendship.allRequests();
      Multimap<User, Friendship> byRequestee
          = Multimaps.index(friendships, f -> f.requestee.get());
      for (User user : Users.all()) {
        Collection<Friendship> requests = byRequestee.get(user);
        System.out.print(".");
        if (requests.size() == user.incomingFriendRequests) continue;
        user.inTransaction(u -> {
          System.out.print('X');
          u.incomingFriendRequests = requests.size();
          return true;
        });
      }
    });
  }
}
