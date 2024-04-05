package present.server.slack;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.SlackAttachment;
import present.proto.SlackPostRequest;
import present.server.model.group.Group;
import present.server.model.user.User;
import present.server.model.util.Coordinates;

import static present.server.slack.SlackClient.link;

/**
 * Announce a new user or group on the appropriate slack channel.
 *
 * @author Pat Niemeyer (pat@present.co)
 */
public class AnnounceToSlack {
  private static final Logger logger = LoggerFactory.getLogger(AnnounceToSlack.class);

  public static void newGroup(Group group) {
    logger.info("Announce new group to slack: {}", group);
    User owner = group.owner.get();
    String text = String.format(
        "%s created <%s|%s> (<%s|entity>).",
        link(owner),
        group.shortLink(),
        group.title,
        group.consoleUrl()
    );
    List<SlackAttachment> attachments = new ArrayList<>();
    if (group.hasCoverContent()) {
      attachments.add(new SlackAttachment("Cover Photo", group.coverContent.get().url()));
    }
    attachments.add(new SlackAttachment("Location", mapImageUrl(group.location())));
    SlackClient.post(new SlackPostRequest.Builder()
        .emoji(":candy:")
        .channel("#newcircles")
        .text(text)
        .attachments(attachments)
        .build());
  }

  public static void userTransition(User user, String action) {
    logger.info("Announce new user to slack: {}", user);
    String text = String.format("%s %s.", link(user), action);
    List<SlackAttachment> attachments = new ArrayList<>();
    attachments.add(new SlackAttachment("Photo", user.profilePhotoUrl()));
    if (user.signupLocation != null) {
      attachments.add(new SlackAttachment("Location", mapImageUrl(user.signupLocation)));
    }
    SlackClient.post(new SlackPostRequest.Builder()
        .emoji(":celebrate:")
        .channel("#newusers")
        .text(text)
        .attachments(attachments)
        .build());
  }

  /**
   * A public URL for a map of the location of this group.
   * e.g. May be used in the admin console or web client.
   */
  private static String mapImageUrl(Coordinates location) {
    String key = "xxx";
    return "https://maps.googleapis.com/maps/api/staticmap"
        + "?size=600x600"
        + "&maptype=roadmap"
        + "&markers=color:red%7Clabel:S%7C" + location.latitude + "," + location.longitude
        + "&key="+key;
  }
}
