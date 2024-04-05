package present.server.model.group;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.common.geometry.S2LatLng;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.cmd.Query;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.ActivityType;
import present.proto.ContentResponse;
import present.proto.GroupLog;
import present.proto.GroupMemberPreapproval;
import present.proto.GroupMembershipState;
import present.proto.GroupResponse;
import present.proto.UserResponse;
import present.server.KeysOnly;
import present.server.model.BasePresentEntity;
import present.server.phone.PhoneServices;
import present.server.RequestHeaders;
import present.server.ShortLinks;
import present.server.Time;
import present.server.model.Space;
import present.server.model.comment.Comment;
import present.server.model.comment.GroupView;
import present.server.model.comment.GroupViews;
import present.server.model.comment.Comments;
import present.server.model.content.Content;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.Privileges;
import present.server.model.user.UnreadStates;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;
import present.server.model.util.Coordinates;
import present.server.model.util.SuggestedLocation;
import present.server.notification.Notification;
import present.server.notification.Notifier;
import present.wire.rpc.core.ClientException;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.proto.Platform.ANDROID;
import static present.proto.Platform.IOS;

/**
 * Represents a group, a location-based chat room.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class Group extends BasePresentEntity<Group> {

  private static final Logger logger = LoggerFactory.getLogger(Group.class);

  /**
   * Format: UUID[:latitude,longitude]
   *
   * The optional location suffix is no longer used but still present in legacy IDs.
   */
  @Id public String id;

  /** Space ID. Women only if null. */
  @Index public String spaceId;

  /** User who owns this group. */
  @Load(unless = KeysOnly.class) @Index public Ref<User> owner;

  /** Number of times users have saved this group. */
  @AlsoLoad("favoriteCount") public int memberCount;

  /** Group title. */
  @Index public String title;

  /** Location of this group. */
  @AlsoLoad("center") public Coordinates location;

  /** S2 leaf cell ID for location. See S2CellId. */
  @Index public long s2CellId;

  /** The user's location when they created the group. */
  public Coordinates createdFrom;

  /** Name of the location. */
  @Index public String locationName;

  /** Schedule for event happening in circle */
  public Schedule schedule;

  /** Location returned by search. Null if the location is custom. */
  public SuggestedLocation suggestedLocation;

  /** A cover photo associated with the Group. Null if there is no cover photo. */
  @Load(unless = KeysOnly.class) public Ref<Content> coverContent;

  /** Description text supplied by the owner. */
  public String description;

  public List<String> categories = new ArrayList<>();

  /**
   * The epoch time in 30-day months someone last commented on or joined this group. Enables us to
   * bucket groups by month and ignore groups that haven't seen activity in a long time.
   */
  @Index public long lastUpdateMonth;

  /** Whether or not this group is discoverable by friends and people nearby. */
  @Index public boolean discoverable = true;

  /** Who can be pre-approved. */
  public GroupMemberPreapproval preapprove = GroupMemberPreapproval.ANYONE;

  /** Number of pending join requests. Used to badge groups for owners. */
  public int joinRequests;

  /**
   * Time of last comment.
   *
   * Warning! Indexing this field will cause scalability problems down the road, as we write it
   * every time a user comments, creating a hot spot in our architecture. See
   * https://goo.gl/UW2XLB.
   *
   * We can address this by partitioning the index or writing a custom server.
   */
  @Index public long lastCommentTime = System.currentTimeMillis();

  /** Index of the last active comment. */
  public int lastCommentIndex = -1;

  /**
   * Total number of comments in this group, including deleted comments. Used to compute comment
   * indices.
   */
  @AlsoLoad("commentCount") public int totalComments;

  /** Number of non-deleted comments. Used for display purposes. */
  public int activeComments;

  /** The most recent significant comment */
  public Ref<Comment> lastSignificantComment;

  /**
   * True if this group involves friends somehow. This field is a hack until we have a better
   * way of finding friends' groups.
   */
  @Ignore public boolean involvesFriends;




  public boolean hasJoinRequests() {
    if (joinRequests < 0) logger.error("Join requests < 0");
    return joinRequests <= 0;
  }

  public String uuid() {
    return Groups.getUuidFromId(this.id);
  }

  public Space space() {
    if (this.spaceId == null) return Space.WOMEN_ONLY;
    return Space.get(this.spaceId);
  }

  /**
   * Returns true if this group is discoverable, preapproved for anyone, and in the everyone space.
   */
  public boolean isPublic() {
    return discoverable
        && preapprove == GroupMemberPreapproval.ANYONE
        && space() == Space.EVERYONE;
  }

  public boolean hasCoverContent() {
    return coverContent != null && coverContent.get() != null;
  }

  /**
   * Returns a link to the cover photo scaled to the given width. Returns a map if there is no
   * cover photo.
   */
  public String coverPhoto(int width) {
    int height = width * 9 / 16;
    if (coverContent != null)
      return coverContent.get().squareUrl(width);
    return mapImageUrl(width, height);
  }

  public void setLocation(Coordinates location) {
    this.location = location;
    this.s2CellId = location.toS2CellId();
  }

  public Coordinates location() {
    return location;
  }

  /** Returns the most recent non-deleted comments by visible users. */
  public List<Comment> activeComments(boolean transitive) {
    return Comments.getComments(this, null, transitive);
  }

  // Updates the last significant comment.
  public void updateLastSignificantComment() {
    Comment comment = Streams.stream(this.activeComments(false))
        // Allow content only to be set as significant (even if there's no text)
        .filter(c -> c.isSignificant(Comments.duplicateExists(c)))
        .findFirst()
        .orElse(null);
    this.inTransaction(g -> {
      g.lastSignificantComment = comment == null ? null : comment.getRef();
      return true;
    });
    logger.info("Group \""
        + title
        + "\" was "
        + "updated. New significant comment: "
        + (comment == null ? null : comment.text));
  }

  public static Query<Group> query() {
    return ofy().load().type(Group.class);
  }

  public static Group get(Key<Group> key) {
    return ofy().load().key(key).now();
  }

  public static Key<Group> keyFor(String id) {
    return Key.create(Group.class, id);
  }

  public boolean isWomenOnly() {
    return spaceId == null || spaceId.equals(Space.WOMEN_ONLY.id);
  }

  public GroupResponse toResponseFor(User user) {
    return toResponseFor(user, g -> {
      if (g.hasCoverContent()) {
        Content cc = g.coverContent.get();
        Client client = Clients.current();
        boolean resizeCover = client.platform() == IOS
            || (client.platform() == ANDROID && client.androidVersion() < 122);
        String url = resizeCover ? cc.squareUrl(1080) : cc.url();
        return new ContentResponse(cc.uuid, cc.type, url, null);
      }
      return null;
    });
  }

  public GroupResponse toResponseFor(User user, Function<Group, ContentResponse> getCover) {
    //// If the user is logged out, they shouldn't see non-discoverable groups. We can't relax this
    //// because group URLs are guessable!
    //if (user == null && !discoverable) {
    //  logger.warn("Tried to render non-discoverable group: " + uuid());
    //  throw new ClientException("Not allowed.");
    //}

    // Note: If the user is logged out, they can see some attributes of women-only groups (used
    // for landing pages). If the user is logged in, they shouldn't see women-only groups at all.
    if (user != null && !user.canAccess(space())) {
      logger.warn("Tried to render inaccessible group: " + uuid());
      throw new ClientException("Not allowed.");
    }

    boolean newlyCreated = user != null && createdTime > user.lastSessionTime();
    List<String> categories = Lists.newArrayList(this.categories);
    if (involvesFriends) {
      categories.add(Category.Dynamic.FRIENDS);
    }
    if (newlyCreated) {
      // Add synthetic "new" category.
      categories.add(Category.Dynamic.NEW);
    }
    if (isWomenOnly()) {
      categories.add(Category.Dynamic.WOMEN_ONLY);
    }

    // TODO: Abstract location when user is logged out.
    GroupResponse.Builder builder = new GroupResponse.Builder()
        .uuid(uuid())
        .type(GroupResponse.Type.CIRCLE)
        .space(space().toResponse())
        .location(location().toProto())
        .locationName(locationName)
        .radius(0.0)
        .title(title)
        .cover(getCover.apply(this))
        .description(description)
        .url(new ShortLinks().to(this))
        .creationTime(createdTime)
        .preapprove(preapprove)
        .discoverable(discoverable)
        .deleted(deleted)
        .categories(categories)
        .joinRequests(joinRequests)

        // TODO: Hide these from clients that aren't logged in.
        .memberCount(memberCount)
        .totalComments(lastCommentIndex + 1)
        .commentCount(activeComments)
        .lastCommentTime(lastCommentTime)
        .owner(this.owner.get().toResponse(false));
    if (user != null) {
      GroupView view = user.viewOf(this);
      if (view == null) view = new GroupView(user, this);
      GroupMembershipState membershipState = user.membershipIn(this);
      boolean joined = membershipState == GroupMembershipState.ACTIVE;
      boolean unread = joined
          && (view.lastRead == null || view.lastRead() < lastCommentIndex());
      // TODO: Set fields to null if the group isn't joined.
      builder
        .schedule(schedule == null ? null : schedule.toProto())
        .unread(unread)
        .unreadCount(view.unreadCount())
        .lastRead(view.lastRead())
        .muted(view.muted)
        .joined(joined)
        .membershipState(membershipState)
        .newlyCreated(newlyCreated);
    }
    return builder.build();
  }

  public Result<Void> delete() {
    Privileges.assertUserOwns(this);
    return super.delete();
  }

  private final Supplier<List<Key<User>>> memberKeys = Suppliers.memoize(() -> {
    List<Key<GroupMembership>> keys
        = GroupMembership.query(this, GroupMembershipState.ACTIVE).keys().list();
    return Lists.transform(keys, GroupMembership::toUser);
  });

  /** Asynchronously retrieves all group member keys. */
  public List<Key<User>> memberKeys() {
    return memberKeys.get();
  }

  public boolean isMember(User user) {
    GroupMembership gm = GroupMembership.get(user, this);
    return gm != null && gm.state == GroupMembershipState.ACTIVE;
  }

  /** Returns the members who have this group saved, for display. */
  public List<User> getMembers(int limit) {
    User current = Users.current();
    return GroupMembership.query(this, GroupMembershipState.ACTIVE)
        .limit(limit)
        .list()
        .stream()
        .map(GroupMembership::member)
        .filter(Objects::nonNull) // Ignore missing users
        .filter(current::canSeeInGroup)
        .collect(Collectors.toList());
  }

  public List<UserResponse> getMembersResponse(int limit) {
    return Lists.newArrayList(User.toResponses(getMembers(limit), false));
  }

  /** Checks access by the given user to this group's content. */
  public void checkContentAccess(User user) {
    if (user == null) {
      if (isPublic()) {
        return;
      } else {
        throw new ClientException("Not allowed");
      }
    }

    user.checkContentAccess(this);
  }

  public String shortLink() {
    return new ShortLinks().to(this);
  }

  /**
   * A public URL for a map of the location of this group.
   * e.g. May be used in the admin console or web client.
   */
  public String mapImageUrl(int width, int height) {
    // TODO: This key is referrer-restricted, however the images still seem to work in Gmail,
    // when requested directly, etc. We should serve the map up from our own server so we
    // can protect the key.
    String key = "xxx";
    String iconUrl = "https://storage.googleapis.com/present-production/android/location-pin.png";
    return "https://maps.googleapis.com/maps/api/staticmap"
        // Divide width and height by 2 since we generate 2X density.
        + "?size=" + (width / 2) + "x" + (height / 2) + "&scale=2"
        + "&zoom=15"
        + "&maptype=roadmap"
        + "&markers=icon:" + iconUrl + "%7C" + location.latitude + "," + location.longitude
        + "&key="+key;
  }

  /*
   * Group view management
   */

  /** Gets or creates the user's view of this group. */
  public GroupView getOrCreateView(User user) {
    GroupView view = getView(user);
    if (view != null) return view;
    return new GroupView(user, this);
  }

  /** Gets the user's view of this group. */
  @Nullable public GroupView getView(User user) {
    return ofy().load().key(viewKeyFor(user.getKey())).now();
  }

  /** Returns the key for given user's view of this container. */
  public Key<GroupView> viewKeyFor(Key<User> userKey) {
    return Key.create(userKey, GroupView.class, uuid());
  }

  /** Index of the last comment. */
  public int lastCommentIndex() {
    return lastCommentIndex;
  }

  /** Increments the comment counter and returns the index of the next comment. */
  public int nextCommentIndex() {
    lastCommentTime = System.currentTimeMillis();
    lastCommentIndex = totalComments;
    return totalComments++;
  }

  /** Deletes the given comment and updates dependent state within this group. */
  public void delete(Comment comment) {
    ofy().transact(() -> {
      Preconditions.checkArgument(comment.group().equals(this));

      Group group = reload();

      if (comment.reload().deleted) {
        // Comment was already deleted.
        return;
      }

      // Delete the comment now so it isn't returned when we look up the next active comment.
      comment.delete().now();

      if (comment.sequence == group.lastCommentIndex) {
        group.updateLastCommentIndex();
      }

      group.activeComments--;

      group.save();
    });
  }

  public void updateLastCommentIndex() {
    // Point to the next active comment. Default to -1 (no active comments).
    this.lastCommentIndex = -1;
    this.lastCommentTime = createdTime;
    for (Comment c : activeComments(false)) {
      this.lastCommentIndex = c.sequence;
      this.lastCommentTime = c.createdTime;
      break;
    }
  }

  /*
   * Group event log. Used in ranking.
   */

  /** Maximum number of entries to keep in the log. */
  private static final int MAX_LOG_LENGTH = 20;

  /** Maximum number of entries per type. */
  private static final int MAX_ENTRIES_PER_TYPE = 5;

  /** Maximum age in millis of a log entry. */
  private static final long MAX_LOG_AGE = Time.MONTH_IN_MILLIS * 3;

  /**
   * Logs an event in this group (outside of any transactions).
   */
  public void log(GroupLog.Entry.Type eventType) {
    if (ofy().getTransaction() != null) {
      // Note: I tried ofy().transactionless(), but it caused weird errors in development.
      throw new IllegalStateException("This shouldn't run in a transaction.");
    }

    User user = Users.current(false);
    if (user == null) return; // Test

    // Ignore owner activity.
    if (user.getRef().equals(owner)) return;

    // Compute distance of the user from the group.
    present.proto.Coordinates userLocation = RequestHeaders.current().location;
    Double distance = null;
    if (userLocation != null) {
      S2LatLng userS2LatLng = S2LatLng.fromDegrees(userLocation.latitude, userLocation.longitude);
      distance = location.toS2LatLng().getEarthDistance(userS2LatLng);
    }

    // Add the entry to the log.
    Log log = getLog();
    GroupLog.Entry newEntry = new GroupLog.Entry(
        user.shortId, eventType, System.currentTimeMillis(), distance);
    List<GroupLog.Entry> entries = new ArrayList<>(MAX_LOG_LENGTH);
    entries.add(newEntry);

    // Re-add existing entries. Filter out expired entries and entries of the same type from
    // the same user. If a user comments multiple times, we'll keep just the latest.
    long minTimestamp = System.currentTimeMillis() - MAX_LOG_AGE;
    TypeHistogram histogram = new TypeHistogram();
    histogram.count(newEntry);
    log.log.entries.stream()
        .filter(e -> e.timestamp > minTimestamp)
        .filter(histogram::count)
        .filter(e -> !sameUserAndType(newEntry, e))
        .limit(MAX_LOG_LENGTH - 1)
        .forEach(entries::add);
    log.log = new GroupLog(entries);

    ofy().save().entity(log);
  }

  private static class TypeHistogram {
    private static final int LENGTH = GroupLog.Entry.Type.values().length;
    private final int[] counts = new int[LENGTH];

    /** Counts the entry's type. Returns true if we're still under the limit. */
    private boolean count(GroupLog.Entry entry) {
      return counts[entry.type.getValue() - 1]++ <= MAX_ENTRIES_PER_TYPE;
    }
  }

  private static final GroupLog EMPTY_LOG = new GroupLog(Collections.emptyList());

  public Log getLog() {
    Log log = ofy().load().type(Log.class).id(uuid()).now();
    if (log == null) {
      log = new Log();
      log.id = this.uuid();
      log.log = EMPTY_LOG;
    }
    return log;
  }

  private boolean sameUserAndType(GroupLog.Entry a, GroupLog.Entry b) {
    return a.userId.equals(b.userId) && a.type == b.type;
  }

  /** Creates a Notifier that sends notifications to the group from the given user. */
  public Notifier notifierFrom(User from) {
    // The only thing that blocks here is querying the member keys.
    List<Key<User>> memberKeys = memberKeys();
    Map<Key<User>, GroupView> views
        = GroupViews.viewsFor(this, memberKeys);
    return Notifier.from(from)
        .toKeys(memberKeys)
        .enable(user -> {
          GroupView view = views.get(user.getKey());
          boolean muted = view != null && view.muted;
          return !muted;
        });
  }

  /** Returns true if this request is pre-approved. */
  public boolean isPreapproved(User member) {
    List<User> friends;
    switch (this.preapprove) {
      case ANYONE:
        return true;
      case FRIENDS:
        friends = member.friends();
        return friends.contains(this.owner.get());
      case FRIENDS_OF_MEMBERS:
        friends = member.friends();
        Set<Key<User>> memberKeys = new HashSet<>(this.memberKeys());
        for (User friend : friends) {
          if (memberKeys.contains(friend.getKey())) return true;
        }
        return false;
      case INVITE_ONLY:
        return false;
      default: throw new AssertionError();
    }
  }

  /** Joins the group on behalf of the given user. */
  public GroupMembershipState join(User user) {
    // Prevent male admins from accidentally joining women only circles.
    if (isWomenOnly() && !user.isWoman()) {
      logger.warn("Ignoring request for non-woman to join women-only group.");
      return GroupMembershipState.REJECTED;
    }

    // Prevent non-women from trying to join women-only groups.
    if (!user.canAccess(space())) throw new ClientException("Unauthorized");

    Notifier groupNotifier = notifierFrom(user);
    Notifier ownerNotifier = Notifier.from(user).to(this.owner.get());

    boolean preapproved = isPreapproved(user);

    GroupMembershipState state = ofy().transact(() -> {
      GroupMembership gm = GroupMembership.getOrCreate(user, this);
      switch (gm.state) {
        case NONE:
        case UNJOINED:
        case INVITED:
          if (preapproved || gm.state == GroupMembershipState.INVITED) {
            gm.changeState(GroupMembershipState.ACTIVE);
            inTransaction(g -> { g.memberCount++; });
            if (user.isVisible()) {
              groupNotifier.stage(new Notification()
                  .title(this.title)
                  .body(user.firstName + " joined.")
                  .put(this)
                  .event(ActivityType.USER_JOINED_GROUP,
                      user.firstName + " joined '" + this.title + ".'", this));
            }
          } else {
            gm.changeState(GroupMembershipState.REQUESTED);
            inTransaction(g -> { g.joinRequests++; });
            ownerNotifier.stage(new Notification()
                .title(this.title)
                .body(user.firstName + " requested to join.")
                .put(this)
                .event(ActivityType.GROUP_MEMBERSHIP_REQUEST,
                    user.firstName + " requested to join '" + this.title + ".'", this));
          }
          break;

        case REQUESTED: // Duplicate
        case ACTIVE:    // Duplicate
        case REJECTED:  // Not allowed.
          break;

        default: throw new AssertionError();
      }

      return gm.state;
    });

    if (state == GroupMembershipState.ACTIVE) {
      this.log(GroupLog.Entry.Type.JOIN);
    }

    if (state == GroupMembershipState.REQUESTED) {
      UnreadStates.markGroupUnread(this.owner.get(), this.id);
    }

    ownerNotifier.commit();
    groupNotifier.commit();

    logger.info("Join requests: " + joinRequests);

    return state;
  }

  public void leave(User user) {
    User owner = this.owner.get();
    if (user.equals(owner)) {
      throw new ClientException("User can't leave owned group.");
    }

    GroupMembershipState state = ofy().transact(() -> {
      GroupMembership gm = GroupMembership.getOrCreate(user, this);
      switch (gm.state) {
        case NONE:
        case REJECTED:
        case UNJOINED:
          break;

        case REQUESTED:
          gm.changeState(GroupMembershipState.NONE);
          inTransaction(g -> { g.joinRequests--; });
          break;

        case ACTIVE:
          gm.changeState(GroupMembershipState.UNJOINED);
          inTransaction(g -> { g.memberCount--; });
          break;

        case INVITED:
          gm.changeState(GroupMembershipState.UNJOINED);
          break;

        default: throw new AssertionError();
      }
      return gm.state;
    });

    if (state == GroupMembershipState.NONE) {
      getOrCreateView(this.owner.get()).membersChanged();
    }

    if (state == GroupMembershipState.UNJOINED) {
      log(GroupLog.Entry.Type.LEAVE);
    }

    logger.info("Join requests: " + joinRequests);
  }

  public void addMembers(User host, Iterable<User> users) {
    Notifier groupNotifier = this.notifierFrom(host);

    // Ensure this user is a member of the group.
    if (!isMember(host)) {
      throw new ClientException("Not authorized.");
    }

    /*
     * TODO: To make this transactional, we need to make GroupMembership a child of Group, which
     * will require a migration.
     */

    Map<User, GroupMembership> memberships = GroupMembership.load(this, users);
    List<User> newMembers = new ArrayList<>();
    int removedRequests = 0;
    for (GroupMembership membership : memberships.values()) {
      // TODO: Ensure host is an admin or host is a friend of member.
      // TODO: Don't allow user to be added if they unjoined?
      if (membership.state != GroupMembershipState.ACTIVE) {
        if (membership.state == GroupMembershipState.REQUESTED) {
          removedRequests++;
        }
        membership.state = GroupMembershipState.ACTIVE;
        membership.host = host.getRef();
        newMembers.add(membership.member());
      }
    }

    ofy().save().entities(memberships.values()).now();

    // Note: If called concurrently, the member and join request counts will get out of sync.
    int finalRemovedRequests = removedRequests;
    if (!newMembers.isEmpty() || finalRemovedRequests > 0) {
      inTransaction(g -> {
        g.memberCount += newMembers.size();
        g.joinRequests -= finalRemovedRequests;
      });
    }

    if (!newMembers.isEmpty()) {
      // TODO: Send updated badge count to owner.
      getOrCreateView(this.owner.get()).membersChanged();

      // Notify new members.
      Notifier newMemberNotifier = Notifier.from(host).to(newMembers);
      newMemberNotifier.send(new Notification()
          .title(this.title)
          .body(host.firstName + " added you")
          .put(this)
          .event(ActivityType.GROUP_MEMBERSHIP_APPROVAL,
              host.fullName() + " added you to '" + this.title + "'", this));

      for (User newMember : newMembers) {
        if (newMember.state == UserState.INVITED) {
          String sms = host.fullName() + " added you to the '" + this.title + "' circle on Present: "
              + shortLink();
          PhoneServices.sms(newMember, sms);
        }
      }

      // Notify other members.
      List<User> activeMembers = newMembers.stream().filter(u -> u.state == UserState.MEMBER)
          .collect(Collectors.toList());
      if (!activeMembers.isEmpty()) {
        String firstNames = toFirstNames(activeMembers);
        String message = host.firstName + " added " + firstNames;
        groupNotifier.send(new Notification()
            .title(this.title)
            .body(message)
            .put(this)
            .event(ActivityType.GROUP_MEMBERSHIP_APPROVAL,
                host.firstName + " added " + firstNames + " to '" + this.title
                    + "'", this));
      }
    }

    logger.info("Join requests: " + joinRequests);
  }

  private static String toFirstNames(List<User> users) {
    Preconditions.checkArgument(!users.isEmpty());
    if (users.size() == 1) return users.get(0).firstName;
    List<String> firstNames = Lists.transform(users, u -> u.firstName);
    String names = Joiner.on(", ").join(firstNames.subList(0, users.size() - 1));
    return names + " and " + firstNames.get(users.size() - 1);
  }

  public void removeMembers(User host, Iterable<User> members) {
    User owner = this.owner.get();

    // Ensure this user is a member of the group.
    if (!isMember(host)) {
      throw new ClientException("Not authorized.");
    }

    Map<User, GroupMembership> memberships = GroupMembership.load(this, members);
    List<User> removedMembers = new ArrayList<>();
    int removedRequests = 0;
    for (GroupMembership membership : memberships.values()) {
      User member = membership.member();

      if (owner.equals(member)) throw new ClientException("Can't remove owner.");
      if (host.equals(member)) throw new ClientException("Can't remove yourself.");

      if (membership.state == GroupMembershipState.ACTIVE) {
        removedMembers.add(member);
      }
      if (membership.state == GroupMembershipState.REQUESTED) {
        removedRequests++;
      }
      membership.host = host.getRef();
      membership.state = GroupMembershipState.REJECTED;
    }

    int finalRemovedRequests = removedRequests;
    if (removedRequests > 0) {
      inTransaction(g -> {
        g.joinRequests -= finalRemovedRequests;
      });
      getOrCreateView(owner).membersChanged();
    }

    // Note: If something goes wrong, the member count will get out of sync.
    inTransaction(g -> { g.memberCount -= removedMembers.size(); });

    ofy().save().entities(memberships.values());

    logger.info("Join requests: " + joinRequests);
  }

  /**
   * Log of events in a group. We store this in a separate entity to avoid write contention
   * on the Group. We write this concurrently outside of a transaction, so we may drop
   * events when under load.
   */
  @Entity(name=Log.KIND) @Cache public static class Log {

    private static final String KIND = "GroupLog";

    /** Same as the Group ID. */
    @Id public String id;

    /** Log of recent events in this group. */
    public GroupLog log;

    public static Key<Group.Log> keyFor(Key<Group> groupKey) {
      // Avoid reflection overhead by creating the key directly.
      return Key.create(com.google.appengine.api.datastore.KeyFactory.createKey(Log.KIND,
          Groups.getUuidFromId(groupKey.getName())));
    }
  }

  // Fields that may be used in filter queries as strings
  public static class Fields {

    private Fields() {}

    public static Field id = get("id");
    public static Field owner = get("owner");
    public static Field title = get("title");
    public static Field lastUpdateMonth = get("lastUpdateMonth");
    public static Field s2CellId = get("s2CellId");
    public static Field spaceId = get("spaceId");
    public static Field discoverable = get("discoverable");

    private static Field get(String fieldName) {
      try {
        return Group.class.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override public int hashCode() {
    return this.id.hashCode();
  }

  @Override public boolean equals(Object other) {
    return other instanceof Group && this.id.equals(((Group) other).id);
  }

  public Key<Group> getKey() {
    // Overridding improves performance vs. constructing the key reflectively.
    return Key.create(Group.class, id);
  }

  @Override protected Group getThis() {
    return this;
  }

  /** Load type for loading comments */
  public static class LastComment { }
}
