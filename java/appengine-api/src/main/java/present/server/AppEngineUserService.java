package present.server;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Query;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.phone.PhoneNumbers;
import present.phone.TwilioGateway;
import present.proto.AddContactsRequest;
import present.proto.AddContactsResponse;
import present.proto.AddFriendResponse;
import present.proto.AdminSearchRequest;
import present.proto.AdminSearchResponse;
import present.proto.AdminUserResponse;
import present.proto.Authorization;
import present.proto.AuthorizationResponse;
import present.proto.BlockScreen;
import present.proto.ComputeUnreadCountsRequest;
import present.proto.ContentResponse;
import present.proto.Empty;
import present.proto.Feature;
import present.proto.Gender;
import present.proto.LinkFacebookRequest;
import present.proto.NotificationReceivedRequest;
import present.proto.PhoneUserResponse;
import present.proto.Platform;
import present.proto.PutDeviceTokenRequest;
import present.proto.PutUserPhotoRequest;
import present.proto.RequestHeader;
import present.proto.RequestVerificationRequest;
import present.proto.RequestVerificationResponse;
import present.proto.ResolveUrlResponse;
import present.proto.SpacesResponse;
import present.proto.SummaryRequest;
import present.proto.SynchronizeRequest;
import present.proto.SynchronizeResponse;
import present.proto.TransitionStateRequest;
import present.proto.UserName;
import present.proto.UserNotificationSettings;
import present.proto.UserProfile;
import present.proto.UserProfileRequest;
import present.proto.UserRequest;
import present.proto.UserResponse;
import present.proto.UserSearchRequest;
import present.proto.UserService;
import present.proto.UsersResponse;
import present.proto.ValidStateTransitionResponse;
import present.proto.ValidStateTransitionsResponse;
import present.proto.VerifyRequest;
import present.server.email.SummaryEmail;
import present.server.environment.Environment;
import present.server.facebook.Facebook;
import present.server.facebook.FacebookException;
import present.server.facebook.FacebookFriendship;
import present.server.facebook.FacebookUserData;
import present.server.model.Space;
import present.server.model.comment.GroupView;
import present.server.model.console.whitelist.Whitelist;
import present.server.model.content.Content;
import present.server.model.geocoding.Geocoding;
import present.server.model.group.Group;
import present.server.model.group.WelcomeGroup;
import present.server.model.log.DatastoreOperation;
import present.server.model.user.BlockedUsers;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.Contact;
import present.server.model.user.Friendship;
import present.server.model.user.IosVersion;
import present.server.model.user.PhoneToUser;
import present.server.model.user.PresentAdmins;
import present.server.model.user.User;
import present.server.model.user.UserSearch;
import present.server.model.user.UserState;
import present.server.model.user.Users;
import present.server.model.user.VerificationRequest;
import present.server.model.util.Coordinates;
import present.server.notification.Notifications;
import present.server.slack.AnnounceToSlack;
import present.server.slack.SlackClient;
import present.wire.rpc.core.ClientException;
import present.wire.rpc.core.ServerException;

import static com.google.appengine.api.datastore.Entity.KEY_RESERVED_PROPERTY;
import static com.google.appengine.api.datastore.Query.CompositeFilterOperator.and;
import static com.google.appengine.api.datastore.Query.CompositeFilterOperator.or;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.proto.Authorization.NextStep.AUTHENTICATE;
import static present.proto.Authorization.NextStep.BLOCK;
import static present.proto.Authorization.NextStep.PROCEED;
import static present.proto.Authorization.NextStep.SIGN_UP;
import static present.server.email.Emails.send;
import static present.server.email.Emails.to;
import static present.server.email.Emails.waitlistEmail;
import static present.server.model.content.Content.capturePhotoToContent;
import static present.server.model.user.Feature.PHONE_SIGNUP;
import static present.server.model.user.User.Fields.email;
import static present.server.model.user.User.Fields.firstName;
import static present.server.model.user.User.Fields.lastName;
import static present.server.model.user.User.Fields.phoneNumber;
import static present.server.model.user.User.Review.EMAIL_WHITELISTED;
import static present.server.model.user.User.Review.INSIDE_GEOFENCE;
import static present.server.model.user.User.Review.OUTSIDE_GEOFENCE;
import static present.server.model.user.User.Review.PRE_APPROVED;
import static present.server.model.user.User.Review.ZIP_WHITELISTED;
import static present.server.model.user.Users.findByFacebookId;
import static present.server.model.util.Coordinates.fromProto;

/**
 * App Engine implementation of UserService.
 *
 * @author Bob Lee (bob@present.co)
 */
public class AppEngineUserService implements UserService {

  private static final Logger logger = LoggerFactory.getLogger(AppEngineUserService.class);

  @Override public AuthorizationResponse linkFacebook(LinkFacebookRequest request)
      throws IOException {
    RequestHeader header = RequestHeaders.current();

    // Don't accidentally authenticate the null client.
    if (header.clientUuid.equals(Uuids.NULL)) throw new ClientException("Nope.");

    Client client = Clients.getOrCreate(header.clientUuid);

    // Look up Facebook data.
    FacebookUserData facebookData;
    if (header.platform == Platform.TEST) {
      facebookData = FacebookUserData.get(request.facebookId);
    } else {
      try {
        facebookData = Facebook.getUserData(request.accessToken);
      } catch (FacebookException e) {
        logger.warn("Error getting Facebook data.", e);
        if (e.requestLogin()) {
          // Try linking Facebook again.
          return authorizationResponse(AUTHENTICATE);
        } else {
          throw new ServerException(e);
        }
      }
    }

    // Is the client already logged in?
    User user = client.user();
    if (user != null) {
      logger.info("Client is already logged in.");

      if (user.facebookId != null && !user.facebookId.equals(facebookData.id)) {
        // This shouldn't happen, but go ahead and link to the new account.
        logger.error("User was already linked to Facebook ID #" + user.facebookId + ".");
      }

      User found = findByFacebookId(facebookData.id);
      if (found != null && !found.equals(user)) {
        // Go ahead and link the Facebook account to two users. We'll need to manually clean it up.
        logger.error("Linking Facebook ID #" + facebookData.id + " to more than one account!");
      }
    } else {
      logger.info("Client isn't logged in yet.");

      user = findByFacebookId(facebookData.id);
      if (user == null) {
        logger.info("New user!");
        user = Users.create();
      }

      client.user = user.getRef();
      Users.setCurrent(user);
    }

    // Update User with Facebook data.
    user.facebookAccessToken = request.accessToken;
    user.facebookId = facebookData.id;
    if (user.firstName == null) user.firstName = facebookData.first_name;
    if (user.lastName == null) user.lastName = facebookData.last_name;
    if (user.email == null) {
      user.email = facebookData.email == null ? null : facebookData.email.toLowerCase();
    }
    if (user.gender == null || user.gender() == Gender.UNKNOWN) {
      user.gender = facebookData.gender();
    }
    if (user.photo == null) {
      Content photo = facebookPhotoToContent(facebookData, RequestHeaders.isTest());
      if (photo != null) user.photo = Ref.create(photo);
    }

    if (user.state == UserState.INVITED) user.state = UserState.SIGNING_UP;

    // Save the data.
    User finalUser = user;
    ofy().transact(() -> {
      ofy().save().entities(client, finalUser, facebookData);
    });

    if (user.nascent && user.state == UserState.SIGNING_UP) {
      AnnounceToSlack.userTransition(user, "started signing up");
    }

    DatastoreOperation.log(user);

    FacebookFriendship.saveFriends(user, facebookData);
    return nextStepFor(user);
  }

  @Override public RequestVerificationResponse requestVerification(RequestVerificationRequest request)
      throws IOException {
    String phone = request.phoneNumber;
    PhoneNumbers.validateUsPhone(phone);
    if (phone.startsWith("+")) phone = phone.substring(1);
    // Example: 14155551212
    boolean test = RequestHeaders.isTest()
        || (!Environment.isProduction() && PhoneNumbers.isTestNumber(phone));
    if (test) logger.info("Test number. Suppressing SMS.");
    VerificationRequest verificationRequest = VerificationRequest.clientDependent(test);
    User user = Users.findByPhone(phone);
    if (user != null) verificationRequest.userId = user.uuid;
    verificationRequest.phoneNumber = phone;
    verificationRequest.save();
    String code = verificationRequest.code();
    String formattedCode;
    if (code.length() >= 6) {
      int half = code.length() / 2;
      formattedCode = code.substring(0, half) + "-" + code.substring(half);
    } else {
      formattedCode = code;
    }
    if (!test) {
      new TwilioGateway().sms(phone, "Present: " + formattedCode
          + " is your verification code."
          + "\n\nEnter it, or tap to continue: " + verificationRequest.url());
    }
    return new RequestVerificationResponse(code.length());
  }

  @Override public AuthorizationResponse verify(VerifyRequest request) throws IOException {
    RequestHeader header = RequestHeaders.current();

    // Don't accidentally authenticate the null client.
    if (header.clientUuid.equals(Uuids.NULL)) throw new ClientException("Nope.");

    // Determine the verification code.
    String code;
    if (request.url != null) {
      URI url = URI.create(request.url);
      String path = url.getRawPath();
      if (!path.startsWith("/v/")) throw new ClientException("Invalid URL: " + request.url);
      code = path.substring(3);
    } else {
      code = request.code;
      if (!code.chars().allMatch(Character::isDigit)) {
        throw new ClientException("Code contains invalid chars: " + code);
      }
    }

    // Look up the verification request.
    VerificationRequest verification = VerificationRequest.forCode(code);
    if (verification == null) throw new ClientException("Invalid verification code.");
    if (System.currentTimeMillis() - verification.createdTime > Time.WEEK_IN_MILLIS) {
      throw new ClientException("Verification code expired.");
    }

    User user = Users.current(false);
    if (user == null) {
      // The client doesn't have a user associated yet.
      user = verification.findUser();
      if (user == null) {
        user = Users.create();
        user.firstName = verification.firstName;
        user.lastName = verification.lastName;
        user.email = verification.email;
        user.phoneNumber = verification.phoneNumber;
        user.state = UserState.SIGNING_UP;
        user.gender = verification.gender;
        user.spaceIds = verification.spaceIds;
        user.save();
      } else {
        if (user.state == UserState.INVITED) {
          // We're taking over a placeholder user.
          user.transitionTo(UserState.SIGNING_UP);
        }
      }

      if (user.nascent) {
        AnnounceToSlack.userTransition(user, "started signing up");
      }

      Users.setCurrent(user);
      if (user.nascent) DatastoreOperation.log(user);

      Client client = Clients.current();
      if (client.user == null || !client.user.equals(user.getRef())) {
        client.user = user.getRef();
        client.save();
      }
    } else {
      // TODO: Support linking email here.

      if (user.phoneNumber == null) {
        // Most likely this is a legacy user who already linked their Facebook.

        // Look for an existing user with this phone number.
        User conflicted = Users.findByPhone(verification.phoneNumber);

        // Link phone number to this user.
        user.inTransaction(u -> {
          u.phoneNumber = verification.phoneNumber;
        });

        // Merge placeholder user if we found one.
        if (conflicted != null) {
          if (conflicted.state == UserState.INVITED) {
            try {
              Users.merge(conflicted, user);
            } catch (Exception e) {
              logger.error("Error merging placeholder user.", e);
            }
          } else {
            logger.error("Phone already linked to " + conflicted + ".",
                new Exception("Phone already linked."));
          }
        }
      } else {
        if (user.phoneNumber.equals(verification.phoneNumber)) {
          logger.info("Duplicate request");
        } else {
          logger.error("{} already has a phone number linked. Linking a new number.", user);
        }
      }
    }

    if (verification.userId == null) {
      // Ensure that the verification URL can't be reused by another user.
      verification.userId = user.uuid;
      verification.save();
    }

    return nextStepFor(user);
  }

  /**
   * @param test Each call with test true uses the same test UUID.
   */
  public static Content facebookPhotoToContent(FacebookUserData facebookData, boolean test) {
    try {
      String facebookPhoto = facebookData.profilePhotoUrl();
      return capturePhotoToContent(facebookPhoto, test);
    } catch (IOException e) {
      logger.warn("Error copying Facebook profile photo.", e);
    }
    return null;
  }

  @Override
  public AuthorizationResponse completeSignup(Empty empty) throws IOException {
    User user = Users.current(false);
    if (user == null) return authorizationResponse(AUTHENTICATE);
    if (user.state == UserState.PRE_APPROVED || user.state == UserState.SIGNING_UP) {
      Coordinates location = fromProto(RequestHeaders.current().location);
      if (user.transitionTo(stateAfterSignup(user, location))) {
        switch (user.state) {
          case MEMBER:
            logger.info("Making {} a member!", user);
            welcomeNewUser(user);
            break;
          case REVIEWING:
            AnnounceToSlack.userTransition(user, "needs review");
            if (user.email() != null) send(waitlistEmail(to(user, "WAITLIST")).build());
            break;
          case REJECTED:
            // TODO: Announce this?
        }
      }
    }
    return nextStepFor(user);
  }

  @Override public SynchronizeResponse synchronize(SynchronizeRequest request) throws IOException {
    // Track whether or not notifications are enabled on this client.
    Client client = Clients.current();
    if (request.notificationsEnabled != null) {
      if (!Objects.equals(client.notificationsEnabled, request.notificationsEnabled)) {
        client.notificationsEnabled = request.notificationsEnabled;
        client.save();
      }
    }

    User user = Users.current(false);
    if (user == null) {
      return synchronizeResponse(authorizationResponse(AUTHENTICATE));
    }

    if (user.state == UserState.REVIEWING) {
      // Check if the user has been added to the whitelist since completing signup.
      Coordinates location = fromProto(RequestHeaders.current().location);
      UserState next = stateAfterSignup(user, location);
      if (next == UserState.MEMBER && user.transitionTo(UserState.MEMBER)) {
        logger.info("Making {} a member!", user);
        welcomeNewUser(user);
      }
    }

    if (user.hasAccess()) {
      user.updateLastActiveTime();

      try {
        if (!RequestHeaders.isTest()) {
          if (user.facebookId != null) {
            FacebookUserData facebookData = user.facebookData();
            if (facebookData == null || facebookData.needsUpdate()) {
              user.updateFacebook();
            }
          }
        } else {
          logger.info("Skipping Facebook sync in test.");
        }
      } catch (FacebookException e) {
        logger.warn("Error getting Facebook data.", e);
        //if (e.requestLogin()) return synchronizeResponse(authorizationResponse(AUTHENTICATE));
      }
    }

    return synchronizeResponse(nextStepFor(user));
  }

  private static SynchronizeResponse synchronizeResponse(AuthorizationResponse response) {
    SynchronizeResponse.Builder builder = new SynchronizeResponse.Builder()
        .authorization(response.authorization)
        .userProfile(response.userProfile);
    // Remove this for men if we see abuse.
    builder.features(ImmutableList.of(Feature.CIRCLE_CREATION));
    return builder.build();
  }

  private static AuthorizationResponse authorizationResponse(Authorization.NextStep nextStep) {
    return authorizationResponse(nextStep, null);
  }

  private static AuthorizationResponse authorizationResponse(Authorization.NextStep nextStep,
      String blockMessage) {
    logger.info("Next step: {}", nextStep);
    if (nextStep == BLOCK && blockMessage == null) {
      throw new IllegalArgumentException("Missing blockMessage.");
    }
    User user = Users.current(false);
    UserProfile profile = user == null ? null : user.toUserProfile();
    BlockScreen blockScreen = blockMessage == null ? null : new BlockScreen(blockMessage);
    return new AuthorizationResponse(new Authorization(nextStep, blockScreen), profile);
  }

  private AuthorizationResponse nextStepFor(User user) {
    Client client = Clients.current();
    if (user.phoneNumber == null && client.supports(PHONE_SIGNUP)) {
      return authorizationResponse(AUTHENTICATE);
    }

    switch (user.state) {
      case GHOSTED:
      case MEMBER:
        return authorizationResponse(PROCEED);
      case INVITED:
      case PRE_APPROVED:
      case SIGNING_UP: return authorizationResponse(SIGN_UP);
      default: return authorizationResponse(BLOCK, BlockMessages.forUser(user));
    }
  }

  public static void addNewUserToHomeCircle(User newUser) {
    Key<Group> key = WelcomeGroup.nearestTo(newUser.signupLocation);
    Group group = ofy().load().key(key).now();
    GroupView view = group.getOrCreateView(Users.current());
    view.mute().save();
    newUser.join(Group.get(key));
  }

  // A new user has passed the whitelist for the first time.
  public static void welcomeNewUser(User newUser) {
    // Notify Present Company on slack
    AnnounceToSlack.userTransition(newUser, "became a member");

    // Initiate a welcome chat
    if (!RequestHeaders.isTest()) {
      // Auto-join new user to closest city's "Welcome to Present" circle
      addNewUserToHomeCircle(newUser);
    }
  }

  static class SendWelcomeMessage implements DeferredTask {

    private static final long serialVersionUID = 0;

    private final String userId;

    public SendWelcomeMessage(String userId) {
      this.userId = userId;
    }

    @Override public void run() {
      User user = User.get(userId);
      if (user == null) {
        logger.warn("User #{} was deleted.", userId);
        return;
      }
      PresentAdmins.newUser(user);
    }
  }

  @Override public Empty putUserName(UserName userName) throws IOException {
    User user = Users.expectedCurrent(false);
    user.firstName = userName.first;
    user.lastName = userName.last;
    user.save();
    return new Empty();
  }

  @Override public UserProfile getUserProfile(Empty empty) throws IOException {
    logger.info("getUserProfile");
    return Users.expectedCurrent(false).toUserProfile();
  }

  @Override public UserProfile putUserProfile(UserProfileRequest request) throws IOException {
    return ofy().transact(() -> {
      User user = Users.expectedCurrent(false).reload();
      user.updateFrom(request);
      user.save();
      DatastoreOperation.log(user);
      return user;
    }).toUserProfile();
  }

  @Override public ContentResponse putUserPhoto(PutUserPhotoRequest request)
      throws IOException {
    User user = Users.expectedCurrent(false);
    Content photoContent = user.setPhotoFromRequest(request.photoRef);
    user.save();
    return photoContent.toResponse();
  }

  @Override
  public Empty putUserNotificationSettings( UserNotificationSettings userNotificationSettings ) throws IOException {
    logger.debug("Put user notification settings: "+userNotificationSettings);
    User user = Users.expectedCurrent(false);
    user.notificationSettings.updateFrom(userNotificationSettings);
    user.save();
    return new Empty();
  }

  public static UserState stateAfterSignup(User user, Coordinates location) {
    if (user.state == UserState.PRE_APPROVED) {
      logger.info("User is in pre-approved state.");
      user.review = PRE_APPROVED;
      return UserState.MEMBER;
    }

    if (Whitelist.userIsWhitelisted(user)) {
      logger.info("User is on whitelist.");
      user.review = EMAIL_WHITELISTED;
      return UserState.MEMBER;
    }

    if (Whitelist.zipCodeIsWhitelisted(user, user.zip)) {
      logger.info("Zip code {} is whitelisted.", user.zip);
      user.review = ZIP_WHITELISTED;
      return UserState.MEMBER;
    }

    if (Whitelist.locationIsWhitelisted(user, location)) {
      logger.info("{} is inside a geofence.", user);
      user.review = INSIDE_GEOFENCE;
      return UserState.MEMBER;
    } else {
      logger.info("{} is outside geofences.", user);
      user.review = OUTSIDE_GEOFENCE;
      return UserState.REVIEWING;
    }
  }

  @Override public Empty putDeviceToken(final PutDeviceTokenRequest request) throws IOException {
    ofy().transact(() -> {
      Client client = Clients.current();

      String deviceToken = request.deviceToken;

      // TODO: Remove this once clients have been updated to pass null instead.
      if ("SWITCHED-ENDPOINTS-DO-NOT-PUSH".equals(deviceToken)) deviceToken = null;

      // Add prefix to iOS tokens targeted at the sandbox environment.
      if (deviceToken != null && client.platform() == Platform.IOS
          && request.apnsEnvironment == PutDeviceTokenRequest.ApnsEnvironment.SANDBOX) {
        deviceToken = Notifications.SANDBOX_TOKEN_PREFIX + deviceToken;
      }

      client.deviceToken = deviceToken;
      client.deviceTokenUpdateTime = System.currentTimeMillis();

      ofy().save().entity(client).now();
    });
    return new Empty();
  }

  @Override public UserResponse getUser(UserRequest request) throws IOException {
    return userFrom(request).toResponse(true);
  }

  @Override public Empty blockUser(UserRequest request) throws IOException {
    // Report the blocked user
    logger.info(String.format("BLOCKED USER REPORT: client: %s, userId: %s", Users.current().uuid, request.userId));
    User blockedUser = userFrom(request);

    String slackMessage = String.format("%s reported %s.", SlackClient.link(Users.current()),
        SlackClient.link(blockedUser));
    SlackClient.post(SlackClient.reportBuilder().text(slackMessage).build());

    Ref<User> currentUserRef = Ref.create(Users.current());
    // Create the blocked users entity if it doesn't exist yet
    BlockedUsers blockedUsers = Users.getBlockedUsers();
    if (blockedUsers == null) {
      blockedUsers = new BlockedUsers(currentUserRef);
    }
    // Add the blocked user to the current user's block list
    Ref<User> blockedUserRef = blockedUser.getRef();
    if (blockedUsers.users.add(blockedUserRef)) {
      ofy().save().entity(blockedUsers).now();
    }
    // Add the current user to the blocked user's inverse block filter
    BlockedUsers invBlockedUsers = Users.getOrCreateBlockedUsers(blockedUserRef);
    invBlockedUsers.inverseUsers.put(currentUserRef);
    ofy().save().entity(invBlockedUsers).now();
    return new Empty();
  }

  @Override public Empty unblockUser(UserRequest request) throws IOException {
    BlockedUsers blockedUsers = Users.getBlockedUsers();

    // Support testing call to reset.
    String resetFlag = "RESET";
    if (request.userId.equals(resetFlag)) {
      if(blockedUsers != null) {
        blockedUsers.deleteHard().now();
      }
      return new Empty();
    }

    if (blockedUsers == null) {
      throw new ClientException("Current user has no blocked users, cannot unblock: "+request.userId);
    }

    // Remove the blocked user
    User blockedUser = userFrom(request);
    if (blockedUsers.users.remove(blockedUser.getRef())) {
      ofy().save().entity(blockedUsers).now();
    } else {
      throw new ClientException("Request to unblock user who was not blocked: "+request.userId);
    }
    // Remove the current user from the blocked user's inverse block filter
    BlockedUsers invBlockedUsers = Users.getOrCreateBlockedUsers(blockedUser.getRef());
    invBlockedUsers.rebuildInverseUsers();
    ofy().save().entity(invBlockedUsers).now();
    return new Empty();
  }

  @Override public UsersResponse getBlockedUsers(Empty empty) throws IOException {
    return Users.allowInvisibleUsers(() -> {
      Iterable<UserResponse> userResponses = User.toResponses(Users.loadBlockedUsers());
      return new UsersResponse(Lists.newArrayList(userResponses));
    });
  }

  @Override public Empty notificationReceived(NotificationReceivedRequest notificationReceivedRequest)
      throws IOException {
    logger.info("Client received push notification: client={}, notification={}",
        Clients.current(), notificationReceivedRequest.notification);
    return new Empty();
  }

  @Override public UsersResponse search(UserSearchRequest userSearchRequest) throws IOException {
    User user = Users.current();
    // Only admins and women can search for people.
    if (!user.isAdmin() && !user.isWoman()) return new UsersResponse(Collections.emptyList());
    Set<Key<User>> resultKeys = UserSearch.search(userSearchRequest.searchText);
    List<UserResponse> userResponses = resultKeys.stream()
      .map(User::get)
      .filter(user::canSee)
      .map(u -> u.toResponse(false))
      .collect(Collectors.toList());
    return new UsersResponse(userResponses);
  }

  @Override @Internal public Empty computeUnreadCounts(ComputeUnreadCountsRequest request)
      throws IOException {
    User user = Users.tryIncrementUnreadVersion(request.userId, request.unreadVersion);
    if (user != null) user.computeUnreadState();
    return Protos.EMPTY;
  }

  @Override @Internal public Empty geocodeSignupLocation(UserRequest request) throws IOException {
    User user = userFrom(request);
    if (user != null) {
      Geocoding.geocodeSignupLocation(user);
    } else {
      logger.error("Can't find user: {}", request.userId);
    }
    return new Empty();
  }

  @Override @Internal public Empty sendSummary(SummaryRequest request) throws IOException {
    User user = User.get(request.userId);
    logger.info("Sending summary email for user {}", user);
    SummaryEmail.sendTo(user);
    return new Empty();
  }

  @Override public SpacesResponse getSpaces(Empty request) throws IOException {
    User user = Users.current(false);
    ImmutableList.Builder<Space> builder = new ImmutableList.Builder<>();
    if (user != null && (user.isWoman() || user.isAdmin())) {
      builder.add(Space.WOMEN_ONLY);
    }
    builder.add(Space.EVERYONE);
    if (user != null && user.canAccess(Space.PRESENT_INSIDERS)) {
      builder.add(Space.PRESENT_INSIDERS);
    }
    return new SpacesResponse(Lists.transform(builder.build(), Space::toResponse));
  }

  @Override public Empty deleteAccount(UserRequest request) throws IOException {
    User current = Users.current(true);
    User delete = userFrom(request);
    if (!current.equals(delete) && !current.isAdmin()) throw new ClientException("Not authorized.");
    delete.transitionTo(UserState.DELETED);
    return Protos.EMPTY;
  }

  @Override public ValidStateTransitionsResponse getValidStateTransitions(UserRequest request)
      throws IOException {
    ensureAdmin();
    User user = userFrom(request);
    List<ValidStateTransitionResponse> states = user.state.validAdminTransitions()
        .stream()
        .map(UserState::toResponse)
        .collect(Collectors.toList());
    return new ValidStateTransitionsResponse(states);
  }

  @Override public Empty transitionState(TransitionStateRequest request) throws IOException {
    ensureAdmin();
    User user = User.get(request.userId);
    UserState beforeState = user.state;
    UserState afterState = UserState.valueOf(request.stateId);
    if (beforeState == afterState) {
      logger.info("User already in state {}.", afterState);
      return Protos.EMPTY;
    }
    if (!beforeState.validAdminTransitions().contains(afterState)) {
      throw new ClientException("Can't transition user to " + afterState + ".");
    }
    if (user.transitionTo(afterState)) {
      if (afterState == UserState.MEMBER && beforeState.preMembership()) {
        try {
          Users.runAsGenericAdminUserRequest(() -> {
            AppEngineUserService.welcomeNewUser(user);
          });
        } catch (Exception e) {
          logger.error("Error welcoming new user.", e);
        }
      }
    }
    return Protos.EMPTY;
  }

  private static void ensureAdmin() {
    if (!Users.current().isAdmin()) throw new ClientException("Not authorized.");
  }

  @Override public UsersResponse getFollowing(UserRequest request) throws IOException {
    return new UsersResponse(Collections.emptyList());
  }

  @Override public UsersResponse getFollowers(UserRequest request) throws IOException {
    return new UsersResponse(Collections.emptyList());
  }

  @Override public AdminSearchResponse adminSearch(AdminSearchRequest request) throws IOException {
    ensureAdmin();
    Cursor cursor = request.cursor == null ? null : Cursor.fromWebSafeString(request.cursor);
    Integer limit = request.limit == null ? 20 : request.limit;
    QueryResultIterator<User> results = Users.search(
        request.query, cursor, request.direction, limit).iterator();
    List<User> users = Lists.newArrayList(results);
    logger.info("Found " + users.size() + " results.");
    Map<User, List<Client>> clientsByUser = Clients.getClientsForUsers(users)
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(Client::user, Collectors.toList()));
    List<AdminUserResponse> responses = users.stream()
        .filter(Objects::nonNull)
        .map(user -> user.toAdminUserResponse(clientsByUser.get(user)))
        .collect(Collectors.toList());
    cursor = results.getCursor();
    Long count = null;
    if (Strings.isNullOrEmpty(request.query)) {
      try {
        Entity userStatEntity = DatastoreServiceFactory.getDatastoreService().get(
            KeyFactory.createKey("__Stat_Kind__", "User"));
        count = (Long) userStatEntity.getProperty("count");
      } catch (Exception e) {
        logger.warn("No entity statistic.");
      }
    }
    return new AdminSearchResponse(cursor == null ? null : cursor.toWebSafeString(),
        responses, count);
  }

  private static UsersResponse toUsersResponse(Iterable<User> users) {
    return new UsersResponse(Streams.stream(users)
        .map(User::toResponse)
        .collect(Collectors.toList()));
  }

  @Override public UsersResponse getFacebookFriends(Empty request) throws IOException {
    User current = Users.current();
    return toUsersResponse(FacebookFriendship.friendsOf(current));
  }

  @Override public UsersResponse getFriends(UserRequest request) throws IOException {
    User current = Users.current();
    User user = userFrom(request);
    Iterable<User> friends = Friendship.friendsOf(user);
    if (!current.isAdmin()
        && !Iterables.contains(friends, current)
        && !current.equals(user)) {
      throw new ClientException("Not allowed.");
    }
    return toUsersResponse(friends);
  }

  @Override public UsersResponse getIncomingFriendRequests(Empty request) throws IOException {
    User current = Users.current();
    return toUsersResponse(Friendship.requestsTo(current));
  }

  @Override public UsersResponse getOutgoingFriendRequests(Empty request) throws IOException {
    User current = Users.current();
    return Users.allowInvisibleUsers(() -> toUsersResponse(Friendship.requestsFrom(current)));
  }

  @Override public AddFriendResponse addFriend(UserRequest request) throws IOException {
    User current = Users.current();
    User friend = userFrom(request);
    return new AddFriendResponse(Friendship.addFriend(current, friend), friend.toResponse());
  }

  @Override public Empty removeFriend(UserRequest request) throws IOException {
    User current = Users.current();
    Friendship.removeFriend(current, userFrom(request));
    return Protos.EMPTY;
  }

  /** Converts UserRequest to User. Returns current user if no ID or phone is specified. */
  private static User userFrom(UserRequest request) {
    if (request.phoneNumber != null) return Users.getOrCreateByPhone(request.phoneNumber);
    if (request.userId != null) {
      User user = User.get(request.userId);
      if (user == null) throw new ClientException("User not found");
      return user;
    }
    return Users.current();
  }

  @Override public AddContactsResponse addContacts(AddContactsRequest request) throws IOException {
    User current = Users.current();
    List<Contact> contacts = request.contacts.stream().map(c -> Contact.from(current, c))
        .collect(Collectors.toList());
    ofy().save().entities(contacts);
    List<Key<PhoneToUser>> keys = contacts.stream()
        .map(c -> Key.create(PhoneToUser.class, c.phoneNumber))
        .collect(Collectors.toList());
    Map<Key<PhoneToUser>, PhoneToUser> index = ofy().load().keys(keys);
    List<PhoneUserResponse> results = new ArrayList<>();
    Client client = Clients.current();
    // Older iOS clients can't handle members without names.
    boolean hideNonMembers = client.platform() == Platform.IOS
        && client.iosVersion().compareTo(IosVersion.V4_1_B2) < 0;
    return Users.allowInvisibleUsers(() -> {
      for (PhoneToUser entry : index.values()) {
        User user = entry.user.get();
        if (hideNonMembers && user.state == UserState.INVITED) continue;
        if (user != null) results.add(new PhoneUserResponse(entry.phoneNumber, user.toResponse()));
      }
      return new AddContactsResponse(results);
    });
  }

  @Override public Empty setOtherGender(Empty request) throws IOException {
    Users.current().inTransaction(u -> {
      u.gender = Gender.OTHER;
    });
    return Protos.EMPTY;
  }
}
