package present.server.tool;

import present.server.model.user.Client;
import present.server.model.PresentEntities;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class UpdateClientIndexes extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();
      QueryResultIterable<Client> clients = ofy().load().type(Client.class).iterable();
      ofy().save().entities( clients ).now();

      // Test the deviceTokenIndex
      Iterable<Client> filter = Iterables.filter(clients, new Predicate<Client>() {
        @Override public boolean apply(Client client) {
          return client.deviceToken != null;
        }
      });
      Client first = Iterables.getFirst(filter, null);
      assert first != null;
      List<Client> found = ofy().load().type(Client.class).filter("deviceToken", first.deviceToken).list();
      System.err.println("found = "+found);
      assert !found.isEmpty();
    }
    installer.uninstall();
  }
}
