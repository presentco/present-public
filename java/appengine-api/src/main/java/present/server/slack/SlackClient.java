package present.server.slack;

import java.io.IOException;
import present.proto.SlackPostRequest;
import present.proto.SlackService;
import present.server.RpcQueue;
import present.server.model.group.Group;
import present.server.model.user.User;

public class SlackClient {

  private static SlackService INSTANCE = RpcQueue.create(SlackService.class);

  public static void post(SlackPostRequest request) {
    try {
      INSTANCE.post(request);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String link(User user) {
    return link(user.publicName(), user.shortLink(), user.consoleUrl());
  }

  public static String link(Group group) {
    return link(group.title, group.shortLink(), group.consoleUrl());
  }

  public static String link(String label, String link, String consoleUrl) {
    return String.format("<%s|%s> (<%s|entity>)",
        link,
        label,
        consoleUrl
    );
  }

  public static SlackPostRequest.Builder reportBuilder() {
    return new SlackPostRequest.Builder()
        .channel("#monitoring")
        .emoji(":poop:");
  }
}
