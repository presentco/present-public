package present.live.server;

import java.nio.ByteBuffer;
import present.proto.DispatchCommentRequest;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.BloomFilter;
import java.util.Set;
import javax.inject.Singleton;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches comments in real time.
 *
 * @author Bob Lee
 */
@Singleton public class CommentDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(CommentDispatcher.class);

  final LoadingCache<String, Set<RemoteClient>> groups =
      CacheBuilder.newBuilder().build(new CacheLoader<String, Set<RemoteClient>>() {
        @Override public Set<RemoteClient> load(String groupId) throws Exception {
          return new ConcurrentHashSet<>();
        }
      });

  /** Sends a message to clients if the message is within their groups. */
  public void dispatch(DispatchCommentRequest request) {
    Set<RemoteClient> clients = groups.getUnchecked(request.groupId);
    if (clients.isEmpty()) {
      logger.debug("comment dispatcher found no clients for group: " + request.groupId);
      return;
    }
    logger.debug("comment dispatcher sending to " + clients.size() + " clients");
    BloomFilter<String> blockedUsersFilter =
        BloomFilters.fromByteString(request.blockedUsersFilter);
    ByteBuffer commentBytes = request.comment.asByteBuffer();
    for (RemoteClient client : clients) {
      // Don't send the comment back to the originating client
      if (client.clientUuid().equals(request.clientUuid)) {
        continue;
      }
      // Don't send the comment to users who have blocked this user.
      if (blockedUsersFilter != null && blockedUsersFilter.mightContain(client.clientUserId())) {
        continue;
      }
      // Synchronizing on client ensures we send the connection response before comments.
      synchronized (client) {
        logger.info("Sending comment");
        client.send(commentBytes);
      }
    }
  }

  /** Registers a new client. */
  public void register(RemoteClient client) {
    logger.debug("comment dispatcher registering: " + client.groupId());
    Set<RemoteClient> clients = groups.getUnchecked(client.groupId());
    clients.add(client);
  }

  /** Deregisters a client. */
  public void unregister(RemoteClient client) {
    logger.debug("comment dispatcher unregistering: " + client.groupId());
    Set<RemoteClient> clients = groups.getUnchecked(client.groupId());
    clients.remove(client);
  }
}


