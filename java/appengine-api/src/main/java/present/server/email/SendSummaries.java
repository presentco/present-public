package present.server.email;

import com.googlecode.objectify.Key;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import present.proto.SummaryRequest;
import present.proto.UserService;
import present.server.RpcQueue;
import present.server.environment.Environment;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.notification.Notifier;

/**
 * Sends activity summary emails.
 *
 * @author Bob Lee (bob@present.co)
 */
@Path("sendSummaries")
public class SendSummaries {

  @GET public Response send(@HeaderParam("X-Appengine-Cron") boolean cron) {
    if (!cron) return Response.status(403).build();

    // Don't send emails if this is staging, but leave cron for testing purposes.
    if (Environment.isStaging()) { return Response.ok().build(); }

    List<Key<User>> recipients = recipientKeys();

    // Kick off summary emails
    UserService us = RpcQueue.to(UserService.class).in("emails").create();
    RpcQueue.batch(() -> {
      for (Key<User> key : recipients) {
        try {
          us.sendSummary(new SummaryRequest(key.getName()));
        } catch (IOException e) {
          // Shouldn't happen.
          throw new RuntimeException(e);
        }
      }
    });

    //// Notify users with daily quote
    //Notifier notifier = Notifier.fromPresent()
    //    .to(recipients)
    //    .messageOnly(SummaryEmail.subjectForToday(), Quote.today().toString());
    //    // .enable(u -> u.notificationSettings.affirmations);
    //    // TODO: Turn on for all users once users have setting to turn these notifications off.
    //notifier.go();

    return Response.ok().build();
  }

  static List<Key<User>> recipientKeys() {
    return Users.query()
        .filter(User.Fields.state.name() + " in", User.HAS_ACCESS).keys().list();
  }

}
