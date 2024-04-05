package present.server.phone;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.PhoneService;
import present.proto.SmsRequest;
import present.server.RpcQueue;
import present.server.model.user.User;

public class PhoneServices {

  private static final Logger logger = LoggerFactory.getLogger(PhoneServices.class);

  private static final PhoneService phoneService
      = RpcQueue.to(PhoneService.class).noRetries().create();

  /** Sends an SMS in the background. */
  public static void sms(User user, String message) {
    if (user.smsStopped) {
      logger.info("User stopped SMS.");
      return;
    }

    try {
      phoneService.sms(new SmsRequest(user.phoneNumber, message));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
