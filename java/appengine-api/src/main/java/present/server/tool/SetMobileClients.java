package present.server.tool;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import java.util.Comparator;
import java.util.List;
import present.server.KeysOnly;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * @author Bob Lee (bob@present.co)
 */
public class SetMobileClients {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      List<Client> allClients = ofy().load().group(KeysOnly.class).type(Client.class).list();
      List<User> members = ofy().load().group(KeysOnly.class).type(User.class)
          .filter("state", UserState.MEMBER).list();
      Multimap<Key<User>, Client> index = Multimaps.index(
          Iterables.filter(allClients, c -> c.user != null), c -> c.user.getKey());
      for (int i = 0; i < members.size(); i++) {
        User member = members.get(i);
        Client mobileClient = index.get(member.getKey()).stream()
            .filter(Client::isMobile)
            .max(Comparator.comparing(c -> c.locationTimestamp))
            .orElse(null);
        if (mobileClient != null && member.mobileClient == null) {
          System.out.println((i * 100 / members.size())
              + "% Setting " + member + " to " + mobileClient);
          member.inTransaction(u -> {
            u.mobileClient = Ref.create(mobileClient);
          });
        }
      }
    });
  }
}
