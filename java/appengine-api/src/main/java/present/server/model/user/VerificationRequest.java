package present.server.model.user;

import com.github.mustachejava.Mustache;
import com.google.common.base.Strings;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import present.proto.EmailAddress;
import present.proto.EmailRequest;
import present.proto.EmailService;
import present.proto.Gender;
import present.server.Mustaches;
import present.server.RpcQueue;
import present.server.environment.Environment;
import present.server.model.BasePresentEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Request for a code used to verify ownership of an email address or phone number. Supports
 * two implementations:
 *
 * 1. A long, client-independent, random code.
 * 2. A short code that must be used with a specified client.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class VerificationRequest extends BasePresentEntity<VerificationRequest> {

  private static final int CODE_LENGTH = 6;
  public static final String TEST_CODE = "111111";

  /** Long, client-independent code, or "[Client UUID]:[Code]". */
  @Id public String id;

  @Index public String email;
  @Index public String phoneNumber;
  public String firstName;
  public String lastName;
  public List<String> spaceIds;
  public Gender gender = Gender.UNKNOWN;
  public String userId;

  private VerificationRequest() {}

  public String code() {
    int separator = id.indexOf(":");
    return separator > -1
        ? id.substring(separator + 1) // Short, client-dependent code
        : id;                         // Long, client-independent code
  }

  /** The verification URL. */
  public String url() {
    return Environment.current().webUrl() + "/v/" + code();
  }

  public static VerificationRequest clientIndependent() {
    VerificationRequest request = new VerificationRequest();
    request.id = newCode();
    return request;
  }

  public static VerificationRequest clientDependent(boolean test) {
    VerificationRequest request = new VerificationRequest();
    Client client = Clients.current();
    String code = test ? TEST_CODE : randomNumber(CODE_LENGTH);
    request.id = client.uuid + ":" + code;
    return request;
  }

  public VerificationRequest name(String first, String last) {
    this.firstName = first;
    this.lastName = last;
    return this;
  }

  public static VerificationRequest parseAndSave(String email) {
    InternetAddress address;
    try {
      address = InternetAddress.parse(email)[0];
    } catch (AddressException e) {
      throw new RuntimeException(e);
    }
    String personal = address.getPersonal();
    int space = personal.indexOf(' ');
    String first = personal.substring(0, space);
    String last = personal.substring(space + 1);
    VerificationRequest vr = clientIndependent().name(first, last).email(address.getAddress());
    vr.save();
    System.out.println("With the app installed, tap this link on your phone to log in: " + vr.url());
    return vr;
  }

  public User findUser() {
    if (userId != null) {
      User user = User.get(userId);
      if (user != null) return user;
    }
    if (phoneNumber != null) {
      User user = Users.findByPhone(phoneNumber);
      if (user != null) return user;
    }
    if (email != null) {
      return Users.findByEmail(email);
    }
    return null;
  }

  public VerificationRequest email(String email) {
    this.email = email.toLowerCase();
    try {
      new InternetAddress(this.email);
    } catch (AddressException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  private static final Mustache emailTemplate = Mustaches.compileResource("/verify.html");

  private static final SecureRandom random = new SecureRandom();

  private void validate() {
    checkNotNull(this.id);
    checkNotNull(this.firstName);
    checkNotNull(this.lastName);
    checkNotNull(this.email);
  }

  private static String newCode() {
    return new BigInteger(64, random).toString(36);
  }

  private static final EmailService emailService = RpcQueue.to(EmailService.class)
      .noRetries()
      .create();

  private String fullName() {
    return firstName + " " + lastName;
  }

  private void sendEmail() {
    try {
      emailService.send(new EmailRequest.Builder()
          .subject("Welcome to Present")
          .id(id)
          .html(Mustaches.toString(emailTemplate, this))
          .to(Collections.singletonList(new EmailAddress(this.email, null, fullName())))
          .build());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Saves and emails requests for verification. */
  public static void emailRequests(Iterable<VerificationRequest> requests) {
    for (VerificationRequest request : requests) request.validate();
    ofy().save().entities(requests).now();
    RpcQueue.batch(() -> {
      for (VerificationRequest request : requests) {
        request.sendEmail();
      }
    });
  }

  public static VerificationRequest forCode(String code) {
    Client client = Clients.current();
    LoadResult<VerificationRequest> clientDependentResult
        = ofy().load().type(VerificationRequest.class).id(client.uuid + ":" + code);
    LoadResult<VerificationRequest> clientIndependentResult
        = ofy().load().type(VerificationRequest.class).id(code);
    VerificationRequest clientDependent = clientDependentResult.now();
    if (clientDependent != null) return clientDependent;
    return clientIndependentResult.now();
  }

  private static String randomNumber(int length) {
    int value = random.nextInt(Integer.MAX_VALUE);
    String s = Integer.toString(value);
    if (s.length() > length) s = s.substring(0, length);
    return Strings.padStart(s, length, '0');
  }

  @Override protected VerificationRequest getThis() {
    return this;
  }

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      String to = "Jessie Newman <jessienewman9@gmail.com>";
      VerificationRequest vr = VerificationRequest.parseAndSave(to);
      System.out.printf("Hi, %s! Please install Present, and then tap this link on your phone: %s\n\n"
          + "It should launch the app and log you in.\n\nBob\n", vr.firstName, vr.url());
    });
  }
}
