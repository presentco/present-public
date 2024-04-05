package present.server.email;

import com.github.mustachejava.Mustache;
import com.google.common.base.Charsets;
import com.google.common.escape.Escaper;
import com.google.common.io.Resources;
import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.util.Collections;
import present.proto.EmailAddress;
import present.proto.EmailRequest;
import present.proto.EmailService;
import present.server.Mustaches;
import present.server.RpcQueue;
import present.server.Uuids;
import present.server.model.group.Group;
import present.server.model.user.User;
import present.server.tool.ProductionUsers;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Email utilities.
 *
 * @author Bob Lee (bob@present.co)
 */
public class Emails {

  private Emails() {}

  public static EmailService service = RpcQueue.create(EmailService.class);
  private static final Escaper parameterEscaper = UrlEscapers.urlFormParameterEscaper();

  /**
   * Creates an email builder with the given recipient. Uses the namespace to generate and set
   * a UUID.
   */
  public static EmailRequest.Builder to(User user, String namespace) {
    EmailAddress to = new EmailAddress(user.email(), null, user.fullName());
    return new EmailRequest.Builder()
        .to(Collections.singletonList(to))
        .id(Uuids.fromName(namespace + ": " + user.uuid));
  }

  /** Sends the given email. */
  public static void send(EmailRequest email) {
    try {
      service.send(email);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Reads a classpath resource into a string. */
  private static String loadResource(String name) {
    try {
      return Resources.toString(Resources.getResource(name), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static EmailRequest welcomeEmailTo(User user) {
    return to(user, "WELCOME")
        .subject("Welcome to Present!")
        .html(Mustaches.toString(WelcomeEmail.template,
            new WelcomeEmail(user.firstName, user.profilePhotoUrl(), user.shortLink())))
        .build();
  }

  private static class WelcomeEmail {
    private static final Mustache template = Mustaches.compileResource("/welcome2.html");

    public final String firstName;
    public final String userImage;
    public final String url;
    public final String encodedUrl;

    private WelcomeEmail(String firstName, String userImage, String url) {
      this.firstName = firstName;
      this.userImage = userImage;
      this.url = url;
      this.encodedUrl = parameterEscaper.escape(url);
    }

    public static void main(String[] args) {
      against(PRODUCTION_SERVER, () -> {
        send(welcomeEmailTo(ProductionUsers.bob()));
      });
    }
  }

  private static final String waitlistEmail = loadResource("waitlist.html");

  public static EmailRequest.Builder waitlistEmail(EmailRequest.Builder emailBuilder) {
    return emailBuilder
        .subject("You're on the list!")
        .html(waitlistEmail);
  }

  public static EmailRequest shareGroupEmailFor(Group group) {
    return to(group.owner.get(), "SHARE-GROUP")
        .subject("Sharing is Caring 🤗")
        .html(Mustaches.toString(ShareGroupEmail.template, new ShareGroupEmail(group)))
        .unsubscribeGroup(4963)
        .build();
  }

  private static class ShareGroupEmail {

    private static final Mustache template = Mustaches.compileResource("/share.html");

    public final String name;
    public final String title;
    public final String encodedTitle;
    public final String locationName;
    public final String url;
    public final String encodedUrl;
    public final String cover;

    public ShareGroupEmail(Group group) {
      this.name = group.owner.get().firstName;
      this.title = group.title;
      this.encodedTitle = parameterEscaper.escape(group.title);
      this.locationName = group.locationName;
      this.url = group.shortLink();
      this.encodedUrl = parameterEscaper.escape(this.url);
      this.cover = group.coverPhoto(550);
    }
  }
}

