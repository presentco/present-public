package present.acceptance;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.CommentResponse;
import present.proto.Coordinates;
import present.proto.GroupResponse;
import present.proto.GroupService;
import present.proto.MarkReadRequest;
import present.proto.PastCommentsRequest;
import present.proto.PastCommentsResponse;
import present.proto.PutCommentRequest;
import present.proto.PutGroupRequest;
import present.proto.JoinGroupRequest;
import present.proto.JoinedGroupsRequest;
import present.proto.JoinedGroupsResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static present.acceptance.AcceptanceTest.newUuid;
import static present.acceptance.AcceptanceTest.randomNumber;

public class UnreadTest {

  private static final Logger logger = LoggerFactory.getLogger(GroupTest.class);

  private AcceptanceTest tests;

  public UnreadTest(AcceptanceTest tests) {
    this.tests = tests;
  }

  public void run() throws IOException {
    GroupService groupService = tests.groupService;
    Coordinates location = tests.location;

    AcceptanceTest.TestUser alice = tests.signUp(new AcceptanceTest.TestUser("Alice", "Smith"));
    AcceptanceTest.TestUser bob = tests.signUp(new AcceptanceTest.TestUser("Bob", "Lee")); // Bob is the current user

    // Bob creates a new group and comments three times.
    final String groupTitle = "Group #" + randomNumber(9);
    String groupId = newUuid();
    groupService.putGroup(new PutGroupRequest.Builder().uuid(groupId)
        .location(location)
        .title(groupTitle)
        .createdFrom(location)
        .locationName("Present HQ")
        .build());
    for (int i = 0; i < 3; i++) {
      groupService.putComment(new PutCommentRequest.Builder().uuid(newUuid())
          .groupId(groupId)
          .comment("Comment #" + i)
          .build());
    }

    // Alice saves the group.
    tests.setCurrentUser(alice);
    groupService.joinGroup(new JoinGroupRequest(groupId, false));
    JoinedGroupsResponse savedGroups = groupService.getJoinedGroups(new JoinedGroupsRequest(null));
    GroupResponse groupResponse = savedGroups.groups.get(0);
    assertTrue(groupResponse.unread);

    // Alice reads the comments.
    PastCommentsResponse pastComments =
        groupService.getPastComments(new PastCommentsRequest(groupId));
    assertEquals(3, pastComments.comments.size());
    CommentResponse lastComment = pastComments.comments.get(0);
    groupService.markRead(new MarkReadRequest(groupId, lastComment.index));
    savedGroups = groupService.getJoinedGroups(new JoinedGroupsRequest(null));
    groupResponse = savedGroups.groups.get(0);
    assertFalse(groupResponse.unread);
  }
}
