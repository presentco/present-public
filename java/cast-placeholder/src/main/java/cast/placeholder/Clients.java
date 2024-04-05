package cast.placeholder;

import present.wire.rpc.core.RpcInvocation;
import present.proto.RequestHeader;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import java.util.UUID;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Bob Lee (bob@present.co)
 */
public class Clients {

  private Clients() {}

  public static Client getOrCreate(final String uuid) {
    return ofy().transact(new Work<Client>() {
      @Override public Client run() {
        Key<Client> key = Key.create(Client.class, uuid);
        Client client = ofy().load().key(key).now();
        if (client == null) {
          client = new Client();
          client.privateId = uuid;
          client.publicId = UUID.randomUUID().toString();
          client.creationTime = System.currentTimeMillis();
          ofy().save().entity(client);
        }
        return client;
      }
    });
  }

  public static Client current() {
    RequestHeader header = RpcInvocation.current().getHeader(RequestHeader.class);
    return getOrCreate(header.clientUuid);
  }
}
