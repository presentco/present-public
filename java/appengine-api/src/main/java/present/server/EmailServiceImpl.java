package present.server;

import com.sendgrid.ASM;
import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.EmailAddress;
import present.proto.EmailRequest;
import present.proto.EmailService;
import present.proto.Empty;
import present.server.email.Emails;
import present.wire.rpc.core.ClientException;

import static java.util.Collections.singletonList;

public class EmailServiceImpl implements EmailService {

  private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

  private static final String SEND_GRID_API_KEY = "xxx";

  private static Email FROM = new Email("noreply@present.co", "Present");

  @Override @Internal public Empty send(EmailRequest request) throws IOException {
    if (request.to.isEmpty()) throw new ClientException("Missing recipients.");

    // If more than one recipient, fork a task per recipient.
    if (request.to.size() > 1) {
      RpcQueue.batch(() -> {
        request.to.forEach(recipient -> {
          try {
            Emails.service.send(request.newBuilder().to(singletonList(recipient)).build());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      });
      return Protos.EMPTY;
    }

    if (RequestHeaders.isTest()) {
      // TODO: Capture this so it can be retrieved from a test.
      logger.info("Skipping email in test: {}", GsonLogging.toJson(request));
      return Protos.EMPTY;
    }

    EmailAddress recipient = request.to.get(0);
    String recipientEmail = recipient.email;
    // Redirect Facebook test user emails to test@present.co.
    if (recipientEmail.endsWith("@tfbnw.net")) recipientEmail = "test@present.co";
    Email to = new Email(recipientEmail, recipient.name);
    Content content = new Content("text/html", request.html);
    Mail mail = new Mail(FROM, request.subject, to, content);
    mail.addHeader("Message-ID", "<" + request.id + "@present.co>");

    // From https://app.sendgrid.com/suppressions/advanced_suppression_manager
    if (request.unsubscribeGroup != null) {
      mail.setASM(new ASM());
      mail.asm.setGroupId(request.unsubscribeGroup);
    }

    SendGrid sendGrid = new SendGrid(SEND_GRID_API_KEY);
    Request emailRequest = new Request();
    emailRequest.setMethod(Method.POST);
    emailRequest.setEndpoint("mail/send");
    emailRequest.setBody(mail.build());
    Response response = sendGrid.api(emailRequest);
    if (response.getStatusCode() >= 300) {
      logger.error("Error sending email: " + response.getStatusCode(),
          new IOException(response.getBody()));
    }
    return new Empty();
  }
}
