package present.server.tool;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import present.proto.EmailAddress;
import present.proto.EmailRequest;
import present.proto.EmailService;
import present.server.EmailServiceImpl;
import present.server.email.Emails;
import present.server.email.PresentEmail;
import present.server.Uuids;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;

import static present.server.email.Emails.welcomeEmailTo;

/**
 * @author Bob Lee (bob@present.co)
 */
public class SendExampleEmails {

  public static void main(String[] args) {
    //String[] emails = args.length == 0 ? new String[] { "bob@present.co" } : args;
    String[] emails = args.length == 0 ? new String[] { "bob@present.co", "janete@present.co" } : args;
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      User bob = User.get("4b7e8518-a0cb-4a25-ac0b-aab5f7e916b8");
      Group group = Groups.findByUuid("2C8691B9-B499-4872-A9FF-D8189D17D129");

      EmailService emailService = new EmailServiceImpl();

      //for (PresentEmail.Type type : PresentEmail.Type.values()) {
      //  PresentEmail.Builder builder = new PresentEmail.Builder()
      //      .uuid(Uuids.newUuid())
      //      .type(type)
      //      .user(bob);
      //  if (type.requiresGroup) builder.group(group);
      //  PresentEmail email = builder.build();
      //  EmailRequest emailRequest = email.requestTo(emails);
      //  try {
      //    emailService.send(emailRequest);
      //  } catch (IOException e) {
      //    throw new RuntimeException(e);
      //  }
      //}

      List<EmailAddress> emailAddresses = Arrays.stream(emails)
          .map(email -> new EmailAddress(email, null, null))
          .collect(Collectors.toList());
      EmailRequest.Builder builder = new EmailRequest.Builder().to(emailAddresses);
      try {
        //emailService.send(Emails.waitlistEmail(builder).id(Uuids.newUuid()).build());
        emailService.send(redirect(welcomeEmailTo(bob), emails));
        //emailService.send(redirect(Emails.shareGroupEmailFor(group), emails));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static EmailRequest redirect(EmailRequest request, String... emails) {
    List<EmailAddress> to = Arrays.stream(emails)
        .map(email -> new EmailAddress(email, null, null))
        .collect(Collectors.toList());
    return request.newBuilder().to(to).build();
  }
}
