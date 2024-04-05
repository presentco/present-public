package present.acceptance;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.io.Resources;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import okio.ByteString;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.live.client.LiveClient;
import present.proto.ActivityService;
import present.proto.AuthorizationResponse;
import present.proto.CommentResponse;
import present.proto.ContentReferenceRequest;
import present.proto.ContentResponse;
import present.proto.ContentService;
import present.proto.ContentType;
import present.proto.ContentUploadRequest;
import present.proto.Coordinates;
import present.proto.Empty;
import present.proto.GroupResponse;
import present.proto.GroupService;
import present.proto.LinkFacebookRequest;
import present.proto.MessagingService;
import present.proto.NearbyGroupsRequest;
import present.proto.NearbyGroupsResponse;
import present.proto.PingRequest;
import present.proto.PingResponse;
import present.proto.PingService;
import present.proto.Platform;
import present.proto.PutDeviceTokenRequest;
import present.proto.RequestHeader;
import present.proto.RequestVerificationRequest;
import present.proto.UrlResolverService;
import present.proto.UserName;
import present.proto.UserProfile;
import present.proto.UserProfileRequest;
import present.proto.UserService;
import present.proto.VerifyRequest;
import present.server.Internal;
import present.server.Protos;
import present.server.RequestHeaders;
import present.server.Uuids;
import present.server.model.group.Group;
import present.server.model.user.VerificationRequest;
import present.server.notification.TestNotification;
import present.server.facebook.FacebookUserData;
import present.server.model.PresentEntities;
import present.server.model.activity.GroupReferral;
import present.server.model.console.whitelist.Whitelist;
import present.server.model.user.Client;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.wire.rpc.core.RpcProtocol;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static present.server.ClientUtil.*;

public abstract class AcceptanceTest {

  private static final Logger logger = LoggerFactory.getLogger(AcceptanceTest.class);

  final GroupService groupService;
  final UserService userService;
  final PingService pingService;
  final ContentService contentService;
  final ActivityService activityService;
  final UrlResolverService urlResolverService;

  final static TestUser UNREGISTERED_USER = new TestUser();
  private TestUser currentUser = UNREGISTERED_USER;
  final Coordinates location = present.server.model.util.Coordinates.SAN_FRANCISCO.toProto();
  private HeaderGenerator headerGenerator = new DefaultHeaderGenerator(
      UNREGISTERED_USER.clientUuid, Platform.TEST, location);

  final String apiUrl;

  public AcceptanceTest(String apiUrl) {
    this.apiUrl = apiUrl;
    this.groupService = rpcClient(apiUrl, GroupService.class, protocol(), headerGenerator);
    this.userService = rpcClient(apiUrl, UserService.class, protocol(), headerGenerator);
    this.pingService = rpcClient(apiUrl, PingService.class, protocol(), headerGenerator);
    this.contentService = rpcClient(apiUrl, ContentService.class, protocol(), headerGenerator);
    this.activityService = rpcClient(apiUrl, ActivityService.class, protocol(), headerGenerator);
    this.urlResolverService = rpcClient(apiUrl, UrlResolverService.class, protocol(), headerGenerator);
    installRemoteApi(apiUrl);
  }

  @Test public void testPing() throws IOException {
    PingResponse ping = pingService.ping(new PingRequest.Builder().value(3).build());
    assertEquals(3, ping.value.intValue());
  }

  @Test public void testSignup() throws IOException {
    new SignupTest(this).testSignup();
  }

  @Test public void testGroupUsage() throws IOException, InterruptedException {
    new GroupTest(this).testGroups();
  }

  @Test public void testBlockingUsers() throws IOException, InterruptedException {
    new BlockUsersTest(this).testBlockingUsers();
  }

  @Test public void testBlockingLiveServer() throws IOException, InterruptedException {
    new BlockUsersTest(this).testBlockingLiveServer();
  }

  @Test public void testGroupReferral() throws IOException {
    new GroupReferralTest(this).testGroupReferral();
  }

  @Test public void testUnread() throws IOException {
    new UnreadTest(this).run();
  }

  @Ignore @Test public void testAutoJoin() throws IOException, InterruptedException {
    new AutoJoinGroupTest(this).testAutoJoin();
  }

  @Test public void getCities() throws IOException {
    assertEquals(10, groupService.getCities(Protos.EMPTY).cities.size());
  }


  @AfterClass public static void cleanup() {
    // TODO: Remove test groups here
  }

  /** Set the active use for API requests */
  public void setCurrentUser( TestUser user ) {
    currentUser = user;
    headerGenerator.setClientUuid(user.clientUuid);
  }

  public TestUser getCurrentUser() {
    return currentUser;
  }

  public RequestHeader newHeader() {
    return headerGenerator.newHeader();
  }

  RpcProtocol protocol() {
    return RpcProtocol.PROTO;
  }

  public FacebookUserData newFacebookDataFor(TestUser user) {
    FacebookUserData fb = new FacebookUserData();
    fb.id = user.facebookId;
    fb.first_name = user.firstName;
    fb.last_name = user.lastName;
    fb.gender = "female";
    fb.friends = new FacebookUserData.Friends();
    fb.friends.summary = new FacebookUserData.Friends.Summary();
    fb.friends.summary.total_count = "25";
    return fb;
  }

  /**
   * Perform the signup login process for the user and set the user as the current user for purposes
   * of subsequent service calls.
   * @return the logged in user which now contains a valid user profile.
   */
  public TestUser signUp(TestUser user) throws IOException {
    setCurrentUser(user);
    userService.requestVerification(new RequestVerificationRequest(user.phone));
    userService.verify(new VerifyRequest(null, VerificationRequest.TEST_CODE));

    // Use a zip code to whitelist the test user.
    userService.putUserProfile(new UserProfileRequest(new UserName(user.firstName, user.lastName),
        null, null, Collections.emptyList(), Whitelist.TEST_ZIP, null));

    // Simulate an iOS client with the user's test device token.
    userService.putDeviceToken(new PutDeviceTokenRequest(user.deviceToken, null));

    // This makes the user a member.
    userService.completeSignup(new Empty());

    user.userProfile = userService.getUserProfile(new Empty());
    assertEquals(user.firstName, user.userProfile.name.first);
    assertEquals(user.lastName, user.userProfile.name.last);

    Users.setCurrent(User.get(user.getUserId()));

    return user;
  }

  GroupResponse findGroupByTitleExpected(final String title) {
    return retry(() -> {
      GroupResponse group = findGroupByTitle(title);
      if ( group == null ) {
        throw new AssertionError();
      }
      return group;
    });
  }

  void findGroupByTitleNotExpected(final String title) {
    retry(() -> {
      GroupResponse group = findGroupByTitle(title);
      if (group != null) {
        throw new AssertionError();
      }
    });
  }

  /**
   * Find the group by title.
   * The title is expected to be unique: If not an assertion is failed and null is returned.
   */
  private GroupResponse findGroupByTitle(final String title) {
    GroupResponse found = null;
    try {
      NearbyGroupsResponse nb = groupService.getNearbyGroups(new NearbyGroupsRequest(location, null));
      for (GroupResponse b : nb.nearbyGroups) {
        if (b.title.equals(title)) {
          if (found != null) {
            fail("Duplicate groups found with the same title: "+b.title);
            return null;
          }
          found = b;
        }
      }
      return found;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static String newUuid() {
    return UUID.randomUUID().toString();
  }

  /**
   * Retry until the contained assertions pass. Necessary for eventually consistent data.
   */
  void retry(final Runnable r) {
    retry((Supplier<Void>) () -> {
      r.run();
      return null;
    });
  }

  /**
   * Retry until the contained assertions pass. Necessary for eventually consistent data.
   */
  <T> T retry(Supplier<T> s) {
    int maxTries = 100;
    int tries = 0;
    while (true) {
      try {
        return s.get();
      } catch (AssertionError e) {
        if (++tries == maxTries) {
          throw e;
        }
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) { /* ignored */ }
    }
  }

  private static SecureRandom random;

  public static String randomNumber(int length) {
    if (random == null) { random = new SecureRandom(); }

    int value = random.nextInt(Integer.MAX_VALUE);
    String code = Integer.toString(value);
    if (code.length() > length) code = code.substring(0, length);
    return Strings.padStart(code, length, '0');
  }

  static class CommentListener implements LiveClient.Listener {

    final BlockingDeque<CommentResponse> comments = new LinkedBlockingDeque<>();
    final BlockingDeque<CommentResponse> deleted = new LinkedBlockingDeque<>();
    final CountDownLatch readyLatch = new CountDownLatch(1);

    @Override public void comment(CommentResponse comment) { comments.add(comment); }
    @Override public void deleted(CommentResponse comment) { deleted.add(comment); }
    @Override public void ready() { readyLatch.countDown(); }
    @Override public void closed() {}
    @Override public void networkError(Throwable t) {
      throw new RuntimeException(t);
    }
  }

  void waitFor(CountDownLatch latch) throws InterruptedException {
    latch.await(5, TimeUnit.SECONDS);
    assertEquals(0, latch.getCount());
  }

  /**
   * A user with a randomized client id and phone.
   */
  public static class TestUser {
    String firstName;
    String lastName;
    String facebookId = randomNumber(10);
    UserProfile userProfile;

    // Client device info
    String clientUuid = UUID.randomUUID().toString();
    String deviceToken = UUID.randomUUID().toString();
    String zip = Whitelist.TEST_ZIP;
    String phone = newPhone();

    public TestUser() { }

    public TestUser(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    public Client client() {
      return Client.get(clientUuid);
    }

    /**
     * Return a new TestUser with the same name and phone but a new, random client uuid and device token.
     */
    public TestUser newClient() {
      TestUser user = new TestUser(firstName, lastName);
      user.facebookId = facebookId;
      user.phone = phone;
      return user;
    }

    public FacebookUserData.Friends.Friend toFacebookFriend() {
      FacebookUserData.Friends.Friend friend = new FacebookUserData.Friends.Friend();
      friend.first_name = firstName;
      friend.last_name = lastName;
      friend.name = firstName + " " + lastName;
      friend.id = facebookId;
      return friend;
    }

    /**
     * If the user has been logged in return the user profile.
     */
    public UserProfile getUserProfile() {
      if (userProfile == null) {
        throw new RuntimeException("User not logged in.");
      }
      return userProfile;
    }

    public String getUserId() {
      return getUserProfile().id;
    }

    public Key<User> key() {
      return Key.create(User.class, getUserId());
    }

    public User user() {
      return User.get(getUserId());
    }
  }

  private static String newPhone() {
    return "1" + AcceptanceTest.randomNumber(3) + "555" + AcceptanceTest.randomNumber(4);
  }

  public static ByteString urlBytes(String url) {
    try {
      return ByteString.of(Resources.toByteArray(new URL(url)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // A fixed uuid used for the JPG1x1Bytess test photo upload so that test photos do not accumulate.
  public static final String JPG1x1Bytes_UUID = "2D68FE53-C7CE-4E6D-8C5C-93C3C60A1966";

  // A 1x1 JPEG.
  public static final ByteString JPG1x1Bytes = ByteString.of(
      (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, (byte) 0x00, (byte) 0x10,
      (byte) 0x4a, (byte) 0x46, (byte) 0x49, (byte) 0x46, (byte) 0x00, (byte) 0x01, (byte) 0x01,
      (byte) 0x01, (byte) 0x00, (byte) 0x60, (byte) 0x00, (byte) 0x60, (byte) 0x00, (byte) 0x00,
      (byte) 0xff, (byte) 0xdb, (byte) 0x00, (byte) 0x43, (byte) 0x00, (byte) 0x08, (byte) 0x06,
      (byte) 0x06, (byte) 0x07, (byte) 0x06, (byte) 0x05, (byte) 0x08, (byte) 0x07, (byte) 0x07,
      (byte) 0x07, (byte) 0x09, (byte) 0x09, (byte) 0x08, (byte) 0x0a, (byte) 0x0c, (byte) 0x14,
      (byte) 0x0d, (byte) 0x0c, (byte) 0x0b, (byte) 0x0b, (byte) 0x0c, (byte) 0x19, (byte) 0x12,
      (byte) 0x13, (byte) 0x0f, (byte) 0x14, (byte) 0x1d, (byte) 0x1a, (byte) 0x1f, (byte) 0x1e,
      (byte) 0x1d, (byte) 0x1a, (byte) 0x1c, (byte) 0x1c, (byte) 0x20, (byte) 0x24, (byte) 0x2e,
      (byte) 0x27, (byte) 0x20, (byte) 0x22, (byte) 0x2c, (byte) 0x23, (byte) 0x1c, (byte) 0x1c,
      (byte) 0x28, (byte) 0x37, (byte) 0x29, (byte) 0x2c, (byte) 0x30, (byte) 0x31, (byte) 0x34,
      (byte) 0x34, (byte) 0x34, (byte) 0x1f, (byte) 0x27, (byte) 0x39, (byte) 0x3d, (byte) 0x38,
      (byte) 0x32, (byte) 0x3c, (byte) 0x2e, (byte) 0x33, (byte) 0x34, (byte) 0x32, (byte) 0xff,
      (byte) 0xdb, (byte) 0x00, (byte) 0x43, (byte) 0x01, (byte) 0x09, (byte) 0x09, (byte) 0x09,
      (byte) 0x0c, (byte) 0x0b, (byte) 0x0c, (byte) 0x18, (byte) 0x0d, (byte) 0x0d, (byte) 0x18,
      (byte) 0x32, (byte) 0x21, (byte) 0x1c, (byte) 0x21, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32,
      (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0x32, (byte) 0xff, (byte) 0xc0,
      (byte) 0x00, (byte) 0x11, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01,
      (byte) 0x03, (byte) 0x01, (byte) 0x22, (byte) 0x00, (byte) 0x02, (byte) 0x11, (byte) 0x01,
      (byte) 0x03, (byte) 0x11, (byte) 0x01, (byte) 0xff, (byte) 0xc4, (byte) 0x00, (byte) 0x15,
      (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0xff, (byte) 0xc4,
      (byte) 0x00, (byte) 0x14, (byte) 0x10, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff,
      (byte) 0xc4, (byte) 0x00, (byte) 0x14, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0xff, (byte) 0xc4, (byte) 0x00, (byte) 0x14, (byte) 0x11, (byte) 0x01, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0xff, (byte) 0xda, (byte) 0x00, (byte) 0x0c, (byte) 0x03, (byte) 0x01,
      (byte) 0x00, (byte) 0x02, (byte) 0x11, (byte) 0x03, (byte) 0x11, (byte) 0x00, (byte) 0x3f,
      (byte) 0x00, (byte) 0xbf, (byte) 0x80, (byte) 0x0f, (byte) 0xff, (byte) 0xd9
  );

  private static ContentReferenceRequest _JPG1x1ReferenceRequest;

  // Upload the JPG1x1Bytes if it has not already been uploaded and return the content reference for it.
  public ContentReferenceRequest getJPG1x1ReferenceRequest() {
    if (_JPG1x1ReferenceRequest != null) {
      return _JPG1x1ReferenceRequest;
    }
    try {
      ContentResponse contentResponse = contentService.putContent(
          new ContentUploadRequest(JPG1x1Bytes_UUID, ContentType.JPEG, JPG1x1Bytes, NO_THUMBNAIL));
      _JPG1x1ReferenceRequest = new ContentReferenceRequest(contentResponse.uuid, contentResponse.contentType);
      return _JPG1x1ReferenceRequest;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ByteString NO_THUMBNAIL = null;
  public static ContentReferenceRequest NO_CONTENT_REF = null;

  /* Installing the remote API takes a few seconds, so we do it just once. */

  private static String remoteApiUrl;
  private static void installRemoteApi(String apiUrl) {
    if (remoteApiUrl != null) {
      Preconditions.checkState(apiUrl.equals(remoteApiUrl));
      return;
    }
    remoteApiUrl = apiUrl; // Skip this method next time.
    Stopwatch sw = Stopwatch.createStarted();
    URI parsedUri = URI.create(apiUrl);
    String host = parsedUri.getHost();
    int port = parsedUri.getPort();
    if (port == -1) port = 443; // default
    RemoteApiOptions options = new RemoteApiOptions();
    // e.g. local.present.co
    if (host.startsWith("local")) {
      // Local dev server must be "localhost"
      options.useDevelopmentServerCredential().server("localhost", port);
    } else {
      options.useApplicationDefaultCredential().server(host, port);
    }
    RemoteApiInstaller installer = new RemoteApiInstaller();
    try {
      installer.install(options);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    NamespaceManager.set("test"); // Test namespace!
    PresentEntities.registerAll();
    try (Closeable closeable = ObjectifyService.begin()) {
      initializeDatastore();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    logger.info("Installed remote API in {}.", sw);
  }

  /** One-time datastore setup. */
  private static void initializeDatastore() {}

  /* Set up and tear down Objectify. */

  private Closeable objectify;

  @Before public void defaultHeaders() {
    // Prevents emails from being sent from tests that invoke EmailService via RpcQueue.
    RequestHeaders.setCurrent(new RequestHeader.Builder()
        .clientUuid(Uuids.NULL)
        .requestUuid(Uuids.NULL)
        .platform(Platform.TEST)
        .authorizationKey("ignored")
        .apiVersion(1)
        .build());
  }

  @Before public void setUpDatastore() throws IOException {
    objectify = ObjectifyService.begin();
    deleteAll(User.class);
    deleteAll(Group.class);
    deleteAll(TestNotification.class);
    deleteAll(GroupReferral.class);
  }

  private static <T> void deleteAll(Class<T> entityType) {
    List<Key<T>> keys = ofy().load().type(entityType).keys().list();
    ofy().delete().keys(keys).now();
  }

  @After public void tearDownDatastore() throws IOException {
    // Tear down Objectify after each test so remaining writes commit.
    objectify.close();
  }

  @Before public void checkNamespace() {
    assertEquals("test", NamespaceManager.get());
  }
}
