package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import present.server.facebook.FacebookUserData;
import present.server.model.PresentEntities;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.User;
import present.server.model.user.Users;

/**
 * List users with their clients, both sorted by most recent client activity time.
 * List orphaned clients.
 */
@SuppressWarnings("ALL")
public class ListUsers extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      //NamespaceManager.set("test");
      PresentEntities.registerAll();

      listUsers();
    }
    installer.uninstall();
  }

  public static void listUsers() {
    // list Users sorted by most recent active client
    List<UserAndClients> usersInfo = getUsersAndClients();
    listUsers(usersInfo);
    listAccountsWithIdentialNames(usersInfo);
    listOrphanedClients();
  }

  public static List<UserAndClients> getUsersAndClients() {
    return Streams.stream(Users.all())
          .map(user->new UserAndClients(user))
          .sorted(Comparator.comparing((UserAndClients u) ->u.lastActivity()).reversed())
          .collect(Collectors.toList());
  }

  public static void toCSV() {
    System.out.println("User,Last Activity,Email,Phone,Facebook Id,Facebook Link,Member,UUID");
    List<ListUsers.UserAndClients> usersAndClients = ListUsers.getUsersAndClients();
    for (ListUsers.UserAndClients userAndClients : usersAndClients) {
      FacebookUserData facebookUserData = userAndClients.user.facebookData();
      SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
      System.out.printf(
          "%s,%s,%s,%s,%s,%s,%s,%s\n",
          userAndClients.user.publicName(),
          dateFormat.format(new Date(userAndClients.lastActivity())),
          userAndClients.user.email(),
          userAndClients.user.phoneNumber,
          userAndClients.user.facebookId,
          facebookUserData == null ? "" : facebookUserData.link,
          userAndClients.user.hasAccess(),
          userAndClients.user.uuid
      );
    }
  }

  static void listUsers(List<UserAndClients> usersInfo) {

    System.out.println("--------- Users ---------");
    for (UserAndClients user : usersInfo) {

      System.out.printf("\nUser: %s, Email: %s, Last activity: %s, UUID: %s\n",
          user.user.publicName(), user.user.email(), new Date(user.lastActivity()), user.user.uuid);
      if (user.user.facebookId != null || user.user.facebookAccessToken != null || user.user.facebookData() != null)
      {
        System.out.format("\tFacebook id: %s, has token: %s, data: %s\n",
            user.user.facebookId, user.user.facebookAccessToken!=null, user.user.facebookData());
      } else {
        System.out.println("\tNo Facebook data.");
      }

      if (user.clients.isEmpty()) {
        System.out.println("\tNo Clients.");
      } else for (Client client : user.clients) {
        System.out.printf("\tClient: %s, Date: %s, Token: %s, UUID: %s\n",
            client.deviceName, new Date(client.deviceTokenUpdateTime), client.deviceToken, client.uuid);
      }
    }
  }

  private static void listAccountsWithIdentialNames(List<UserAndClients> usersInfo)
  {
    System.out.println("\n\n------------ Accounts with identical names -------------");

    Multimap<String, UserAndClients> map = Multimaps.index(usersInfo, u -> {
      return u.user.publicName() == null ? "<null>" : u.user.publicName();
    });
    Collection<Collection<UserAndClients>> values = map.asMap().values();


    for (Map.Entry<String, Collection<UserAndClients>> accounts : map.asMap().entrySet())
    {
      if(accounts.getValue().size() > 1) {
        System.out.println("Multiple accounts with the same user name: "+accounts.getKey());
        for (UserAndClients user: accounts.getValue()) {
          System.out.println(
              "\tLast activity: " +new Date(user.lastActivity())
              +", uuid: "+ user.user.uuid
              + ", email: " + user.user.email()
              + ", facebookId: " + user.user.facebookId
          );
        }
      }
    }
  }

  private static void listOrphanedClients()
  {
    System.out.println("\n\n------------ Orphaned Clients -------------");
    Iterable<Client> clients = Clients.all();
    List<Client> clientsWithMissingUser = new ArrayList<>();
    for (Client client : clients) {
      if (client.user == null || client.user.get() == null) {
        clientsWithMissingUser.add(client);
      }
    }
    clientsWithMissingUser.sort(sortUpdateTimeDescending);
    for (Client client : clientsWithMissingUser) {
      System.out.println("Client: "+client.deviceName
          +", token: "+client.deviceToken
          +", tokenUpdate: "+new Date(client.deviceTokenUpdateTime)
          +", user: "+client.user
          +", uuid: "+client.uuid);
    }
  }

  @SuppressWarnings("ALL")
      // user with clients sorted by device update time
  public static class UserAndClients {
    User user;
    List<Client> clients;

    public UserAndClients(User user) {
      this.user = user;
      this.clients = Lists.newArrayList(user.clients());
      this.clients.sort(sortUpdateTimeDescending);
    }

    public long lastActivity() {
      if (clients.isEmpty()) {
        return Long.MIN_VALUE;
      }
      return clients.get(0).deviceTokenUpdateTime;
    }

    /** The enabled status or null if the status is unknown */
    public @Nullable Boolean notificationsEnabled() {
      return clients.stream()
        .map(client -> client.notificationsEnabled)
        .filter(Predicates.notNull())
        .reduce(Boolean::logicalOr)
        .orElse(null);
    }
  }

  static Comparator<Client> sortUpdateTimeDescending = new Comparator<Client>() {
    @Override public int compare(Client o1, Client o2) {
      return -Long.compare(o1.deviceTokenUpdateTime, o2.deviceTokenUpdateTime);
    }
  };

}


