package present.live.server;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import okio.ByteString;
import org.eclipse.jetty.server.Server;
import org.junit.Test;
import present.jetty.Servers;
import present.live.client.InternalLiveClient;
import present.live.client.LiveClient;
import present.proto.CommentResponse;
import present.proto.ContentReferenceRequest;
import present.proto.DispatchCommentRequest;
import present.proto.FriendResponse;
import present.proto.LiveService;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.proto.UserResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Bob Lee (bob@present.co)
 */
public class LiveServerTest {

  private static final int PORT = Servers.randomPort();

  @Test public void testLiveComments() throws Exception {
    Server server = LiveServer.newInstance(PORT);
    server.start();

    for (int i = 0; i < 2; i++) {
      String clientId = UUID.randomUUID().toString();
      String groupId = UUID.randomUUID().toString();
      String commentId = UUID.randomUUID().toString();
      String commentString = "Hello, World!";
      UserResponse author = new UserResponse("id", "Bob Lee", "Bob",
          "https://goo.gl/E08v0o", " ", new ArrayList<String>(),
          new ArrayList<FriendResponse>(), null, "https://present.co", true);
      CommentResponse comment = new CommentResponse.Builder()
          .uuid(commentId)
          .groupId(groupId)
          .author(author)
          .creationTime(System.currentTimeMillis())
          .comment(commentString)
          .likes(0)
          .deleted(false)
          .index(0)
          .build();
      LiveService bs = InternalLiveClient.connectTo("https://local.present.co:" + PORT + "/api");
      CommentListener listener = new CommentListener();
      RequestHeader header =
          new RequestHeader(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "IGNORED",
              Platform.TEST, 1, "1", "1", null, null, "Test Device",
              "women-only");

      String userId = "1234";
      LiveClient client = new LiveClient("local.present.co", PORT, header, userId, groupId, listener);

      waitFor(listener.readyLatch);

      ByteString encodedFilter = null;
      bs.dispatchComment(new DispatchCommentRequest(clientId, groupId,
          ByteString.of(CommentResponse.ADAPTER.encode(comment)), encodedFilter));

      // Make sure it came out the other side.
      CommentResponse actual = listener.comments.poll(5, TimeUnit.SECONDS);
      assertNotNull(actual);
      assertEquals(comment, actual);

      client.close();
      waitFor(listener.closeLatch);
    }

    server.stop();
  }

  private void waitFor(CountDownLatch latch) throws InterruptedException {
    latch.await(5, TimeUnit.SECONDS);
    assertEquals(0, latch.getCount());
  }

  private class CommentListener implements LiveClient.Listener {

    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private final BlockingDeque<CommentResponse> comments = new LinkedBlockingDeque<>();

    @Override public void comment(CommentResponse comment) {
      comments.add(comment);
    }
    @Override public void deleted(CommentResponse comment) { throw new AssertionError(); }
    @Override public void ready() {
      readyLatch.countDown();
    }
    @Override public void closed() {
      closeLatch.countDown();
    }
    @Override public void networkError(Throwable t) {
      throw new AssertionError(t);
    }
  }
}
