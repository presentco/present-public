package present.server.model.user;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import present.proto.RequestHeader;
import present.server.RequestHeaders;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Client utilities
 *
 * @author Bob Lee (bob@present.co)
 */
public class Clients {

  public static Query<Client> query() {
    return ofy().load().type(Client.class);
  }

  public static List<Client> all() {
    return query().list();
  }

  public static Stream<Client> stream() {
    return Streams.stream(all());
  }

  public static List<Client> authenticated() {
    return query()
        .filter("user !=", null)
        .list();
  }

  public static Client getOrCreate(final String uuid) {
    Key<Client> key = Key.create(Client.class, uuid);

    // Try without a transaction first (common path).
    Client client = ofy().load().key(key).now();
    if (client != null) return client;

    return getOrCreateInTransaction(uuid);
  }

  private static Client getOrCreateInTransaction(String uuid) {
    return ofy().transact(() -> {
      Key<Client> key = Key.create(Client.class, uuid);
      Client client = ofy().load().key(key).now();
      if (client == null) {
        client = new Client();
        client.uuid = uuid;
        RequestHeader header = RequestHeaders.current();
        if (header != null) client.updateWith(header);
      }
      return client;
    });
  }

  public static Client current() {
    RequestHeader header = RequestHeaders.current();
    if (header == null) return new Client();
    return getOrCreate(header.clientUuid);
  }

  /** Get all clients for a user. */
  public static List<Client> getClientsForUser(Key<User> userKey) {
    return ofy().load().type(Client.class).filter("user", userKey).list();
  }

  /** Get all clients for a user. */
  public static List<Client> getClientsForUser(User user) {
    return getClientsForUser(user.getKey());
  }

  /** Get all clients for a set of users. */
  public static List<Client> getClientsForUsers(Iterable<User> users) {
    return Iterables.isEmpty(users) ? Collections.<Client>emptyList()
        : ofy().load().type(Client.class).filter("user in", users).list();
  }

  public static S2LatLng mostRecentLocation(List<Client> clients) {
    Client mostRecent = null;
    for (Client client : clients) {
      if (client.location != null) {
        if (mostRecent == null || mostRecent.location == null
            || client.locationTimestamp > mostRecent.locationTimestamp) {
          mostRecent = client;
        }
      }
    }
    return mostRecent == null ? null : new S2CellId(mostRecent.location).toLatLng();
  }
}
