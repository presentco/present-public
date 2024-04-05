package present.live.server;

import present.proto.LiveCommentsRequest;
import java.nio.ByteBuffer;

/**
 * Represents a remote client.
 *
 * @author Bob Lee
 */
public abstract class RemoteClient {

  final LiveCommentsRequest request;

  public RemoteClient(LiveCommentsRequest request) {
    this.request = request;
  }

  public String groupId() {
    return request.groupId;
  }

  public String clientUuid() {
    return request.header.clientUuid;
  }

  /**
   * The userId The userId associated with the client: Note that this should be redundant with the
   * client id in the header, however the live server currently has no way to resolve clients to
   * users for user based filtering.
   */
  public String clientUserId() {
    return request.userId;
  }

  /** Sends the given bytes to this client. Non-blocking. */
  public abstract void send(ByteBuffer bytes);
}
