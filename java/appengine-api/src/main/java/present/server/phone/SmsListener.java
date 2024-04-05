package present.server.phone;

import java.util.regex.Pattern;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.phone.PhoneNumbers;
import present.proto.SlackPostRequest;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.slack.SlackClient;

/**
 * Receives incoming text messages.
 */
@Path("/sms")
public class SmsListener {

  private static final Logger logger = LoggerFactory.getLogger(SmsListener.class);

  /** Twilio handles these automatically. */
  private static final Pattern stopPatterns = Pattern.compile(
      "^(STOP|STOPALL|UNSUBSCRIBE|CANCEL|END|QUIT)$", Pattern.CASE_INSENSITIVE);

  @POST
  @Produces("text/xml; charset=UTF-8")
  public Response sms(
      @FormParam("MessageSid") String messageSid,
      @FormParam("AccountSid") String accountSid,
      @FormParam("MessagingServiceSid") String messagingServiceSid,
      @FormParam("From") String from,
      @FormParam("To") String to,
      @FormParam("Body") String body,
      @FormParam("NumMedia") String numMedia
  ) {
    logger.info("MessageSid: {}\n"
        + "AccountSid: {}\n"
        + "MessageServiceSid: {}\n"
        + "From: {}\n"
        + "To: {}\n"
        + "Body: {}\n"
        + "NumMedia: {}",
        messageSid,
        accountSid,
        messagingServiceSid,
        from,
        to,
        body,
        numMedia
    );

    User user = Users.findByPhone(PhoneNumbers.validateE164(from));

    body = body.trim();

    if (user != null && stopPatterns.matcher(body).matches()) {
      logger.info("Stopping SMS.");
      user.inTransaction(u -> {
        u.smsStopped = true;
      });
    }

    String message = "From: ";
    if (user != null) {
      message += SlackClient.link(user) + " (" + from + ")";
    } else {
      message += from;
    }
    message += "\n\n\"" + body + "\n";

    SlackClient.post(new SlackPostRequest.Builder()
        .text(message)
        .emoji(":iphone:")
        .channel("#sms")
        .build());

    return Response.ok("<Response></Response>").build();
  }
}
