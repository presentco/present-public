package present.server.tool;

import present.server.Uuids;
import present.server.model.comment.Comment;
import present.server.model.group.Group;
import present.server.model.group.WelcomeGroup;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

public class UpdateWelcomeComments {

  private static String WELCOME_MESSAGE = "Hello! I’m Bob, one of Present's co-founders. Our mission is to help you find fun things to do with people nearby."
      + " I added you to this [CITY] circle so you can meet other Present members near you.\n"
      + "\n"
      + "\uD83D\uDC4B Please introduce yourself to the group.\n"
      + "\n"
      + "\uD83D\uDD0E Explore and join other circles nearby. We make our recommendations based on dozens of factors like distance and popularity.\n"
      + "\n"
      + "\uD83D\uDC6B Present is more fun with friends. Add them from your contacts so you can see what they’re up to and do more together.\n"
      + "\n"
      + "➕ Create a circle of your own to attract like-minded people. Make it private if you want to keep things lowkey.\n"
      + "\n"
      + "Thank you for joining our community. Please let me know if you have any questions!\n"
      + "\n"
      + "P.S. You can mute \uD83D\uDD07 circles if they get too noisy. I already muted this one for you! \uD83E\uDD17\n";

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      WelcomeGroup.ALL.forEach(wg -> {
        Group group = ofy().load().key(wg.key()).now();
        String id = Uuids.fromName("Welcome comment for " + group.id);
        Comment comment = Comment.get(group, id);
        comment.text = WELCOME_MESSAGE.replace("[CITY]", wg.city);
        comment.save();
      });
    });
  }
}