package present.server.model.user;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.geometry.S2LatLng;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.condition.IfNotNull;
import com.googlecode.objectify.condition.IfTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.AdminUserResponse;
import present.proto.City;
import present.proto.ContentReferenceRequest;
import present.proto.ContentType;
import present.proto.EmailAddress;
import present.proto.FriendResponse;
import present.proto.Gender;
import present.proto.GroupMemberPreapproval;
import present.proto.GroupMembershipState;
import present.proto.GroupService;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.proto.UnreadState;
import present.proto.UserName;
import present.proto.UserNotificationSettings;
import present.proto.UserProfile;
import present.proto.UserProfileRequest;
import present.proto.UserResponse;
import present.server.Cities;
import present.server.KeysOnly;
import present.server.MoreObjectify;
import present.server.NearbyCity;
import present.server.RequestHeaders;
import present.server.ShortLinks;
import present.server.Time;
import present.server.Uuids;
import present.server.facebook.Facebook;
import present.server.facebook.FacebookException;
import present.server.facebook.FacebookFriendship;
import present.server.facebook.FacebookUserData;
import present.server.model.BasePresentEntity;
import present.server.model.Space;
import present.server.model.ZipCode;
import present.server.model.comment.Comment;
import present.server.model.comment.GroupView;
import present.server.model.content.Content;
import present.server.model.geocoding.Geocoding;
import present.server.model.group.Group;
import present.server.model.group.GroupMembership;
import present.server.model.group.GroupSearch;
import present.server.model.util.Address;
import present.server.model.util.Coordinates;
import present.server.notification.Notifier;
import present.wire.rpc.core.ClientException;
import present.wire.rpc.core.ServerException;

import static com.google.common.base.Objects.equal;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.model.util.Operations.firstIfNotNull;

/**
 * A user. We create a User instance the moment a user identifies themselves. See {@link #state}
 * to detemine a user's state.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class User extends BasePresentEntity<User> {

  private static final Logger logger = LoggerFactory.getLogger(User.class);

  /** Field names that may be used in filter queries as strings */
  public enum Fields { uuid, email, phoneNumber, facebookId, firstName, lastName, notificationSettings, signupTime, state  }

  /** We will not allow a user to be registered without a photo, but this is a fallback. */
  public static String MISSING_PHOTO_URL
      = "https://lh3.googleusercontent.com/VWF298GMl2MtkZqr1VAY6rjJQJpNmsoUyFK7DnIoTOIqzksuhM6e8gTZ15wft0lajniBQXsLDH--XtRs0MLUcmoK1vdMmMQF";

  /** The uuid */
  @Id public String uuid;

  /** Did we just create this user? */
  @Ignore public boolean nascent;

  /** The user's external facebook user id as used in the FB graph API. */
  @Index public String facebookId; // e.g. "102419537036712"

  /** The user Facebook access token, which may be updated by the client periodically. */
  public String facebookAccessToken;

  /** The user's normalized phone number. */
  @Index public String phoneNumber;

  /** User first (given) name as entered by the user. */
  @Index public String firstName;

  /** User last (family) name as entered by the user. */
  @Index public String lastName;

  /** The user's state. */
  @Index public UserState state = UserState.SIGNING_UP; // Initial state

  /** Indicate for which activities the user should receive push notifications */
  @Index public NotificationSettings notificationSettings = new NotificationSettings();

  /** The user's avatar photo. Null if no photo has yet been set. */
  @Load(unless = KeysOnly.class) public Ref<Content> photo;

  /** Gender identity. Used to restrict access to women-only content. */
  public Gender gender;

  /** The user's self description. */
  public String bio;

  /** Categories that the user has chosen as interests. */
  public List<String> interests = new ArrayList<>();

  /** Location from which the user signed up */
  public Coordinates signupLocation;
  @Index(IfNotNull.class) public Long signupCellId;

  public void setSignupLocation(Coordinates location) {
    this.signupLocation = location;
    this.signupCellId = location.toS2CellId();
  }

  /** Reverse geocoded signup location */
  public Address signupAddress;

  /** Indexed signup time */
  @Index public long signupTime = createdTime;

  /** Administrative privileges */
  public Privileges privileges = new Privileges();

  /** 5-digit zip code */
  public String zip;

  public enum SignupLocationSource { GPS, ZIP }

  /** Source of signup location. */
  public SignupLocationSource signupLocationSource;

  @Index public String email;

  public Review review;

  /** Number of times we've computed the unread count from scratch. */
  public int unreadVersion;

  /** When a user's client last called UserService.synchronize(). */
  public long lastActiveTime;

  /**
   * When the user last called UserService.synchronize(), prior to 24 hours of inactivity.
   * Can be 0 for legacy users.
   */
  public long lastSessionTime;

  /** Number of pending friend requests. */
  public int incomingFriendRequests;

  /** True if the user messaged us to "stop" SMS. */
  public boolean smsStopped;

  /** Most-recently used mobile client. */
  @Load(Client.class) public Ref<Client> mobileClient;

  public User() {}

  public String uuid() {
    return uuid;
  }

  @Override public Key<User> getKey() {
    // Overridding improves performance vs. constructing the key reflectively.
    return Key.create(User.class, uuid);
  }

  /** States of users who have access to the network. */
  public static final Set<UserState> HAS_ACCESS = EnumSet.of(UserState.MEMBER, UserState.GHOSTED);

  /** Returns true if this user has access to content on the network. */
  public boolean hasAccess() {
    return HAS_ACCESS.contains(state);
  }

  /** Returns true if this user's content is visible to others. */
  public boolean isVisible() {
    switch (state) {
      case MEMBER:
      case SUSPENDED: return true;
      default: return false;
    }
  }

  public Gender gender() {
    if (this.gender == null) {
      return Gender.UNKNOWN;
    }
    return gender;
  }

  public boolean isWoman() {
    return gender() == Gender.WOMAN;
  }

  public boolean isAdmin() {
    return privileges != null && privileges.isAdmin;
  }

  public String publicName() {
    if (lastName == null && firstName == null) return null;
    if (lastName == null) return firstName;
    return firstName + " " + lastName;
  }

  public String fullName() {
    if (Strings.isNullOrEmpty(firstName)) {
      return Strings.isNullOrEmpty(lastName) ? "(Name Missing)" : lastName;
    } else {
      return Strings.isNullOrEmpty(lastName) ? firstName : firstName + " " + lastName;
    }
  }

  public UserName name() {
    return new UserName(Objects.toString(firstName, ""),
        Objects.toString(lastName, ""));
  }

  @Ignore private Supplier<FacebookUserData> facebookSupplier = Suppliers.memoize(
      () -> FacebookUserData.get(facebookId));

  public FacebookUserData facebookData() {
    return facebookSupplier.get();
  }

  public String profilePhotoUrl() {
    return photo != null ? photo.get().url() : MISSING_PHOTO_URL;
  }

  public String profilePhotoUrl(int width) {
    int height = width;
    return photo != null ? photo.get().squareUrl(width) : MISSING_PHOTO_URL;
  }

  public Gender getGenderIdentification() {
    return Facebook.genderOf(facebookData());
  }

  public @Nullable EmailAddress emailAddress() {
    String email = email();
    return email == null ? null : new EmailAddress.Builder().email(email).name(fullName()).build();
  }

  public @Nullable String email() { return this.email; }

  public List<Client> clients() { return Clients.getClientsForUser(getKey()); }

  @Ignore private Supplier<Map<Key<Group>, GroupMembership>> groupMemberships
      = Suppliers.memoize(this::loadGroupMemberships);

  /** Asynchronously loads group memberships. */
  private Map<Key<Group>, GroupMembership> loadGroupMemberships() {
    List<GroupMembership> groups = GroupMembership.query(this).list();
    return MoreObjectify.lazyMap(() -> Maps.uniqueIndex(groups, gm -> gm.group.getKey()));
  }

  /** Returns the user's membership state in the given group. */
  public GroupMembershipState membershipIn(Group group) {
    Map<Key<Group>, GroupMembership> memberships = this.groupMemberships.get();
    GroupMembership membership = memberships.get(group.getKey());
    return membership == null ? GroupMembershipState.NONE : membership.state;
  }

  /**
   * Get the user's joined groups. The result is suitable for display to the user,
   * excluding any deleted or hidden.
   */
  public List<Group> joinedGroups() {
    // Kick off query asynchronously.
    Map<Key<Group>, GroupMembership> memberships = groupMemberships.get();
    return MoreObjectify.lazyList(() -> {
      return memberships.values().stream()
          .filter(gm -> gm.state == GroupMembershipState.ACTIVE)
          .map(GroupMembership::group)
          .filter(java.util.Objects::nonNull)
          .filter(g -> !g.isDeleted())
          // TODO: Move this up the stack? Other users can call this method.
          .filter(g -> canSee(g.owner.get()))
          .collect(Collectors.toList());
    });
  }

  /**
   * Add the group to the user's joined group set if it is not already included.
   *
   * This method does *not* generate notifications or events related to the save (join).
   * @see present.server.AppEngineGroupService
   *
   * @return true if the group was saved.
   */
  public boolean join(Group group) {
    GroupMembership membership = GroupMembership.get(this, group);
    if (membership == null || membership.state != GroupMembershipState.ACTIVE) {
      membership = GroupMembership.newInstance(this, group);
      membership.state = GroupMembershipState.ACTIVE;
      membership.save();
      group.inTransaction(g -> { g.memberCount++; });
      return true;
    }
    return false;
  }

  /**
   * Generate a user reponse for this user.
   * @param full If false some optional details such as bio, interests, and friends graph will be excluded.
   */
  public UserResponse toResponse(boolean full) {
    User current = Users.current(false);
    boolean admin;
    if (current != null) {
      admin = current.isAdmin();
      // Notify us if we try to create a response for a user who shouldn't be seen.
      if (!current.canSee(this) && !Users.allowInvisibleUsers.get()) {
        logger.error("Oops.", new AssertionError("Rendered unexpected user."));
      }
    } else {
      admin = false;
    }

    return new UserResponse(
        uuid,
        publicName(),
        firstName,
        profilePhotoUrl(),
        bio, // TODO: Including the bio in the short form for now.
        full ? interests : Collections.emptyList(),
        Collections.emptyList(),
        admin && signupAddress != null ? signupAddress.niceString() : null,
        shortLink(),
        isVisible()
    );
  }

  public FriendResponse toFriendResponse() {
    return new FriendResponse(toResponse(false));
  }

  public UserResponse toResponse() {
    return toResponse(false);
  }

  public static Iterable<UserResponse> toResponses(Iterable<User> users, boolean full) {
    return Iterables.transform(users, user -> user.toResponse(full));
  }

  public static Iterable<UserResponse> toResponses(Iterable<User> users) {
    return toResponses(users, true);
  }

  public static Key<User> keyFor(String uuid) {
    return Key.create(User.class, uuid);
  }

  public static User get(String uuid) {
    return get(keyFor(uuid));
  }

  public static User get(Key<User> userKey) {
    return ofy().load().key(userKey).now();
  }

  public Content setPhotoFromRequest(ContentReferenceRequest photoRef) {
    if (photoRef.type != ContentType.JPEG) {
      throw new ServerException("user photo should be JPEG content");
    }
    Content photoContent = Content.get(photoRef);
    if (photoContent == null) {
      throw new ServerException("photo content not found: "+photoRef);
    }
    photo = photoContent.getRef();
    return photoContent;
  }

  /**
   * Capture an external photo from the specified URL, upload it to our content service,
   * and set the content as the user photo.
   * @param photoUrl
   */
  public void setPhotoFromExternalUrl(String photoUrl) throws IOException {
    try {
      Content content = Content.capturePhotoToContent(photoUrl, false);
      if (content != null) {
        photo = content.getRef();
      }
    } catch (IOException e) {
      logger.error("Unable to capture and assign user photo from external URL: "+e);
      throw e;
    }
  }

  public void updateFrom(UserProfileRequest request) {
    // update name
    if (request.name != null) {
      firstName = firstIfNotNull(request.name.first, firstName);
      lastName = firstIfNotNull(request.name.last, lastName);
    }

    // update photo
    if (request.photo != null) setPhotoFromRequest(request.photo);

    // update bio
    bio = firstIfNotNull(request.bio, bio);

    // update interests (if empty but non-null user may be removing interests)
    interests = firstIfNotNull(request.interests, interests);

    if (request.zip != null) {
      this.zip = ZipCode.validate(request.zip);
      if (this.signupLocation == null) {
        // Geocode zip code (captured during web signup).
        Coordinates location = Geocoding.geocodeZipCode(this.zip);
        // Store sign up location coordinates for user
        if (location != null) {
          this.setSignupLocation(location);
          // Store source of signup location as ZIP
          this.signupLocationSource = SignupLocationSource.ZIP;
        }
      }
    }

    // update notifications
    if (request.notificationSettings != null) {
      notificationSettings.userCommentsOnJoinedGroup = firstIfNotNull(
        request.notificationSettings.userCommentsOnJoinedGroup, // prefer new
        request.notificationSettings.deprecated_favoritedGroups, // then deprecated
        notificationSettings.userCommentsOnJoinedGroup); // then existing value
    }
  }

  public void updateFacebook() throws IOException, FacebookException {
    if (facebookAccessToken != null) {
      FacebookUserData facebookData = Facebook.getUserData(facebookAccessToken);
      ofy().save().entity(facebookData);
      if (this.gender == null || this.gender == Gender.UNKNOWN) {
        this.gender = facebookData.gender();
      }
      this.facebookSupplier = () -> facebookData;
      FacebookFriendship.saveFriends(this, facebookData);
    }
  }

  public UserProfile toUserProfile() {
    String photoUrl = photo == null ? null : photo.get().url();
    return new UserProfile(
        uuid, name(), photoUrl, bio, interests, Collections.emptyList(),
        notificationSettings.toUserNotificationSettings(), privileges.isAdmin,
        ShortLinks.toApp(), homeCity(), shortLink(), gender, facebookId != null,
        phoneNumber != null, incomingFriendRequests);
  }

  public AdminUserResponse toAdminUserResponse(Collection<Client> clients) {
    String photoUrl = photo == null ? null : photo.get().url();
    String gender = gender() == null ? null : gender().name();
    String signupState = signupAddress == null ? null : signupAddress.state;
    String signupCity = signupAddress == null ? null : signupAddress.city;
    Double signupLat = signupLocation == null ? null : signupLocation.latitude;
    Double signupLong = signupLocation == null ? null : signupLocation.longitude;
    Double signupLocationAccuracy = signupLocation == null ? null : signupLocation.accuracy;
    String reviewString = review == null ? null : review.description;
    List<String> availableActions = state.validAdminTransitions().stream().map(UserState::adminVerb)
        .collect(Collectors.toList());
    List<String> clientsArray = Collections.emptyList();
    Long lastActivityTime = updatedTime;
    Boolean notificationsEnabled = null;

    if (clients != null) {
      // Generate list of client platforms
      clientsArray = clients.stream()
          .filter(c -> c.platform != 0)
          .map(Client::platformToString)
          .collect(Collectors.toList());

      // Compute last updated time.
      lastActivityTime = Long.MIN_VALUE;
      for (Client c : clients) {
        if (c.deviceTokenUpdateTime > lastActivityTime) {
          lastActivityTime = c.deviceTokenUpdateTime;
        }
      }
      if (updatedTime > lastActivityTime) {
        lastActivityTime = updatedTime;
      }

      notificationsEnabled = clients.stream()
          .map(client -> client.notificationsEnabled)
          .filter(Predicates.notNull())
          .reduce(Boolean::logicalOr)
          .orElse(null);
    }

    return new AdminUserResponse(
        uuid, photoUrl, firstName, lastName, gender, phoneNumber, email,
        facebookLink(), shortLink(), signupTime, notificationsEnabled, lastActivityTime,
        signupCity, signupState, signupLat, signupLong, signupLocationAccuracy,
        state.toString(), reviewString, clientsArray, availableActions, debugString());
  }

  public City homeCity() {
    NearbyCity nearest = nearestCity();
    return nearest == null ? Cities.SAN_FRANCISCO : nearest.city;
  }

  public NearbyCity nearestCity() {
    if (this.signupLocation != null) {
      return Cities.nearestTo(signupLocation);
    } else {
      // We used to have a bug where we failed to capture signup location. Default to SF.
      logger.warn("Missing signup location.");
      return null;
    }
  }

  public SignupLocationSource signupLocationSource() {
    // For users who have no source listed for their sign up location:
    // If there are location coordinates and no zip code, assume source is GPS
    if (this.signupLocationSource == null && this.zip == null && this.signupLocation != null) {
      return SignupLocationSource.GPS;
    }
    return this.signupLocationSource;
  }

  @Ignore private Supplier<List<User>> friendSupplier = Suppliers.memoize(() -> {
    return Lists.newArrayList(Friendship.friendsOf(this))
        .stream()
        .filter(this::canSee)
        .collect(Collectors.toList());
  });

  /**
   * Return the list of Present users who have a friend relationship with this user.
   * Note: Limited to Facebook friends who are also Present users.
   */
  public List<User> friends() {
    return friendSupplier.get();
  }

  /** Returns friends within 128km of the given location. */
  public List<User> friendsNear(S2LatLng location) {
    List<User> friends = friendSupplier.get();

    // Bulk load most recent clients.
    List<Ref<Client>> clientRefs = friends.stream()
        .filter(u -> u.mobileClient != null)
        .map(u -> u.mobileClient)
        .collect(Collectors.toList());

    if (clientRefs.isEmpty()) return Collections.emptyList();

    return friends.stream()
        .filter(f -> {
          Double distance = f.distanceTo(location);
          return distance != null && distance < GroupSearch.MAX_RADIUS;
        })
        .collect(Collectors.toList());
  }

  /** Returns the distance between this user's last known location and the given location. */
  public Double distanceTo(S2LatLng location) {
    if (mobileClient == null) {
      logger.info("Missing mobile client.");
      return null;
    }
    Client client = mobileClient.get();
    S2LatLng clientLocation = client.location();
    if (clientLocation== null) {
      logger.info("Missing client location.");
      return null;
    }
    return clientLocation.getEarthDistance(location);
  }

  @Ignore private Supplier<Set<String>> friendIds = Suppliers.memoize(() ->
      friends().stream().map(User::uuid).collect(Collectors.toSet()));

  /** Returns a set of the user's friend's UUIDs. */
  public Set<String> friendIds() {
    return friendIds.get();
  }

  @Ignore private Supplier<BlockedUsers> blockedUsersSupplier = Suppliers.memoize(() ->
      BlockedUsers.loadFor(this));

  public BlockedUsers getBlockedUsers() {
    return blockedUsersSupplier.get();
  }

  /** Returns true if we can see the other user. */
  public boolean canSee(User other) {
    return notNull(other)
        && other.isVisible()
        && !blocks(other);
  }

  /** Returns true if we can see the comment. */
  public boolean canSee(Comment comment) {
    return comment != null
        && comment.author != null
        && canSee(comment.author.get());
  }

  /** Ensures user can access the given group's content. */
  public void checkContentAccess(Group group) {
    if (isAdmin()) return;

    if (!canAccess(group.space())) {
      throw new ClientException("Unauthorized");
    }

    if (group.isMember(this)) return;

    if (group.preapprove == GroupMemberPreapproval.ANYONE) return;

    throw new ClientException("Unauthorized");
  }

  @Index public List<String> spaceIds;

  /** Checks whether a user can access a group. */
  public boolean canAccess(Space space) {
    return space.isAccessibleBy(this);
  }

  /** Returns true if this user can notify the other. */
  public boolean canNotify(User other) {
    return notNull(other)
        && other.hasAccess()
        && this.state == UserState.MEMBER // Only members can trigger notifications.
        && !blocks(other);
  }

  /**
   * We shouldn't hard delete users, but in case someone does, this method enables us to handle
   * it gracefully.
   */
  private static boolean notNull(User user) {
    if (user == null) logger.error("Null user!", new NullPointerException());
    return user != null;
  }

  /**
   * Returns true if this user blocks the given user or vice versa. Lazy loads the BlockedUsers
   * entity. Limit how many different users you call this on per request.
   */
  public boolean blocks(User other) {
    BlockedUsers blockedUsers = getBlockedUsers();
    if (blockedUsers == null) return false;

    // If we blocked them, we can't see them.
    Ref<User> otherRef = other.getRef();
    if (blockedUsers.users.contains(otherRef)) return true;

    // If they blocked us, we can't see them.
    return blockedUsers.blockedBy(otherRef);
  }

  /** Returns true if we can see the other user's membership in a group. */
  public boolean canSeeInGroup(User other) {
    if (!other.isVisible()) return false;

    BlockedUsers blockedUsers = getBlockedUsers();
    if (blockedUsers == null) return true;

    // If they blocked us, we can't see them.
    return !blockedUsers.blockedBy(Ref.create(other));
  }

  /** Returns an HTTPS link to this user. */
  public String shortLink() {
    return new ShortLinks().to(this);
  }

  public String facebookLink() {
    String link = "";
    if (this.facebookId != null) {
      link = "https://www.facebook.com/app_scoped_user_id/" + this.facebookId + "/";
    }
    return link;
  }

  @Override public String toString() {
    return fullName() + " (User #" + uuid + ")";
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return equal(uuid, user.uuid);
  }

  @Override public int hashCode() { return uuid == null ? 0 : uuid.hashCode(); }

  // Specify which types of system activity generate notifications for the user.
  public static class NotificationSettings {
    public enum Fields { userCommentsOnJoinedGroup, userJoinsOwnedGroup, missedNewActivity }

    // Another user comments on a group I have joined.
    @Index(IfTrue.class) public boolean userCommentsOnJoinedGroup = true;

    // Another user joins a group that I own.
    @Index(IfTrue.class) public boolean userJoinsOwnedGroup = true;

    // I have missed new activity (summary notifications)
    @Index(IfTrue.class) public boolean affirmations = true;

    public UserNotificationSettings toUserNotificationSettings() {
      boolean favoritedGroups = userCommentsOnJoinedGroup; // legacy
      boolean contributedGroups = false;  // legacy
      boolean ownedGroups = false; // legacy
      return new UserNotificationSettings(ownedGroups, favoritedGroups, contributedGroups,
          userCommentsOnJoinedGroup, userJoinsOwnedGroup, affirmations);
    }

    public void updateFrom(UserNotificationSettings uns) {
      userCommentsOnJoinedGroup = firstIfNotNull(uns.userCommentsOnJoinedGroup, userCommentsOnJoinedGroup);
      userJoinsOwnedGroup = firstIfNotNull(uns.userJoinsOwnedGroup, userJoinsOwnedGroup);
      affirmations = firstIfNotNull(uns.affirmations, affirmations);
    }
  }

  public static class Privileges {

    // Present Admin status allows editing content owned by other users.
    @Index(IfTrue.class) public boolean isAdmin = false;

    // This user should automatically join *all* new circles.
    @Index(IfTrue.class) public boolean autoJoinsNewCircles = false;

    // Time delay amount for user to join new circles. Default is no delay. Default time unit is milliseconds.
    public long autoJoinsTimeDelay = 0L;

    // This user should automatically message *all* new users with a welcome message.
    @Index(IfTrue.class) public boolean welcomesNewUsers = false;

    // A string to be formatted by WelcomeMessageTemplate
    public String welcomesNewUsersTemplate;
  }

  /**
   * Atomically changes the user's state. Defends against old clients which invoke completeSignup()
   * twice concurrently.
   *
   * @param destination state
   * @return true if the state changed, false otherwise
   */
  public boolean transitionTo(UserState destination) {
    return inTransaction(u -> {
      if (u.state == destination) return false;
      u.state = destination;
      u.review = this.review;
      return true;
    });
  }

  /**
   * Creates a virtual client for the current user.
   *
   * @return Client object for virtual client
   */
  public Client virtualClient() {
    String virtualUuid = Uuids.fromName("Virtual Client " + uuid);
    Client client = Clients.getOrCreate(virtualUuid);
    if (client.user == null) {
      client.user = getRef();
      client.platform = Platform.VIRTUAL.getValue();
      client.apiVersion = 0;
      client.save().now();
    }
    return client;
  }

  /** Creates an RPC client that uses this user's virtual client. */
  public <T> T rpcClient(Class<T> service) {
    return virtualClient().rpcSimulator(service);
  }

  /**
   * Run provided code as the current user.
   *
   * @param r runnable to be run as user
   */
  public void run(Runnable r) {
    logger.info("Running operations with virtual client for user: {}", this.toString());
    // Save current request header fields and current user.
    RequestHeader previousRequestHeader = RequestHeaders.current();
    User previousUser = Users.current(false);
    String requestUuid;
    try {
      Client virtualClient = virtualClient();
      Platform platform = Platform.VIRTUAL;
      if (RequestHeaders.isTest()) {
        // Ensures that we continue to use the "TEST" namespace.
        platform = Platform.TEST;
      }
      if (RequestHeaders.current() == null) {
        // If called from a context where there is no request uuid, generate one.
        requestUuid = Uuids.newUuid();
      } else {
        // Otherwise use current request uuid.
        requestUuid = RequestHeaders.current().requestUuid;
      }
      RequestHeader virtualRequestHeader = new RequestHeader.Builder()
          .clientUuid(virtualClient.uuid)
          .requestUuid(requestUuid)
          .authorizationKey("not used")
          .platform(platform)
          .apiVersion(1)
          .build();
      // Set header in current thread to fields for virtual client.
      RequestHeaders.setCurrent(virtualRequestHeader);
      // Set current user.
      Users.setCurrent(this);
      r.run();
    } finally {
      // Restore previous header fields and previous user.
      RequestHeaders.setCurrent(previousRequestHeader);
      Users.setCurrent(previousUser);
    }
  }

  public void incrementUnreadVersion() {
    ofy().transact(() -> {
      User user = reload();
      user.unreadVersion++;
      user.save();
    });
  }

  /** Computes unread counts and pushes them to user's devices. */
  public UnreadState computeUnreadState() {
    logger.info("Computing unread count for {}.", this);
    UnreadState state = UnreadStates.computeFor(this);
    UnreadStates.put(this, state);
    Notifier.sendBadgeCounts(this, state);
    return state;
  }

  /** Returns the user's view of the given comment container. */
  public GroupView viewOf(Group container) {
    return container.getView(this);
  }

  @Override public Result deleteHard() {
    throw new UnsupportedOperationException("Use Users.cascadingDelete().");
  }

  public boolean hasAppleDevice() {
    return clients().stream().map(Client::platform).anyMatch(p -> p == Platform.IOS);
  }

  public boolean isTestUser() {
    return email != null && email.endsWith("tfbnw.net");
  }

  /** Updates the last active and session times. */
  public void updateLastActiveTime() {
    long now = System.currentTimeMillis();
    inTransaction(user -> {
      if (now - user.lastActiveTime > Time.DAY_IN_MILLIS) {
        // More than a day has passed since the last synchronization. Consider this the start
        // of a new session.
        user.lastSessionTime = user.lastActiveTime;
      }
      user.lastActiveTime = now;
      return true;
    });
  }

  /**
   * Returns the start time of the user's prior session, prior to 24 hours of inactivity.
   * If we don't have a last session time (in the case of legacy users), we default to
   * one week ago.
   */
  public long lastSessionTime() {
    if (this.lastSessionTime == 0) return this.lastActiveTime - Time.WEEK_IN_MILLIS;
    return this.lastSessionTime;
  }

  private static final String DEFAULT_PROFILE_PHOTO_UUID = Uuids.fromName("Default Profile Photo");

  public static Key<Content> defaultProfilePhoto() {
    return Key.create(Content.class, DEFAULT_PROFILE_PHOTO_UUID);
  }

  public void updateMobileClient(Client client) {
    if (!client.isMobile()) return;
    if (this.mobileClient != null && Key.create(client).equals(this.mobileClient.getKey())) return;

    logger.info("Updating user's mobile client.");
    inTransaction(u -> {
      u.mobileClient = Ref.create(client);
    });
  }

  @OnSave public void indexPhoneNumber() {
    if (null != this.phoneNumber) {
      PhoneToUser phoneToUser = new PhoneToUser();
      phoneToUser.phoneNumber = this.phoneNumber;
      phoneToUser.user = getRef();
      ofy().save().entities(phoneToUser).now();
    }
  }

  @Override protected User getThis() {
    return this;
  }

  public enum Review {
    // Rejected:
    MAN("Man on Facebook"),

    // Review requested:
    NOT_A_WOMAN("Not a woman on Facebook"),
    TOO_FEW_FRIENDS("< 25 friends on Facebook"),
    OUTSIDE_GEOFENCE("Outside geofence"),

    // Approved:
    EMAIL_WHITELISTED("Email whitelisted"),
    PRE_APPROVED("Approved while signing up"),
    PHONE_VERIFIED("Phone verified"),
    ZIP_WHITELISTED("Zip code whitelisted"),
    INSIDE_GEOFENCE("Inside geofence"),

    POWER_RISING("At Power Rising");

    public final String description;

    Review(String description) {
      this.description = description;
    }
  }
}
