package present.acceptance;

import present.live.client.LiveClient;
import present.proto.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static present.acceptance.AcceptanceTest.*;
import static present.acceptance.AcceptanceTest.randomNumber;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class BlockUsersTest {
  private AcceptanceTest tests;

  public BlockUsersTest(AcceptanceTest tests) {
    this.tests = tests;
  }

  public void testBlockingUsers() throws IOException {
    GroupService groupService = tests.groupService;
    UserService userService = tests.userService;
    Coordinates location = present.server.model.util.Coordinates.SAN_FRANCISCO.toProto();

    // Sign up two users
    TestUser Alice = tests.signUp(new TestUser("Alice", "Smith"));
    TestUser Bob = tests.signUp(new TestUser("Bob", "Lee")); // Bob is the current user

    // Bob creates a group and posts a comment.
    final String groupTitle = "Group #" + randomNumber(9);
    groupService.putGroup(new PutGroupRequest.Builder().uuid(newUuid())
        .location(location)
        .title(groupTitle)
        .createdFrom(location)
        .locationName("Present HQ")
        .build());
    GroupResponse group = tests.findGroupByTitleExpected(groupTitle);
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(newUuid())
        .groupId(group.uuid)
        .comment("Hello, World!")
        .build());
    PastCommentsResponse comments = groupService.getPastComments(new PastCommentsRequest(group.uuid));
    assertEquals(comments.comments.size(), 1);

    // Alice sees the group and comment
    tests.setCurrentUser(Alice); // current user Alice
    GroupResponse aliceGroup = tests.findGroupByTitleExpected(groupTitle);
    PastCommentsResponse aliceComments = groupService.getPastComments(
        new PastCommentsRequest(aliceGroup.uuid));
    assertEquals(aliceComments.comments.size(), 1);

    // Alice has no users in her blocked user list
    assertEquals(0,userService.getBlockedUsers(new Empty()).users.size());

    // Alice blocks user Bob
    userService.blockUser(new UserRequest(Bob.getUserId(), null));

    // Alice sees Bob in her her blocked users list
    assertEquals(userService.getBlockedUsers(new Empty()).users.get(0).name, "Bob Lee");

    // Alice no longer sees the group owned by Bob.
    tests.setCurrentUser(Alice);
    tests.findGroupByTitleNotExpected(groupTitle);

    // Alice can no longer see the message from Bob within the group
    // (Cheating by fetching the group directly by id)
    PastCommentsResponse aliceCommentsAfterBlock = groupService.getPastComments(
        new PastCommentsRequest(aliceGroup.uuid));
    assertEquals(aliceCommentsAfterBlock.comments.size(), 0);

    // Alice un-blocks user Bob
    userService.unblockUser(new UserRequest(Bob.getUserId(), null));

    // Alice no longer sees Bob in her her blocked users list
    assertTrue(userService.getBlockedUsers(new Empty()).users.isEmpty());

    // Alice sees the group owned by Bob again.
    assertNotNull(tests.findGroupByTitleExpected(groupTitle));

    // TODO:
    // Test: Blocked user's direct message removal from three party chat.
    // Test: Blocked user's groups removed from favorited groups.
    // Test: Blocked user's messages removed from live server messaging.
  }

  public void testBlockingLiveServer() throws IOException, InterruptedException {
    GroupService groupService = tests.groupService;
    UserService userService = tests.userService;
    Coordinates location = present.server.model.util.Coordinates.SAN_FRANCISCO.toProto();

    // Sign up two users
    TestUser Alice = tests.signUp(new TestUser("Alice", "Smith"));
    TestUser Bob = tests.signUp(new TestUser("Bob", "Lee")); // Bob is the current user

    // Bob creates a group
    final String groupTitle = "Group #" + randomNumber(9);
    groupService.putGroup(new PutGroupRequest.Builder()
        .uuid(newUuid())
        .location(location)
        .title(groupTitle)
        .createdFrom(location)
        .locationName("Present HQ")
        .build());
    GroupResponse group = tests.findGroupByTitleExpected(groupTitle);

    // Alice Listens for live comments on the group
    tests.setCurrentUser(Alice);
    CommentListener aliceCommentListener = new CommentListener();
    LiveClient aliceLiveClient = new LiveClient(groupService, tests.newHeader(), Alice.getUserId(), group.uuid, aliceCommentListener);

    tests.waitFor(aliceCommentListener.readyLatch);

    // Bob posts to the group
    tests.setCurrentUser(Bob);
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(newUuid())
        .groupId(group.uuid)
        .comment("#1")
        .build());

    // Alice receives the live comment.
    tests.setCurrentUser(Alice); // not necessary for the poll, but switching back here
    CommentResponse message = aliceCommentListener.comments.poll(5, TimeUnit.SECONDS);
    assertNotNull(message);

    // Alice blocks user Bob
    userService.blockUser(new UserRequest(Bob.getUserId(), null));

    // Bob posts #2 to the group
    tests.setCurrentUser(Bob);
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(newUuid())
        .groupId(group.uuid)
        .comment("#2")
        .build());

    // Alice does not receive live comment #2
    tests.setCurrentUser(Alice); // not necessary for the poll, but switching back here
    CommentResponse message2 = aliceCommentListener.comments.poll(5, TimeUnit.SECONDS);
    assertNull(message2);

    // Alice un-blocks user Bob
    userService.unblockUser(new UserRequest(Bob.getUserId(), null));

    // Bob posts #3 to the group
    tests.setCurrentUser(Bob);
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(newUuid())
        .groupId(group.uuid)
        .comment("#3")
        .build());

    // Alice receives the live comment #3
    tests.setCurrentUser(Alice); // not necessary for the poll, but switching back here
    CommentResponse message3 = aliceCommentListener.comments.poll(5, TimeUnit.SECONDS);
    assertNotNull(message3);

    aliceLiveClient.close();
  }

}
