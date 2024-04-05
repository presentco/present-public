package cast.placeholder;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.appengine.repackaged.com.google.common.base.Predicate;
import com.google.appengine.repackaged.com.google.common.collect.Iterables;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Bob Lee (bob@present.co)
 */
public class FlagEmail {

  private static final Logger logger = LoggerFactory.getLogger(FlagEmail.class);

  private static final String TEMPLATE_NAME = "flag-email.html";

  private static final Mustache htmlTemplate =  new DefaultMustacheFactory().compile(
      new InputStreamReader(FlagEmail.class.getResourceAsStream(TEMPLATE_NAME)),
      TEMPLATE_NAME
  );

  public final Client flagger;
  public final Cast flagged;

  public FlagEmail(Client flagger, Cast flagged) {
    this.flagger = flagger;
    this.flagged = flagged;
  }

  public void send() {
    try {
      Properties props = new Properties();
      Session session = Session.getDefaultInstance(props, null);
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress("noreply@cast-placeholder.appspotmail.com",
          "Cast Moderator"));
      msg.addRecipient(Message.RecipientType.TO,
          new InternetAddress("flagged-casts@present.co"));
      // Using one subject per cast will thread emails by cast.
      msg.setSubject("Cast Flagged: " + flagged.id);
      msg.setContent(html(), "text/html");
      Transport.send(msg);
    } catch (Exception e) {
      logger.error("Error sending flag email.", e);
    }
  }

  public String html() {
    StringWriter html = new StringWriter();
    htmlTemplate.execute(html, this);
    return html.toString();
  }

  public String imageUrl() {
    return flagged.imageUrl();
  }

  public int previousCastFlags() {
    return ofy().load().type(Flag.class).filter("cast", flagged).count();
  }

  public int previousClientFlags() {
    return ofy().load().type(Flag.class).filter("client", flagged.creator).count();
  }

  public Iterable<Cast> otherCasts() {
    List<Cast> allCasts = ofy().load().type(Cast.class).filter("creator", flagged.creator).list();
    return Iterables.filter(allCasts, new Predicate<Cast>() {
      @Override public boolean apply(@Nullable Cast cast) {
        return !cast.id.equals(flagged.id);
      }
    });
  }

  private String baseUrl() {
    return Environment.inDevelopment()
        ? "http://localhost:8080/rest"
        : "https://cast-placeholder.appspot.com/rest";
  }

  public String censorCast() {
    return baseUrl() + Censor.CENSOR_CAST + "?id=" + flagged.id;
  }

  public String censorClient() {
    return baseUrl() + Censor.CENSOR_CLIENT + "?id=" + flagged.creator.get().privateId;
  }

  public String mapImage() {
    return flagged.mapImageUrl(300, 300);
  }
}
