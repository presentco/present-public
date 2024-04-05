package present.server.model.group;

import com.google.appengine.repackaged.com.google.common.collect.ImmutableList;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import present.proto.Coordinates;
import present.proto.DeleteCommentRequest;
import present.proto.GroupService;
import present.proto.Platform;
import present.proto.PutCommentRequest;
import present.proto.PutGroupRequest;
import present.proto.RequestHeader;
import present.proto.UnreadState;
import present.server.AppEngineGroupService;
import present.server.RequestHeaders;
import present.server.Uuids;
import present.server.model.PresentEntities;
import present.server.model.Space;
import present.server.model.user.Client;
import present.server.model.user.Friendship;
import present.server.model.user.UnreadStates;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;
import present.server.notification.TestNotification;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;

public class CommentDeleteTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before public void setUp() {
    helper.setUp();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  @Test public void testDeleteComment() throws IOException {
    try (Closeable ignored = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      User user = newUser('A');
      Users.setCurrent(user);
      Coordinates location = present.server.model.util.Coordinates.SAN_FRANCISCO.toProto();

      RequestHeaders.setCurrent(new RequestHeader.Builder()
          .clientUuid(Uuids.NULL)
          .requestUuid(Uuids.NULL)
          .platform(Platform.TEST)
          .location(location)
          .authorizationKey("ignored")
          .apiVersion(1)
          .build());

      GroupService gs = new AppEngineGroupService();
      String groupId = Uuids.newUuid();
      gs.putGroup(new PutGroupRequest.Builder()
          .uuid(groupId)
          .title("Test Group")
          .location(location)
          .locationName("San Francisco")
          .createdFrom(location)
          .spaceId(Space.EVERYONE.id)
          .build());

      String commentA = Uuids.newUuid();
      gs.putComment(new PutCommentRequest(commentA, groupId, "Comment A", null, false));

      String commentB = Uuids.newUuid();
      gs.putComment(new PutCommentRequest(commentB, groupId, "Comment B", null, false));

      assertEquals(1, Groups.findByUuid(groupId).lastCommentIndex);

      gs.deleteComment(new DeleteCommentRequest(commentB));

      ofy().clear();

      assertEquals(0, Groups.findByUuid(groupId).lastCommentIndex);
    }
  }

  private User newUser(char c) {
    User user = new User();
    user.firstName = "User " + c;
    user.uuid = Uuids.repeat(c);
    user.state = UserState.MEMBER;
    user.save();
    Client client = new Client();
    client.uuid = user.uuid;
    client.user = user.getRef();
    client.platform = Platform.TEST.getValue();
    client.deviceToken = user.uuid;
    client.save();

    // Prime unread state so we don't try to compute it asynchronously.
    UnreadState state = new UnreadState(Collections.emptyList());
    UnreadStates.put(user, state);

    return user;
  }
}
