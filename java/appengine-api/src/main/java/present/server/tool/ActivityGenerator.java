package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import present.proto.GroupService;
import present.proto.Platform;
import present.proto.PutCommentRequest;
import present.server.ClientUtil;
import present.server.PresentObjectifyFactory;
import present.server.Uuids;
import present.server.model.PresentEntities;
import present.server.model.group.Group;
import present.server.model.user.Client;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.wire.rpc.core.RpcProtocol;

import static present.server.tool.RemoteTool.installRemoteAPI;

/**
 * @author Pat Niemeyer (pat@pat.net)
 * Date: 11/25/17
 */
public class ActivityGenerator {
  // TODO: Consolidate all URLs related to each server into one place (Environment?)
  static String server = RemoteTool.STAGING_SERVER;
  static String serverApiUrl = "https://api.staging.present.co/api";

  // The destination user who will receive the chat or group activity notification
  static String userEmail = "pat@pat.net";

  // The other user who will be sending the chat message or group comment
  static String otherUserEmail = "pat_etdivui_tester@tfbnw.net";

  // The saved group that is the target of the activity by the other user
  static String savedGroupTitle = "New Test";

  public static class GenerateGroupComment {
    public static void main(String[] args) throws IOException {
      runAgainstUser((user, otherUser) -> {
        Client fromUserClient = getFirstClient(otherUser, Platform.IOS);
        Group group = getSavedGroup(user, savedGroupTitle);

        // TODO: Make factories in RemoteTool
        GroupService groupService = ClientUtil.rpcClient(serverApiUrl, GroupService.class, RpcProtocol.PROTO, fromUserClient.uuid, Platform.IOS);

        PutCommentRequest request = new PutCommentRequest.Builder()
            .uuid(Uuids.newUuid())
            .groupId(group.uuid())
            .comment("Hey Group: " + new SimpleDateFormat("h:mm:ss").format(new Date()))
            .build();
        groupService.putComment(request);
        System.out.println("Generated comment on group '"+group.title+"': " + request.comment);
      });
    }
  }

  private static Group getSavedGroup(User user, String title) {
    Group group = user.joinedGroups().stream()
      .filter(g->g.title.trim().equals(title.trim()))
      .findFirst()
      .orElse(null);
    if (group == null) {
      throw new RuntimeException("User does not have saved group: " + title);
    }
    return group;
  }

  private static Client getFirstClient(User user, Platform platform) {
    // Send using the 'from' user's first iOS client
    Client foundClient = user.clients()
        .stream()
        .filter(client -> client.platform == platform.getValue())
        .findFirst()
        .orElse(null);
    if (foundClient == null) {
      throw new RuntimeException("User has no iOS clients: " + user);
    }
    return foundClient;
  }

  public static void runAgainstUser(ConsumeUsers userConsumer) throws IOException {
      RemoteApiInstaller installer = installRemoteAPI(server);
      ObjectifyService.setFactory(new PresentObjectifyFactory());
      try (Closeable closeable = ObjectifyService.begin()) {
        PresentEntities.registerAll();

        User user = Users.findByEmail(userEmail);
        User otherUser = Users.findByEmailExpected(otherUserEmail);
        userConsumer.accept(user, otherUser);
      } finally {
        installer.uninstall();
      }
  }

  @FunctionalInterface
  public interface ConsumeUsers {
    void accept(User user, User otherUser) throws IOException;
  }
}
