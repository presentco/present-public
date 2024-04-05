package present.acceptance;

import com.google.common.collect.ImmutableSet;
import com.googlecode.objectify.Key;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.junit.Ignore;
import present.proto.Authorization;
import present.proto.AuthorizationResponse;
import present.proto.Empty;
import present.proto.Gender;
import present.proto.LinkFacebookRequest;
import present.proto.PutDeviceTokenRequest;
import present.proto.PutUserPhotoRequest;
import present.proto.RequestVerificationRequest;
import present.proto.SynchronizeRequest;
import present.proto.SynchronizeResponse;
import present.proto.UserName;
import present.proto.UserProfile;
import present.proto.UserProfileRequest;
import present.proto.UserService;
import present.proto.VerifyRequest;
import present.server.Protos;
import present.server.environment.Environment;
import present.server.facebook.FacebookUserData;
import present.server.model.console.whitelist.Whitelist;
import present.server.model.user.BlockedUsers;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;
import present.server.model.user.VerificationRequest;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static present.acceptance.AcceptanceTest.TestUser;
import static present.acceptance.AcceptanceTest.randomNumber;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
public class SignupTest {
  private AcceptanceTest tests;
  private UserService us;

  public SignupTest(AcceptanceTest tests) {
    this.tests = tests;
    this.us = tests.userService;
  }

  public void testSignup() throws IOException {
    testPhoneThenFacebook();
    testTwoClients();
    testNeedsReview();
    testPreapproval();
    testAdminApproveMembership();
    testCopyProfilePhoto();
    testFriendJoins();
    testCascadingDelete();
  }

  private void testFriendJoins() throws IOException {
    TestUser janete = new TestUser("Janete", "Perez");
    tests.signUp(janete);

    // Don't use "Pat." He gets filtered out as a male admin.
    TestUser pat = new TestUser("Patricia", "Niemeyer");
    tests.signUp(pat);

    Set<TestUser> friends = ImmutableSet.of(janete, pat);

    // Bob, friends with Janete and Pat, signs up.
    TestUser bob = new TestUser("Bob", "Lee");
    tests.setCurrentUser(bob);
    FacebookUserData fb = tests.newFacebookDataFor(bob);
    fb.friends.data = new FacebookUserData.Friends.Friend[] {
        janete.toFacebookFriend(), pat.toFacebookFriend() };
    ofy().save().entity(fb).now();
    UserService us = tests.userService;
    us.linkFacebook(new LinkFacebookRequest("not used", bob.facebookId));
    us.putUserProfile(new UserProfileRequest(null,
        null, null, Collections.emptyList(), bob.zip, null));
    bob.userProfile = us.getUserProfile(new Empty());
    us.completeSignup(new Empty());

    // Janete and Pat have activity events and notifications.
    // TODO: We don't send notifs when Facebook friends join.
    //for (TestUser friend : friends) {
    //  tests.setCurrentUser(friend);
    //
    //  PastActivityResponse
    //      activity = tests.activityService.getPastActivity(new PastActivityRequest(null, null));
    //  activity.events.stream()
    //      .filter(e -> e.type.equals(ActivityType.FRIEND_JOINED_PRESENT))
    //      .filter(e -> e.defaultTarget.user.id.equals(bob.getUserId()))
    //      .filter(e -> e.summary.contains("Your friend Bob"))
    //      .collect(MoreCollectors.onlyElement());
    //
    //  // TODO: Fix.
    //  tests.notificationsClient.expectNotification(ExpectedNotification.toUser(friend)
    //      .containingText("Your friend Bob")
    //      .hasPayload(PayloadNames.USER));
    //}
  }

  private void testPreapproval() throws IOException {
    TestUser user = new TestUser("Pre", "Approved");
    user.zip = null; // Not whitelisted
    tests.setCurrentUser(user);
    us.requestVerification(new RequestVerificationRequest(user.phone));
    assertEquals(Authorization.NextStep.SIGN_UP,
        us.verify(new VerifyRequest(null, VerificationRequest.TEST_CODE)).authorization.nextStep);
    user.userProfile = us.getUserProfile(new Empty());
    User serverUser = User.get(user.getUserId());
    assertEquals(UserState.SIGNING_UP, serverUser.state);
    serverUser.state = UserState.PRE_APPROVED;
    serverUser.save().now();
    assertEquals(Authorization.NextStep.PROCEED,
        us.completeSignup(new Empty()).authorization.nextStep);
    ofy().clear(); // Don't pull user from the session cache.
    serverUser = User.get(user.getUserId());
    assertEquals(UserState.MEMBER, serverUser.state);
  }

  private static final SynchronizeRequest synchronizeRequest
      = new SynchronizeRequest(null);

  private void testAdminApproveMembership() throws IOException {
    // TODO: Use location outside geofence.

    //TestUser user = new TestUser("Post", "Approved");
    //user.zip = null; // Not whitelisted
    //tests.setCurrentUser(user);
    //us.requestVerification(new RequestVerificationRequest(user.phone));
    //assertEquals(Authorization.NextStep.SIGN_UP,
    //    us.verify(new VerifyRequest(null, VerificationRequest.TEST_CODE)).authorization.nextStep);
    //us.putDeviceToken(new PutDeviceTokenRequest(user.deviceToken, null));
    //user.userProfile = us.getUserProfile(new Empty());
    //assertEquals(Authorization.NextStep.BLOCK,
    //    us.completeSignup(new Empty()).authorization.nextStep);
    //us.putUserProfile(new UserProfileRequest(null, null, null,
    //    Collections.emptyList(), Whitelist.TEST_ZIP, null));
    //// It would be nice to hit the admin console, but it can't access the "test" namespace.
    //User serverUser = User.get(user.getUserId());
    //// Make them a member.
    //serverUser.state = UserState.MEMBER;
    //serverUser.save().now();
    //User admin = new FakeUser();
    //admin.state = UserState.MEMBER;
    //Users.setCurrent(admin);
  }

  private void testTwoClients() throws IOException {
    TestUser alice = new TestUser("Alice", "Smith"); // Alice is the current user
    tests.setCurrentUser(alice);
    SynchronizeResponse sr = us.synchronize(synchronizeRequest);
    assertEquals(Authorization.NextStep.AUTHENTICATE, sr.authorization.nextStep);
    assertNull(sr.userProfile);
    us.requestVerification(new RequestVerificationRequest(alice.phone));
    AuthorizationResponse lfr = us.verify(new VerifyRequest(null,
        VerificationRequest.TEST_CODE));
    assertEquals(Authorization.NextStep.SIGN_UP, lfr.authorization.nextStep);
    UserProfile userProfile = us.getUserProfile(new Empty());
    assertEquals("", userProfile.name.first);
    assertEquals("", userProfile.name.last);
    assertEquals(userProfile.id, lfr.userProfile.id);
    sr = us.synchronize(synchronizeRequest);
    assertEquals(Authorization.NextStep.SIGN_UP, sr.authorization.nextStep);
    us.putUserProfile(new UserProfileRequest(new UserName(alice.firstName, alice.lastName),
        null, null, Collections.emptyList(), alice.zip, null));
    // This makes the user a member.
    AuthorizationResponse ar = us.completeSignup(new Empty());
    assertEquals(Authorization.NextStep.PROCEED, ar.authorization.nextStep);
    assertEquals(new UserName("Alice", "Smith"), ar.userProfile.name);
    sr = us.synchronize(synchronizeRequest);
    assertEquals(Authorization.NextStep.PROCEED, sr.authorization.nextStep);
    assertEquals(new UserName("Alice", "Smith"), sr.userProfile.name);
    alice.userProfile = us.getUserProfile(new Empty());

    // Alice logs in with a second client (same name and phone)
    TestUser aliceSecondClientProfile = tests.signUp(alice.newClient());
    assertEquals(alice.getUserProfile(), aliceSecondClientProfile.getUserProfile());
  }

  private void testNeedsReview() throws IOException {
    // TODO: Use a location outside the geofence.

    //TestUser needsReview = new TestUser("Needs", "Review");
    //needsReview.zip = null; // Not whitelisted
    //tests.setCurrentUser(needsReview);
    //us.requestVerification(new RequestVerificationRequest(needsReview.phone));
    //assertEquals(Authorization.NextStep.SIGN_UP, us.verify(new VerifyRequest(null,
    //    VerificationRequest.TEST_CODE)).authorization.nextStep);
    //needsReview.userProfile = us.getUserProfile(new Empty());
    //assertEquals(Authorization.NextStep.BLOCK,
    //    us.completeSignup(new Empty()).authorization.nextStep);
    //assertEquals(UserState.REVIEWING, User.get(needsReview.getUserId()).state);
  }

  private void testCopyProfilePhoto() throws IOException {
    TestUser janete = new TestUser("Janete", "Perez");
    tests.setCurrentUser(janete);
    FacebookUserData fb = tests.newFacebookDataFor(janete);
    fb.picture = new FacebookUserData.Picture();
    fb.picture.data = new FacebookUserData.Picture.data();
    fb.picture.data.url = User.MISSING_PHOTO_URL;
    us.requestVerification(new RequestVerificationRequest(janete.phone));
    assertEquals(Authorization.NextStep.SIGN_UP,
        us.verify(new VerifyRequest(null, VerificationRequest.TEST_CODE)).authorization.nextStep);
    ofy().save().entity(fb).now();
    AuthorizationResponse response = tests.userService.linkFacebook(
        new LinkFacebookRequest("not used", janete.facebookId));
    assertTrue(response.authorization.nextStep == Authorization.NextStep.SIGN_UP);
    UserProfile userProfile = us.getUserProfile(new Empty());
    assertNotNull(userProfile.photo);
    if (!Environment.isDevelopment()) assertTrue(userProfile.photo.startsWith("https://lh3.googleusercontent.com/"));
  }

  private void testCascadingDelete() throws IOException {
    if (Environment.isProduction()) return;
    TestUser user = new TestUser("Cascading", "Delete");
    tests.signUp(user);
    Users.cascadingDelete(Key.create(User.class, user.getUserId()));
  }

  private void testPhoneThenFacebook() throws IOException {
    TestUser user = new TestUser("Facebook", "User");
    tests.setCurrentUser(user);

    // Link phone.
    String phone = "1" + randomNumber(10);
    us.requestVerification(new RequestVerificationRequest(phone));
    AuthorizationResponse response = us.verify(
        new VerifyRequest(null, "111111"));
    assertEquals(Authorization.NextStep.SIGN_UP, response.authorization.nextStep);
    us.putUserProfile(new UserProfileRequest(new UserName(user.firstName, user.lastName),
        null, null, Collections.emptyList(), user.zip, null));
    response = us.completeSignup(Protos.EMPTY);
    assertEquals(Authorization.NextStep.PROCEED, response.authorization.nextStep);

    // Now link Facebook.
    FacebookUserData fb = tests.newFacebookDataFor(user);
    ofy().save().entity(fb).now();
    response = us.linkFacebook(new LinkFacebookRequest("not used", user.facebookId));
    assertEquals(Authorization.NextStep.PROCEED, response.authorization.nextStep);

    // Make sure phone number is still linked.
    UserProfile profile = us.getUserProfile(Protos.EMPTY);
    assertEquals(Gender.WOMAN, profile.gender);
    assertTrue(profile.phoneVerified);
  }

  private static class FakeUser extends User {
    @Override public BlockedUsers getBlockedUsers() {
      return new BlockedUsers();
    }
  }
}
