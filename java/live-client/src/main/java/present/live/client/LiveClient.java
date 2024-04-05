package present.live.client;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.CommentResponse;
import present.proto.FindLiveServerRequest;
import present.proto.FindLiveServerResponse;
import present.proto.GroupService;
import present.proto.LiveCommentsRequest;
import present.proto.LiveCommentsResponse;
import present.proto.RequestHeader;

/**
 * Manages a web socket connection to a live comment server.
 *
 * @author Bob Lee (bob@present.co)
 */
public class LiveClient {

  private static final Logger logger = LoggerFactory.getLogger(LiveClient.class);

  private final Listener listener;
  private final WebSocket webSocket;
  private final LiveCommentsRequest lcr;

  /**
   * @param userId The userId associated with the client: Note that this should be redundant with
   * the client id in the header, however the live server currently has no way to resolve clients
   * to users for user based filtering.
   */
  public LiveClient(
      String host,
      int port,
      RequestHeader header,
      String userId,
      String groupId,
      Listener listener) throws IOException {
    String liveUrl = "wss://" + host + ":" + port + "/comments";
    if (listener == null) throw new NullPointerException("listener");
    this.listener = listener;
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(liveUrl).build();
    this.webSocket = client.newWebSocket(request, new CommentListener());
    lcr = new LiveCommentsRequest(header, groupId, userId, 1);
  }

  /**
   * @param userId The userId associated with the client: Note that this should be redundant with
   * the client id in the header, however the live server currently has no way to resolve clients
   * to users for user based filtering.
   */
  private LiveClient(
      FindLiveServerResponse flsr,
      RequestHeader header,
      String userId,
      String groupId,
      Listener listener) throws IOException {
    this(flsr.host, flsr.port, header, userId, groupId, listener);
  }

  /**
   * @param userId The userId associated with the client: Note that this should be redundant with
   * the client id in the header, however the live server currently has no way to resolve clients
   * to users for user based filtering.
   */
  public LiveClient(GroupService groupService,
      RequestHeader header,
      String userId,
      String groupId,
      Listener listener) throws IOException {
    this(groupService.findLiveServer(new FindLiveServerRequest(groupId)),
        header, userId, groupId, listener);
  }

  private class CommentListener extends WebSocketListener {
    @Override public void onOpen(WebSocket webSocket, Response response) {
      logger.info("onOpen()");
      webSocket.send(ByteString.of(lcr.encode()));
    }

    /** Ready to receive comments. */
    boolean ready = false;

    @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
      logger.info("onMessage()");
      try {
        if (!ready) {
          LiveCommentsResponse response = LiveCommentsResponse.ADAPTER.decode(bytes);
          if (response.status == LiveCommentsResponse.Status.READY) {
            listener.ready();
          } else {
            listener.networkError(new IOException("Bad response status: " + response.status));
          }
          ready = true;
          return;
        }
        CommentResponse comment = CommentResponse.ADAPTER.decode(bytes);
        if (!comment.deleted) {
          listener.comment(comment);
        } else {
          listener.deleted(comment);
        }
      } catch (Exception e) {
        logger.error("Error calling LiveClient.Listener.", e);
      }
    }

    @Override public void onMessage(WebSocket webSocket, String text) {
      throw new AssertionError();
    }

    @Override public void onClosing(WebSocket webSocket, int code, String reason) {
      logger.debug("onClosing()");
    }

    @Override public void onClosed(WebSocket webSocket, int code, String reason) {
      logger.debug("onClosed()");
      listener.closed();
    }

    @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
      listener.networkError(t);
    }
  }

  public void close() {
    webSocket.close(1000, null);
  }

  /** Listens for live client events. */
  public interface Listener {

    /** Called after the client is registered to receive comments. */
    void ready();

    /** Called when a comment is received. */
    void comment(CommentResponse comment);

    /** Called when a comment is deleted. */
    void deleted(CommentResponse comment);

    /** Called after a connection is closed (by the client or server). */
    void closed();

    /** Called in the event of a network error. No further calls to this listener will be made. */
    void networkError(Throwable t);
  }
}