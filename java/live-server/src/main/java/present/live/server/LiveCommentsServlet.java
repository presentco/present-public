package present.live.server;

import present.proto.LiveCommentsRequest;
import com.google.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.LiveCommentsResponse;

/**
 * Returns a websocket stream of comments on the given bubble.
 *
 * @author Bob Lee
 */
@Singleton
public class LiveCommentsServlet extends WebSocketServlet implements WebSocketCreator {

  /*
   * Keep all websocket-specific code in this class.
   */

  private static final Logger logger = LoggerFactory.getLogger(LiveCommentsServlet.class);

  private final CommentDispatcher messageDispatcher;

  @Inject public LiveCommentsServlet(CommentDispatcher messageDispatcher) {
    this.messageDispatcher = messageDispatcher;
  }

  @Override public void configure(WebSocketServletFactory factory) {
    factory.setCreator(this);
  }

  @Override public Object createWebSocket(ServletUpgradeRequest request,
      ServletUpgradeResponse response) {
    logger.debug("createWebSocket");
    return new Listener();
  }

  private static final ByteBuffer SUCCESSFUL_RESPONSE = ByteBuffer.wrap(new LiveCommentsResponse(
      LiveCommentsResponse.Status.READY).encode());

  class Listener implements WebSocketListener {

    private Session session;
    private RemoteClient client;

    @Override public void onWebSocketConnect(Session session) {
      logger.debug("onWebSocketConnect");
      this.session = session;
    }

    @Override public void onWebSocketBinary(byte[] payload, int offset, int len) {
      logger.debug("web socket binary");
      if (this.client != null) {
        logger.warn("Ignoring unexpected bytes from client.");
        return;
      }
      try {
        LiveCommentsRequest request = LiveCommentsRequest.ADAPTER.decode(
            new ByteArrayInputStream(payload, offset, len));

        this.client = new RemoteClient(request) {
          @Override public void send(ByteBuffer bytes) {
            // Duplicate the byte buffer——Jetty mutates it.
            session.getRemote().sendBytesByFuture(bytes.duplicate());
          }
        };

        // Synchronization ensures we send response before comments start coming in.
        synchronized (this.client) {
          messageDispatcher.register(client);

          // We send a response in version 1 and above.
          if (request.version > 0) {
            logger.info("Sending response");
            this.client.send(SUCCESSFUL_RESPONSE);
          }
        }
      } catch (IOException e) {
        logger.error("web socket exception: " + e);
        throw new AssertionError();
      }
    }

    @Override public void onWebSocketError(Throwable cause) {
      logger.error("onWebSocketError", cause);
      if (client != null) messageDispatcher.unregister(client);
    }

    @Override public void onWebSocketText(String message) {
      logger.error("onWebSocketText: "+message);
      throw new AssertionError();
    }

    @Override public void onWebSocketClose(int statusCode, String reason) {
      logger.debug("onWebSocketClose({}, {})", statusCode, reason);
      if (client != null) messageDispatcher.unregister(client);
    }
  }
}
