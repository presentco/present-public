package present.server.tool;

import present.server.model.activity.Event;
import present.server.model.activity.Events;
import present.server.model.comment.Comment;
import present.server.model.user.User;

/**
 * @author Bob Lee (bob@present.co)
 */
public class DebugActivity {

  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      User pegah = User.get("74c89bc5-419b-4004-89ce-6ee1eaa87044");
      Iterable<Event> events = Events.getEventsForUser(pegah, 0L, System.currentTimeMillis());
      for (Event event : events) {
        Object entity = event.defaultTarget.get();
        if (entity instanceof Comment) {
          Comment c = (Comment) entity;
          System.out.println(event.initiator + " " + pegah.canSee(c.author.get()));
        }
      }
    });
  }
}
