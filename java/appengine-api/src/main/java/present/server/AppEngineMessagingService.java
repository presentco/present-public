package present.server;

import java.io.IOException;
import java.util.Collections;
import present.proto.ChatsResponse;
import present.proto.Empty;
import present.proto.MessagingService;

public class AppEngineMessagingService implements MessagingService {

  @Override public ChatsResponse getChats(Empty empty) throws IOException {
    return new ChatsResponse(Collections.emptyList());
  }
}
