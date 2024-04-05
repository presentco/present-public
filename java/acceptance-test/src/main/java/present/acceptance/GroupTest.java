package present.acceptance;

import com.google.common.collect.MoreCollectors;
import com.googlecode.objectify.Key;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.live.client.LiveClient;
import present.proto.ActivityType;
import present.proto.CommentRequest;
import present.proto.CommentResponse;
import present.proto.ContentResponse;
import present.proto.Coordinates;
import present.proto.DeleteCommentRequest;
import present.proto.DeleteGroupRequest;
import present.proto.EventResponse;
import present.proto.ExploreHtmlRequest;
import present.proto.FeedHtmlRequest;
import present.proto.FindLiveServerRequest;
import present.proto.GroupMembersRequest;
import present.proto.GroupResponse;
import present.proto.GroupService;
import present.proto.InviteFriendsRequest;
import present.proto.JoinGroupRequest;
import present.proto.JoinedGroupsRequest;
import present.proto.JoinedGroupsResponse;
import present.proto.LeaveGroupRequest;
import present.proto.MuteGroupRequest;
import present.proto.NearbyGroupsRequest;
import present.proto.NearbyGroupsResponse;
import present.proto.PastActivityRequest;
import present.proto.PastActivityResponse;
import present.proto.PastCommentsRequest;
import present.proto.PastCommentsResponse;
import present.proto.PutCommentRequest;
import present.proto.PutGroupRequest;
import present.proto.PutUserPhotoRequest;
import present.proto.ResolveUrlRequest;
import present.proto.ResolveUrlResponse;
import present.proto.UserService;
import present.server.model.Space;
import present.server.model.activity.GroupReferrals;
import present.server.model.comment.Comment;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;
import present.server.notification.TestNotification;
import present.wire.rpc.core.ClientException;
import present.wire.rpc.core.ServerException;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static present.acceptance.AcceptanceTest.CommentListener;
import static present.acceptance.AcceptanceTest.JPG1x1Bytes;
import static present.acceptance.AcceptanceTest.TestUser;
import static present.acceptance.AcceptanceTest.UNREGISTERED_USER;
import static present.acceptance.AcceptanceTest.newUuid;
import static present.acceptance.AcceptanceTest.randomNumber;
import static present.acceptance.AcceptanceTest.urlBytes;

/**
 * @author Bob Lee
 * @author Pat Niemeyer
 */
public class GroupTest {
  private static final Logger logger = LoggerFactory.getLogger(GroupTest.class);

  private AcceptanceTest tests;

  public GroupTest(AcceptanceTest tests) {
    this.tests = tests;
  }

  public void testGroups() throws IOException, InterruptedException {
    testGroupUsage();
    testLoggedOut();
  }

  public void testGroupUsage() throws IOException, InterruptedException {
    GroupService groupService = tests.groupService;
    UserService userService = tests.userService;
    Coordinates location = tests.location;

    TestUser alice = tests.signUp(new TestUser("Alice", "Smith"));
    TestUser bob = tests.signUp(new TestUser("Bob", "Lee")); // Bob is the current user

    // Delete test comments
    List<Key<Comment>> commentKeys = ofy().load().type(Comment.class).keys().list();
    ofy().delete().keys(commentKeys).now();

    // Bob creates a new group.
    final String bobGroupTitle1 = "Group #" + randomNumber(9);
    groupService.putGroup(new PutGroupRequest.Builder().uuid(newUuid())
        .location(location)
        .title(bobGroupTitle1)
        .createdFrom(location)
        .locationName("Present HQ")
        .build());
    GroupResponse bobGroup1 = tests.findGroupByTitleExpected(bobGroupTitle1);
    assertNotNull(bobGroup1);
    assertEquals(0, bobGroup1.commentCount.intValue());
    assertEquals(bobGroupTitle1, bobGroup1.title);
    assertEquals(location, bobGroup1.location);
    assertEquals("Bob Lee", bobGroup1.owner.name);
    assertEquals(bob.getUserId(), bobGroup1.owner.id);
    assertEquals(1, bobGroup1.memberCount.intValue()); // Bob's group was auto-joined when he created it.

    // Bob invites Alice to the group.
    groupService.inviteFriends(
        new InviteFriendsRequest(bobGroup1.uuid, Collections.singletonList(alice.getUserId())));

    // Alice saves Bob's group
    tests.setCurrentUser(alice);

    // Alice received a notification.
    //tests.notificationsClient.expectNotification(ExpectedNotification.toUser(alice)
    //        .containingText(bobGroupTitle1)
    //        .hasPayload(PayloadNames.GROUP_ID));
    //tests.notificationsClient.clearNotifications();

    // Alice has an activity event.
    PastActivityResponse aliceActivity = tests.activityService.getPastActivity(new PastActivityRequest(null, null));
    aliceActivity.events.stream()
        .filter(e -> e.type.equals(ActivityType.USER_INVITED_TO_GROUP))
        .filter(e -> e.defaultTarget.group.uuid.equals(bobGroup1.uuid))
        .filter(e -> e.summary.contains(bobGroupTitle1))
        .collect(MoreCollectors.onlyElement());

    groupService.joinGroup(new JoinGroupRequest(bobGroup1.uuid, false));

    // Bob creates and deletes a second group
    tests.setCurrentUser(bob);
    final String deletedGroupTitle = "Group #" + randomNumber(9);
    groupService.putGroup(new PutGroupRequest.Builder().uuid(newUuid())
        .location(location)
        .title(deletedGroupTitle)
        .createdFrom(location)
        .locationName("HQ2")
        .build());
    GroupResponse deletedGroup = tests.findGroupByTitleExpected(deletedGroupTitle);
    assertNotNull(deletedGroup);
    groupService.deleteGroup(new DeleteGroupRequest(deletedGroup.uuid));
    tests.findGroupByTitleNotExpected(deletedGroupTitle);

    // Bob is unable to create a group with invalid categories.
    final String invalidCategoryGroup = "Group #" + randomNumber(9);
    final List<String> invalidCategories = Arrays.asList("fake", "no good", "Food & Drink");
    PutGroupRequest groupRequest = new PutGroupRequest.Builder()
        .uuid(newUuid())
        .location(location)
        .title(invalidCategoryGroup)
        .createdFrom(location)
        .locationName("HQ2.5")
        .categories(invalidCategories)
        .build();
    try {
      groupService.putGroup(groupRequest);
      Assert.fail();
    } catch (ServerException e) {
      assertTrue(e.getMessage().contains("Invalid category: fake"));
    }
    tests.findGroupByTitleNotExpected(invalidCategoryGroup);

    // Alice attempts to join the deleted group without ignoring deleted groups.
    // An exception is thrown.
    tests.setCurrentUser(alice);
    try {
      groupService.joinGroup(new JoinGroupRequest(deletedGroup.uuid, false));
      Assert.fail();
    } catch (ClientException e) {
      /* Expected */
    }

    // Alice attempts to join the deleted group while ignoring deleted groups.
    // An exception is not thrown.
    groupService.joinGroup(new JoinGroupRequest(deletedGroup.uuid, true));

    // Alice creates a group
    final String aliceGroupTitle1 = "Group #" + randomNumber(9);
    groupService.putGroup(new PutGroupRequest.Builder().uuid(newUuid())
        .location(location)
        .title(aliceGroupTitle1)
        .createdFrom(location)
        .locationName("HQ3")
        .build());

    // Alice creates a second group
    tests.setCurrentUser(alice);
    final String aliceGroupTitle2 = "Group #" + randomNumber(9);
    groupService.putGroup(new PutGroupRequest.Builder().uuid(newUuid())
        .location(location)
        .title(aliceGroupTitle2)
        .createdFrom(location)
        .locationName("HQ4")
        .build());

    // Bob joins (saves/favorites) Alice's first group.
    tests.setCurrentUser(bob);
    GroupResponse aliceGroup = tests.findGroupByTitleExpected(aliceGroupTitle1);
    JoinedGroupsResponse savedGroups = groupService.getJoinedGroups(new JoinedGroupsRequest(null));
    assertEquals(1, savedGroups.groups.size()); // Bob had one auto-joined group
    groupService.joinGroup(new JoinGroupRequest(aliceGroup.uuid, false)); // Bob joins Alice's group
    savedGroups = groupService.getJoinedGroups(new JoinedGroupsRequest(null)); // Bob's joined groups
    aliceGroup = tests.findGroupByTitleExpected(aliceGroupTitle1);
    assertEquals(2, savedGroups.groups.size()); // Bob has two saved groups now
    assertEquals(2, aliceGroup.memberCount.intValue()); // The Alice group has two favorites now
    assertEquals(2, groupService.getGroupMembers(
        new GroupMembersRequest(aliceGroup.uuid)).members.size()); //  Alice's group has two members now

    // TODO: Fix.
    // Alice gets a notification that Bob has joined her first group
    //tests.notificationsClient.expectNotification(toUser(alice).containingText("Bob"));

    // Bob posts a photo comment to Alice's group
    tests.setCurrentUser(bob); // Switch back to Bob
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(newUuid())
        .groupId(aliceGroup.uuid)
        .comment("Hello, World!")
        .content(tests.getJPG1x1ReferenceRequest())
        .build());

    // Alice's activity feed shows Bob joining and posting to her group
    tests.setCurrentUser(alice);
    aliceActivity = tests.activityService.getPastActivity(new PastActivityRequest(null, null));
    //for (EventResponse event : aliceActivity.events) { System.out.println("event = " + event); }

    // Alice receives a notification for Bob's comment.
    TestNotification.to(alice.client()).stream()
        .filter(n -> n.body.matches("^Bob.*image.*"))
        .findFirst()
        .get();

    // Events are returned in descending order by time (latest first):
    // : Bob joined Alice's group
    String aliceGroupId = aliceGroup.uuid;
    aliceActivity.events.stream()
        .filter(e -> e.type.equals(ActivityType.USER_JOINED_GROUP))
        .filter(e -> e.defaultTarget.group.uuid.equals(aliceGroupId))
        .collect(MoreCollectors.onlyElement());
    // : Bob commented on Alice's group
    aliceActivity.events.stream()
        .filter(e -> e.type.equals(ActivityType.USER_COMMENTED_ON_GROUP))
        .filter(e -> e.defaultTarget.comment.groupId.equals(aliceGroupId))
        .collect(MoreCollectors.onlyElement());

    // : There were three events
    assertEquals(3, aliceActivity.events.size());

    // Bob un-joins (un-saves/un-favorites) Alice's group
    tests.setCurrentUser(bob);
    groupService.leaveGroup(new LeaveGroupRequest(aliceGroup.uuid));
    savedGroups = groupService.getJoinedGroups(new JoinedGroupsRequest(null));
    assertEquals(1, savedGroups.groups.size()); // Bob is back to one saved
    aliceGroup = tests.findGroupByTitleExpected(aliceGroupTitle1);
    assertEquals(1, aliceGroup.memberCount.intValue()); // Alice's group is back to 1 favorite

    // Listens for live comments.
    tests.setCurrentUser(alice); // Listen as Alice (client's don't receive their own comments back).
    CommentListener commentListener = new CommentListener();
    LiveClient liveClient = new LiveClient(groupService, tests.newHeader(),
        tests.getCurrentUser().getUserId(), bobGroup1.uuid, commentListener);

    tests.waitFor(commentListener.readyLatch);

    // Bob posts a comment to his group.
    tests.setCurrentUser(bob); // Switch back to Bob
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(newUuid())
        .groupId(bobGroup1.uuid)
        .comment("Hello, World!")
        .content(tests.getJPG1x1ReferenceRequest())
        .build());
    PastCommentsResponse comments = groupService.getPastComments(
        new PastCommentsRequest(bobGroup1.uuid));
    CommentResponse comment = comments.comments.get(0);
    assertEquals("Bob Lee", comment.author.name);
    assertEquals("Hello, World!", comment.comment);
    assertTrue(comment.creationTime > 0);
    assertEquals(bobGroup1.owner.id, comment.author.id);

    // Alice received a second notification.
    assertEquals(2, TestNotification.to(alice.client()).stream()
        .filter(n -> n.body.matches("^Bob.*image.*"))
        .count());

    // Retrieve a comment by id if needed
    CommentResponse commentRetrieved = groupService.getComment(new CommentRequest(comment.uuid));
    assertNotNull(commentRetrieved);
    assertEquals(commentRetrieved.comment, comment.comment);

    // Bob does not see activity for his own actions
    tests.setCurrentUser(bob);
    PastActivityResponse bobActivity = tests.activityService.getPastActivity(new PastActivityRequest(null, null));
    for (EventResponse event : bobActivity.events) {
      assertFalse("Bob does not see his own activity", event.summary.contains("Bob Lee"));
    }

    // Check the comment count.
    tests.retry(() -> {
      GroupResponse b = tests.findGroupByTitleExpected(bobGroupTitle1);
      assertEquals(1, b.commentCount.intValue());
      assertEquals(comment.creationTime, b.lastCommentTime, TimeUnit.SECONDS.toMillis(10));
      return b;
    });

    // Make sure we received the live comment.
    CommentResponse actual = commentListener.comments.poll(5, TimeUnit.SECONDS);
    assertNotNull(actual);
    assertEquals(comment, actual);
    assertEquals(JPG1x1Bytes, urlBytes(comment.content.content));

    // Bob deletes his comment.
    groupService.deleteComment(new DeleteCommentRequest(comment.uuid));
    comments = groupService.getPastComments(new PastCommentsRequest(bobGroup1.uuid));
    assertEquals(0, comments.comments.size());
    CommentResponse deletedComment = commentListener.deleted.poll(5, TimeUnit.SECONDS);
    assertNotNull(deletedComment);
    assertTrue(deletedComment.deleted);
    assertNull(deletedComment.content);
    assertEquals("*deleted*", deletedComment.comment);
    assertEquals("Bob Lee", deletedComment.author.name);
    assertTrue(deletedComment.creationTime > 0);
    assertEquals(bobGroup1.owner.id, deletedComment.author.id);

    // Test profile photo.
    tests.setCurrentUser(bob); // Switch back to Bob
    ContentResponse photoResponse = userService.putUserPhoto(
        new PutUserPhotoRequest(tests.getJPG1x1ReferenceRequest()));
    assertEquals(JPG1x1Bytes, urlBytes(photoResponse.content));

    // Bob Updates his group, changing its location
    tests.setCurrentUser(bob);
    Coordinates newLocation = new Coordinates( bobGroup1.location.latitude+0.1, bobGroup1.location.longitude+0.1, 0.0);
    // Same uuid and other data
    groupService.putGroup(new PutGroupRequest.Builder().uuid(bobGroup1.uuid)
        .location(newLocation)
        .title(bobGroup1.title)
        .createdFrom(bobGroup1.location)
        .locationName(bobGroup1.locationName)
        .build());
    GroupResponse bobGroup1Updated = tests.findGroupByTitleExpected(bobGroup1.title); // Also guarantees unique
    assertNotNull(bobGroup1Updated);
    assertEquals(newLocation, bobGroup1Updated.location); // Location has been updated
    assertEquals(bobGroupTitle1, bobGroup1Updated.title);
    assertEquals(bob.getUserId(), bobGroup1Updated.owner.id);
    assertEquals(2, bobGroup1Updated.memberCount.intValue());
    assertEquals(0, bobGroup1Updated.commentCount.intValue());

    liveClient.close();

    // Alice mutes a group
    tests.setCurrentUser(alice);
    Key<Group> aliceGroupKey = Key.create(Group.class, aliceGroup.uuid);
    assertNull(Group.get(aliceGroupKey).getView(alice.user()));
    groupService.muteGroup(new MuteGroupRequest(aliceGroup.uuid));
    ofy().clear();
    assertTrue(Group.get(aliceGroupKey).getView(alice.user()).muted);

    // TODO: Test that Alice doesn't receive a notification.

    // Alice shares a link to Bob's group.
    GroupResponse bobsGroupForAlice = tests.findGroupByTitleExpected(bobGroupTitle1);
    ResolveUrlResponse resolveUrlResponse =
        tests.urlResolverService.resolveUrl(new ResolveUrlRequest(bobsGroupForAlice.url));
    tests.retry(() -> {
      // Alice's resolution of her own referral link is not counted.
      assertEquals(GroupReferrals.countReferrals(User.get(alice.getUserId())), 0);
    });
    // Bob resolves the URL.
    tests.setCurrentUser(bob);
    resolveUrlResponse =
        tests.urlResolverService.resolveUrl(new ResolveUrlRequest(bobsGroupForAlice.url));
    assertEquals(bobsGroupForAlice.uuid, resolveUrlResponse.group.uuid);
    assertEquals(alice.getUserId(), resolveUrlResponse.referrer.id);
    tests.retry(() -> {
      // Alice's share is not logged for Bob since he owns the group.
      assertEquals(0, GroupReferrals.countReferrals(User.get(alice.getUserId())));
    });

    // A user who isn't logged in can resolve the URL.
    resolveUrlResponse =
        tests.urlResolverService.resolveUrl(new ResolveUrlRequest(bobsGroupForAlice.url));
    tests.setCurrentUser(UNREGISTERED_USER);
    assertEquals(bobsGroupForAlice.uuid, resolveUrlResponse.group.uuid);
    assertEquals(alice.getUserId(), resolveUrlResponse.referrer.id);
    // Now resolve the URL returned to that user.
    resolveUrlResponse =
        tests.urlResolverService.resolveUrl(new ResolveUrlRequest(resolveUrlResponse.group.url));
    assertEquals(bobsGroupForAlice.uuid, resolveUrlResponse.group.uuid);

    // Alice shares a link to her group.
    tests.setCurrentUser(alice);
    aliceGroup = tests.findGroupByTitleExpected(aliceGroupTitle1);
    // Bob resolves the URL.
    tests.setCurrentUser(bob);
    resolveUrlResponse =
        tests.urlResolverService.resolveUrl(new ResolveUrlRequest(aliceGroup.url));
    // Bob joins Alice's group from her referral
    tests.setCurrentUser(bob);
    groupService.joinGroup(new JoinGroupRequest(aliceGroup.uuid, false)); // Bob joins Alice's group
    tests.retry(() -> {
      // Alice's share/Bob's join is logged
      assertEquals(1, GroupReferrals.countReferrals(User.get(alice.getUserId())));
    });

    // Alice shares a link to another group.
    tests.setCurrentUser(alice);
    aliceGroup = tests.findGroupByTitleExpected(aliceGroupTitle2);
    // Bob resolves the URL.
    tests.setCurrentUser(bob);
    resolveUrlResponse =
        tests.urlResolverService.resolveUrl(new ResolveUrlRequest(aliceGroup.url));
    // Bob joins Alice's group from her referral
    tests.setCurrentUser(bob);
    groupService.joinGroup(new JoinGroupRequest(aliceGroup.uuid, false)); // Bob joins Alice's group
    tests.retry(() -> {
      // Alice's share/Bob's second join is logged
      assertEquals(2, GroupReferrals.countReferrals(User.get(alice.getUserId())));
    });

    // Alice posts a short comment, which should never be set as significant.
    String shortCommentText = "Short";
    String shortCommentUuid = newUuid();
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(shortCommentUuid)
        .groupId(bobGroup1.uuid)
        .comment(shortCommentText)
        .build());
    // The short comment is not set as significant.
    Group bobGroup1WithSignificantComment = Groups.findByUuid(bobGroup1.uuid);
    assertNull(bobGroup1WithSignificantComment.lastSignificantComment);

    // Bob posts a comment.
    tests.setCurrentUser(bob);
    String bobCommentText = "This comment will eventually be deleted. Here is a number: " + randomNumber(9);
    String bobCommentUuid = newUuid();
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(bobCommentUuid)
        .groupId(bobGroup1.uuid)
        .comment(bobCommentText)
        .build());
    // The new comment is set as significant.
    ofy().clear();
    bobGroup1WithSignificantComment = Groups.findByUuid(bobGroup1.uuid);
    // A significant comment still exists
    assertNotNull(bobGroup1WithSignificantComment.lastSignificantComment);
    assertEquals(bobCommentText, bobGroup1WithSignificantComment.lastSignificantComment.get().text);
    assertEquals(bobCommentUuid, bobGroup1WithSignificantComment.lastSignificantComment.get().uuid);

    // Alice sees the comment activity.
    tests.setCurrentUser(alice);
    aliceActivity = tests.activityService.getPastActivity(new PastActivityRequest(null, null));
    List<EventResponse> events = aliceActivity.events.stream()
        .filter(e -> e.type.equals(ActivityType.USER_COMMENTED_ON_GROUP))
        .filter(e -> e.defaultTarget.comment.uuid.equals(bobCommentUuid))
        .collect(Collectors.toList());
    assertEquals(1, events.size());

    // Alice posts a comment.
    String aliceCommentText = "Comment is unique. Here is a number: " + randomNumber(9);
    String aliceCommentUuid = newUuid();
    String duplicateCommentUuid = newUuid();
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(aliceCommentUuid)
        .groupId(bobGroup1.uuid)
        .comment(aliceCommentText)
        .build());
    // The new comment is set as significant.
    ofy().clear();
    bobGroup1WithSignificantComment = Groups.findByUuid(bobGroup1.uuid);
    // A significant comment still exists
    assertNotNull(bobGroup1WithSignificantComment.lastSignificantComment);
    assertEquals(aliceCommentText, bobGroup1WithSignificantComment.lastSignificantComment.get().text);
    assertEquals(aliceCommentUuid, bobGroup1WithSignificantComment.lastSignificantComment.get().uuid);

    // Alice posts a comment with the same text in a different group.
    groupService.putComment(new PutCommentRequest.Builder()
        .uuid(duplicateCommentUuid)
        .groupId(aliceGroup.uuid)
        .comment(aliceCommentText)
        .build());
    // The duplicate comment is not set as significant.
    Group aliceGroupWithSignificantComment = Groups.findByUuid(aliceGroup.uuid);
    assertNull(aliceGroupWithSignificantComment.lastSignificantComment);

    // Alice deletes her comment.
    groupService.deleteComment(new DeleteCommentRequest(aliceCommentUuid));
    // The comment is no longer significant.
    ofy().clear();
    bobGroup1WithSignificantComment = Groups.findByUuid(bobGroup1.uuid);
    // A significant comment still exists
    assertNotNull(bobGroup1WithSignificantComment.lastSignificantComment);
    assertNotEquals(bobGroup1WithSignificantComment.lastSignificantComment.get().text, aliceCommentText);
    assertNotEquals(bobGroup1WithSignificantComment.lastSignificantComment.get().uuid, aliceCommentUuid);
    // The significant comment is now Bob's comment.
    assertEquals(bobGroup1WithSignificantComment.lastSignificantComment.get().text, bobCommentText);
    assertEquals(bobGroup1WithSignificantComment.lastSignificantComment.get().uuid, bobCommentUuid);

    // Bob deletes his comment
    tests.setCurrentUser(bob);
    groupService.deleteComment(new DeleteCommentRequest(bobCommentUuid));
    // The comment is no longer significant. There are no other comments to be set as significant.
    ofy().clear();
    bobGroup1WithSignificantComment = Groups.findByUuid(bobGroup1.uuid);
    assertNull(bobGroup1WithSignificantComment.lastSignificantComment);

    // Alice no longer sees comment activity for that comment
    tests.setCurrentUser(alice);
    aliceActivity = tests.activityService.getPastActivity(new PastActivityRequest(null, null));
    List<EventResponse> eventResponses = aliceActivity.events.stream()
        .filter(e -> e.type.equals(ActivityType.USER_COMMENTED_ON_GROUP))
        .filter(e -> e.defaultTarget.comment.uuid.equals(bobCommentUuid))
        .collect(Collectors.toList());
    assertEquals(0, eventResponses.size());

    // Delete test groups.
    List<Key<Group>> groupKeys = ofy().load().type(Group.class).keys().list();
    ofy().delete().keys(groupKeys).now();

    // Delete test comments
    List<Key<Comment>> commentKeys2 = ofy().load().type(Comment.class).keys().list();
    ofy().delete().keys(commentKeys2).now();

    logger.info("Deleted {} groups.", groupKeys.size());
  }

  private void testLoggedOut() throws IOException {
    // Create a public group and post a comment.
    tests.signUp(new TestUser("Bob", "Lee")); // Bob is the current user
    final String title = "Group #" + randomNumber(9);
    String groupId = newUuid();
    tests.groupService.putGroup(new PutGroupRequest.Builder()
        .uuid(groupId)
        .spaceId(Space.EVERYONE.id)
        .location(tests.location)
        .title(title)
        .createdFrom(tests.location)
        .locationName("Present HQ")
        .build());
    tests.groupService.putComment(new PutCommentRequest.Builder()
        .uuid(newUuid())
        .groupId(groupId)
        .comment("Hello, World!")
        .build());

    // Log out.
    tests.setCurrentUser(AcceptanceTest.UNREGISTERED_USER);
    NearbyGroupsResponse nearbyGroups = tests.groupService.getNearbyGroups(new NearbyGroupsRequest(
        present.server.model.util.Coordinates.SAN_FRANCISCO.toProto(), null));
    tests.groupService.getPastComments(new PastCommentsRequest(groupId));
    tests.groupService.findLiveServer(new FindLiveServerRequest(groupId));
    tests.groupService.getFeedHtml(new FeedHtmlRequest(null));
    tests.groupService.getExploreHtml(new ExploreHtmlRequest(null));
  }
}

