package present.server.email;

import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Date;
import present.proto.EmailAddress;
import present.proto.EmailRequest;
import present.proto.EmailService;
import present.proto.UserService;
import present.server.MoreObjectify;
import present.server.RpcQueue;
import present.server.Uuids;
import present.server.model.user.User;
import present.server.tool.InternalRpcClient;
import present.server.tool.ProductionUsers;
import present.server.tool.RemoteTool;

import static present.server.email.SendSummaries.recipientKeys;

/**
 * Generates a summary email, opens it in the browser, and sends it via email.
 *
 * @author Bob Lee (bob@present.co)
 */
public class TestSummaryEmails {

  public static void main(String[] args) {
    //testAllEmails();
    testOneEmail();
  }

  private static void testOneEmail() {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      // Generate an email and open it in the browser.
      SummaryEmail email = new SummaryEmail(ProductionUsers.gabrielle(),
          ProductionUsers.gabrielle().lastActiveTime);
          // System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4));
      printSummary(email);
      writeFile(email);
      //sendEmail(email);
      //email.send();
    });
  }

  private static void testAllEmails() {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      UserService us = RpcQueue.to(UserService.class).in("emails").create();
      RpcQueue.batch(() -> {
        Iterable<User> recipients = MoreObjectify.load(recipientKeys());
        for (User recipient : recipients) {
          SummaryEmail.sendTo(recipient);
        }
      });
    });
  }

  private static void sendEmail(SummaryEmail email) throws IOException {
    //EmailAddress to = new EmailAddress("bob@present.co", null, "Bob Lee");
    //EmailAddress to = new EmailAddress("kristina@present.co", null, "Kristina Plummer");
    //EmailAddress to = new EmailAddress("janete@present.co", null, "Janete Perez");
    EmailAddress to = new EmailAddress("gabrielle@present.co", null, "Gabrielle Taylor");
    EmailService emailService = InternalRpcClient.create(
        "https://api.staging.present.co/api", EmailService.class);
    emailService.send(new EmailRequest.Builder()
        .to(Collections.singletonList(to))
        .subject(email.subject)
        .id(Uuids.newUuid())
        .html(email.toHtml())
        .build());
  }

  private static void writeFile(SummaryEmail email) throws IOException, InterruptedException {
    email.toHtml();
    String path = "/tmp/summary.html";
    new File(path).delete();
    Files.write(Paths.get(path),
        email.toHtml().getBytes(Charsets.UTF_8), StandardOpenOption.CREATE);
    new ProcessBuilder("open", path).inheritIO().start().waitFor();
  }

  private static void printSummary(SummaryEmail email) {
    System.out.println(email.user.fullName() + ":");
    for (GroupSummary groupSummary : email.groupSummaries) {
      System.out.println("  " + groupSummary.group.title + ": " + groupSummary.comments);
    }
  }
}
