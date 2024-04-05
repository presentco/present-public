package present.server.phone;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.phone.PhoneNumbers;
import present.phone.TwilioGateway;
import present.proto.Empty;
import present.proto.PhoneService;
import present.proto.SmsRequest;
import present.server.Internal;
import present.server.Protos;
import present.server.environment.Environment;
import present.server.model.user.Users;

public class PhoneServiceImpl implements PhoneService {

  private static final Logger logger = LoggerFactory.getLogger(PhoneServiceImpl.class);

  @Override @Internal public Empty sms(SmsRequest request) throws IOException {
    if (Environment.isProduction()) {
      sms(request.phoneNumber, request.text);
      return Protos.EMPTY;
    }

    if (Environment.isTest()) {
      logger.info("sms(" + request + ")");
      return Protos.EMPTY;
    }

    // In staging, send texts to the user's own phone.
    String to = Users.current().phoneNumber;
    if (to == null) {
      logger.info("No phone #.");
    } else {
      sms(to, request.text);
    }

    return Protos.EMPTY;
  }

  private void sms(String to, String text) {
    new TwilioGateway().sms(PhoneNumbers.validateUsPhone(to), text);
  }
}
