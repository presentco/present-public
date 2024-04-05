package present.server.email;

import com.github.mustachejava.Mustache;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import present.proto.EmailAddress;
import present.proto.EmailRequest;
import present.server.Mustaches;
import present.server.Uuids;
import present.server.model.group.Group;
import present.server.model.user.User;

import static com.google.common.base.Preconditions.checkNotNull;

public class PresentEmail {

  private static final Mustache template = Mustaches.compileResource("/email.html");

  private static final String GROUP_LINK
      = "<a style=\"color:rgb(116,60,206); text-decoration: none;\" href=\"{{link}}\">{{groupTitle}}</a>";

  private static final String OPEN_IN_APP = "Open in App";

  public enum Type {
    FRIEND_JOINED_PRESENT(
        "YESSSS! Your friend {{{userName}}} just joined Present!",
        "Your friend <b>{{userName}}</b> just joined Present!",
        "Say Hi!",
        false
    ),

    MESSAGED_YOU(
        "{{{userName}}} sent you a message. \uD83D\uDC8C",
        "<b>{{userName}}</b> sent you a message on Present.",
        OPEN_IN_APP,
        false
    ),

    SOMEONE_JOINED_YOUR_GROUP(
        "Woot! Someone extraordinary joined '{{{groupTitle}}}!' \uD83E\uDD84",
        "<b>{{userName}}</b> joined your circle " + GROUP_LINK + ".",
        OPEN_IN_APP,
        true
    ),

    FRIEND_INVITED_YOU_TO_GROUP(
        "Hey Miss Popular! \uD83E\uDD84 You've been invited you to '{{{groupTitle}}}.'",
        "<b>{{userName}}</b> invited you to join the circle " + GROUP_LINK + ".",
        OPEN_IN_APP,
        true
    ),

    FRIEND_JOINED_SAME_GROUP(
        "OMG - Guess who joined '{{{groupTitle}}}.' \uD83D\uDC6F",
        "<b>{{userName}}</b> joined the circle " + GROUP_LINK + ".",
        OPEN_IN_APP,
        true
    );

    private final Mustache subject;
    private final Mustache message;
    private final String buttonLabel;
    public final boolean requiresGroup;

    Type(String subject, String message, String buttonLabel, boolean requiresGroup) {
      this.subject = Mustaches.compileString(subject, name() + ".subject");
      this.message = Mustaches.compileString(message, name() + ".message");
      this.buttonLabel = checkNotNull(buttonLabel);
      this.requiresGroup = requiresGroup;
    }
  }

  private final String uuid;
  private final User user;
  private final Type type;
  private final Group group;

  private PresentEmail(Builder builder) {
    uuid = builder.uuid;
    user = builder.user;
    type = builder.type;
    group = builder.group;
  }

  public String uuid() {
    return uuid;
  }

  public String subject() {
    return Mustaches.toString(type.subject, this);
  }

  public String message() {
    return Mustaches.toString(type.message, this);
  }

  public String userName() {
    return user.firstName;
  }

  public String userPhoto() {
    return user.profilePhotoUrl(200);
  }

  public String buttonLabel() {
    return type.buttonLabel;
  }

  public String coverPhoto() {
    return group.coverPhoto(600);
  }

  public String groupTitle() {
    return group.title.trim();
  }

  public String locationName() {
    return group.locationName;
  }

  public String link() {
    return type.requiresGroup ? group.shortLink() : user.shortLink();
  }

  public boolean hasGroup() { return group != null; }

  public String toHtml() {
    return Mustaches.toString(template, this);
  }

  public static final class Builder {
    private String uuid = Uuids.newUuid();
    private User user;
    private Type type;
    private Group group;

    public Builder uuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder user(User user) {
      this.user = user;
      return this;
    }

    public Builder type(Type type) {
      this.type = type;
      return this;
    }

    public Builder group(Group group) {
      this.group = group;
      return this;
    }

    public PresentEmail build() {
      checkNotNull(uuid);
      checkNotNull(user);
      checkNotNull(type);
      if (type.requiresGroup) checkNotNull(group);
      return new PresentEmail(this);
    }
  }

  public EmailRequest requestTo(String... emails) {
    List<EmailAddress> emailAddresses = Arrays.stream(emails)
        .map(email -> new EmailAddress(email, null, null))
        .collect(Collectors.toList());
    return new EmailRequest.Builder()
        .to(emailAddresses)
        .id(uuid())
        .subject(subject())
        .html(toHtml())
        .build();
  }

  public EmailRequest requestTo(Iterable<User> users) {
    List<EmailAddress> to = Streams.stream(users)
        .map(User::emailAddress)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (to.isEmpty()) return null;
    return new EmailRequest.Builder()
        .to(to)
        .id(uuid())
        .subject(subject())
        .html(toHtml())
        .build();
  }

  public void sendTo(Iterable<User> users) throws IOException {
    // Temporarily disable emails except welcome emails.
    // EmailRequest request = requestTo(users);
    // if (request != null) Emails.service.send(request);
  }

  public void sendTo(User user) throws IOException {
    sendTo(Collections.singleton(user));
  }
}
