package present.server;

import com.google.appengine.api.ThreadManager;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.common.hash.BloomFilter;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.squareup.wire.Wire;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.live.client.InternalLiveClient;
import present.proto.ActivityType;
import present.proto.CitiesResponse;
import present.proto.CommentRequest;
import present.proto.CommentResponse;
import present.proto.CountGroupReferralsResponse;
import present.proto.DeleteCommentRequest;
import present.proto.DeleteGroupRequest;
import present.proto.DirectGroupRequest;
import present.proto.DirectGroupsResponse;
import present.proto.DispatchCommentRequest;
import present.proto.Empty;
import present.proto.ExploreHtmlRequest;
import present.proto.FeedEntry;
import present.proto.FeedHtmlRequest;
import present.proto.FeedRequest;
import present.proto.FeedResponse;
import present.proto.FindLiveServerRequest;
import present.proto.FindLiveServerResponse;
import present.proto.FlagCommentRequest;
import present.proto.FlagGroupRequest;
import present.proto.GroupComments;
import present.proto.GroupLog;
import present.proto.GroupMemberPreapproval;
import present.proto.GroupMembersRequest;
import present.proto.GroupMembersResponse;
import present.proto.GroupMembershipState;
import present.proto.GroupReferralResponse;
import present.proto.GroupReferralsResponse;
import present.proto.GroupRequest;
import present.proto.GroupResponse;
import present.proto.GroupService;
import present.proto.HtmlResponse;
import present.proto.InviteFriendsRequest;
import present.proto.JoinGroupRequest;
import present.proto.JoinGroupResponse;
import present.proto.JoinedGroupsRequest;
import present.proto.JoinedGroupsResponse;
import present.proto.LeaveGroupRequest;
import present.proto.LiveService;
import present.proto.MarkReadRequest;
import present.proto.MembersRequest;
import present.proto.MembershipRequest;
import present.proto.MembershipRequestsRequest;
import present.proto.MembershipRequestsResponse;
import present.proto.MuteGroupRequest;
import present.proto.MutedGroupsResponse;
import present.proto.NearbyGroupsRequest;
import present.proto.NearbyGroupsResponse;
import present.proto.PastCommentsRequest;
import present.proto.PastCommentsResponse;
import present.proto.Platform;
import present.proto.PutCommentRequest;
import present.proto.PutGroupRequest;
import present.proto.PutGroupResponse;
import present.proto.ReassignGroupRequest;
import present.proto.RequestHeader;
import present.proto.SlackAttachment;
import present.proto.UnmuteGroupRequest;
import present.proto.UnreadState;
import present.server.email.PresentEmail;
import present.server.model.Space;
import present.server.model.activity.Event;
import present.server.model.activity.GroupReferral;
import present.server.model.activity.GroupReferrals;
import present.server.model.comment.Comment;
import present.server.model.comment.GroupView;
import present.server.model.comment.GroupViews;
import present.server.model.comment.Comments;
import present.server.model.content.Content;
import present.server.model.group.Category;
import present.server.model.group.Group;
import present.server.model.group.GroupMembership;
import present.server.model.group.GroupRanker;
import present.server.model.group.GroupSearch;
import present.server.model.group.Groups;
import present.server.model.group.RankedGroup;
import present.server.model.group.Schedule;
import present.server.model.log.DatastoreOperation;
import present.server.model.user.BlockedUsersFilter;
import present.server.model.user.Clients;
import present.server.model.user.Feature;
import present.server.model.user.PresentAdmins;
import present.server.model.user.Privileges;
import present.server.model.user.UnreadStates;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.model.util.BloomFilters;
import present.server.model.util.Coordinates;
import present.server.model.util.Operations;
import present.server.model.util.SuggestedLocation;
import present.server.notification.CommentNotifications;
import present.server.notification.Notification;
import present.server.notification.Notifier;
import present.server.slack.AnnounceToSlack;
import present.server.slack.SlackClient;
import present.wire.rpc.core.ClientException;
import present.wire.rpc.core.ServerException;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.stream.Collectors.toList;
import static present.server.Protos.EMPTY;
import static present.server.environment.Environment.isProduction;
import static present.server.model.PresentEntities.expected;

/**
 * App Engine implementation of GroupService. Our API and server implementation differ in their
 * use of the term "Group." In the API, "group" refers to both circles and direct groups. In
 * our server implementation, "Group" refers to circles only, while "Chat" implements
 * direct groups.
 *
 * @author Bob Lee (bob@present.co)
 * @author Pat Niemeyer (pat@pat.net)
 */
public class AppEngineGroupService implements GroupService {

  private static final Logger logger = LoggerFactory.getLogger(AppEngineGroupService.class);

  @Override public NearbyGroupsResponse getNearbyGroups(NearbyGroupsRequest request) {
    User user = Users.current(false);
    Iterable<String> mutedGroups = Groups.getMutedGroups(user);
    List<Group> groups = GroupSearch
        .near(location(request.location))
        .space(Space.selected(request.spaceId))
        .using(GroupRanker.EXPLORE)
        .limit(150)
        .run()
        .stream()
        .map(RankedGroup::group)
        .collect(toList());
    Stopwatch sw = Stopwatch.createStarted();
    List<GroupResponse> responses = groups.stream()
        .map(group -> group.toResponseFor(user))
        .collect(toList());
    logger.info("Converted groups to responses in {}.", sw);
    return new NearbyGroupsResponse(responses,
        new MutedGroupsResponse(Lists.newArrayList(mutedGroups)));
  }

  /** Returns the selected location or the actual location. */
  private static Coordinates location() {
    return location(null);
  }

  /**
   * Returns the selected location, the actual location, or the provided location, in that order.
   */
  private static Coordinates location(present.proto.Coordinates defaultLocation) {
    RequestHeader requestHeader = RequestHeaders.current();
    Coordinates location = Operations.firstIfNotNull(
        Coordinates.fromProto(requestHeader.selectedLocation),
        Coordinates.fromProto(requestHeader.location),
        Coordinates.fromProto(defaultLocation)
    );
    if (location == null) {
      throw new ClientException("Missing location in request headers.");
    }
    return location;
  }

  @Override public GroupResponse getGroup(GroupRequest request) {
    User user = Users.current(false);
    Group group = (Group) Groups.findByUuid(request.groupId);
    return group.toResponseFor(user);
  }

  @Override public PutGroupResponse putGroup(PutGroupRequest request) {
    User user = Users.current();

    // Load or create the group by uuid
    @Nullable Group group = Groups.findByUuid(request.uuid);

    // Is the group new?
    boolean isNew = group == null;
    if (isNew) {
      logger.debug("New group.");
      logger.info("New group.");
      group = new Group();
      group.id = request.uuid;
      Space space = Space.selected(request.spaceId);
      if (space == null) {
        // Legacy client?
        space = Space.EVERYONE;
      }
      group.spaceId = space.id;
      user.checkContentAccess(group);
      group.owner = Ref.create(user);
      group.setShortId();
      group.createdFrom = new Coordinates(request.createdFrom);
    } else {
      logger.debug("Update existing group.");
      logger.info("Update existing group.");
      Privileges.assertUserOwns(group);
    }

    // Server should not allow categories not in the defined Category enum, unless user is an admin.
    List<String> inputCategories = request.categories.stream()
        // Ignore dynamic categories if the client passes them back.
        .filter(c -> !Category.isDynamic(c))
        .collect(toList());
    List<String> categories = new ArrayList<>();
    for (String c : inputCategories) {
      // Map legacy categories to current ones.
      c = Category.map(c);
      categories.add(c);
      if (!Category.isValid(c)) {
        // If user is an admin, allow new category creation.
        if (!user.isAdmin()) {
          throw new ServerException("Invalid category: " + c);
        }
      }
    }

    // Update remaining fields
    group.title = request.title.trim();
    group.setLocation(Coordinates.fromProto(request.location));
    group.locationName = request.locationName;
    group.schedule = request.schedule == null ? null : new Schedule(request.schedule);
    group.description = request.description;
    if (request.preapprove != null) group.preapprove = request.preapprove;
    if (request.discoverable != null) group.discoverable = request.discoverable;

    if (user.isAdmin() && Clients.current().platform() != Platform.WEB) {
      // We only support custom categories in web right now. If an admin edits from iOS or Android,
      // retain the custom categories.
      group.categories = Streams.concat(
          // Retain custom categories
          group.categories.stream().filter(c -> !Category.isValid(c)).filter(Objects::nonNull),
          // Update standard categories
          categories.stream()
      ).distinct().collect(toList());
    } else {
      group.categories = categories;
    }

    group.lastUpdateMonth = Time.epochMonth();

    if (request.suggestedLocation != null) {
      group.suggestedLocation = new SuggestedLocation(request.suggestedLocation);
    }

    // Update the cover content ref or remove it
    group.coverContent = request.cover == null
        ? null : Content.refFor(request.cover.uuid);

    if (isNew) {
      // Automatically join groups that the user creates
      user.join(group);

      AnnounceToSlack.newGroup(group);
      PresentAdmins.newGroup(group);

      // Notify nearby friends.
      if (group.discoverable) {
        List<User> nearbyFriends = user.friendsNear(group.location.toS2LatLng());
        if (!nearbyFriends.isEmpty()) {
          Notifier notifier = Notifier.create().to(nearbyFriends);
          String message = user.firstName + " started '" + group.title + "'";
          notifier.send(new Notification()
              .body(message)
              .put(group)
              .event(ActivityType.USER_INVITED_TO_GROUP, message, group.getRef()));
        } else {
          logger.info("No friends found nearby.");
        }
      }
    } else {
      group.save();
    }

    DatastoreOperation.log(group);

    return new PutGroupResponse(group.toResponseFor(user));
  }

  private static void checkContentAccess(Group container) {
    if (container instanceof Group) {
      Group group = container;
      User user = Users.current(false);
      group.checkContentAccess(user);
    }
  }

  @Override public CommentResponse getComment(CommentRequest request) {
    Comment comment = Comments.findByUuid(request.commentId);
    if (comment == null) {
      throw new ClientException("Comment not found: " + request.commentId);
    }
    Group container = comment.group.get();
    checkContentAccess(container);
    return comment.toResponse();
  }

  @Override public PastCommentsResponse getPastComments(PastCommentsRequest request) {
    User user = Users.current(false);
    Group container = Groups.findByUuid(request.groupId);
    checkContentAccess(container);
    Iterable<Comment> comments = Comments.getComments(container, user, true);
    List<CommentResponse> responses = Streams.stream(comments)
        .map(Comments::toResponse)
        .collect(toList());
    return new PastCommentsResponse(responses);
  }

  @Override public Empty putComment(PutCommentRequest request) {
    // TODO: Support direct group comments.
    checkContentAccess(Groups.findByUuid(request.groupId));
    SendGroupComment putComment = new SendGroupComment(request,
        Coordinates.fromProto(RequestHeaders.current().location));
    putComment.run();
    return Protos.EMPTY;
  }

  /**
   * Post a test comment from the generic admin user.
   */
  public void postTestComment(String title, String comment) {
    ArrayList<Group> groups = Lists.newArrayList(Groups.findByTitle(title));
    if (groups.size() == 0) {
      throw new ClientException("No group with title found:" + title);
    }
    if (groups.size() > 1) {
      throw new ClientException("More than one group with title found: " + title);
    }
    Group group = groups.get(0);
    Users.runAsGenericAdminUserRequest(() -> putComment(new PutCommentRequest(
        Uuids.newUuid(), group.uuid(), comment, null, null)));
  }

  @Override public Empty flagGroup(FlagGroupRequest fgr) {
    logger.info(String.format("ABUSE REPORT: client: %s, groupId: %s", Users.current(false).uuid, fgr.groupId));
    User user = Users.current();
    Group container = Groups.findByUuid(fgr.groupId);
    if (container instanceof Group) {
      Group group = (Group) container;
      User owner = group.owner.get();
      String text = String.format("Reported group: <%s|%s> owned by: %s",
          group.consoleUrl(), group.title, SlackClient.link(owner))
          + reportingUserInfo(user, fgr.customReason);
      List<SlackAttachment> attachments = new ArrayList<>();
      if (group.hasCoverContent()) {
        attachments.add(new SlackAttachment("Group Image", group.coverContent.get().url()));
      }
      attachments.add(new SlackAttachment("Group Location", group.mapImageUrl(600, 600)));
      SlackClient.post(SlackClient.reportBuilder()
          .text(text)
          .attachments(attachments)
          .build());
    } else {
      throw new UnsupportedOperationException("We don't support flagging direct groups yet.");
    }
    return new Empty();
  }

  @Override public Empty flagComment(FlagCommentRequest fcr) {
    User user = Users.current();
    logger.info(String.format("ABUSE REPORT: user: %s, commentId: %s", user.uuid, fcr.commentId));
    Comment comment = expected(Comments.findByUuid(fcr.commentId), fcr.commentId);
    User author = comment.author.get();
    String text = String.format("Reported comment: <%s|%s>, authored by: %s",
        comment.consoleUrl(), comment.text, SlackClient.link(author))
        + reportingUserInfo(user, fcr.customReason);
    List<SlackAttachment> attachments = new ArrayList<>();
    if (comment.hasContent()) {
      attachments.add(new SlackAttachment("Comment Content", comment.contentRef.get().url()));
    }
    SlackClient.post(SlackClient.reportBuilder()
        .text(text)
        .attachments(attachments)
        .build());
    return new Empty();
  }

  private static String reportingUserInfo(User reportedByUser, String reason) {
    String report = "";
    if (reportedByUser != null) {
      report = report + String.format("\nReported by user: %s", SlackClient.link(reportedByUser));
    }
    if (reason != null) {
      report = report + String.format("\nReason: %s", reason);
    }
    return report;
  }

  @Override public Empty deleteGroup(DeleteGroupRequest deleteGroupRequest) {
    Group container = Groups.findByUuid(deleteGroupRequest.groupId);
    container.delete();
    return new Empty();
  }

  @Override public Empty deleteComment(DeleteCommentRequest request) {
    String commentId = request.commentId;
    Comment comment = Comments.findByUuid(commentId);
    if (comment == null) throw new ClientException("Not found: " + commentId);
    Privileges.assertUserOwns(comment);

    Group group = comment.group.get();

    group.delete(comment);

    // Delete comment from feed if included. We have to run this outside of the transaction
    // as it queries for duplicate comments across groups.
    if (comment.getRef().equals(group.lastSignificantComment)) {
      logger.info("Significant comment deleted.");
      group.updateLastSignificantComment();
    }

    // Delete events associated with comment
    List<Event> events = ofy().load().type(Event.class)
        .filter(Event.Fields.defaultTarget.name(), comment.getRef())
        .list()
        .stream()
        .filter(e -> e.type == ActivityType.USER_COMMENTED_ON_GROUP)
        .collect(Collectors.toList());
    if (!events.isEmpty()) ofy().delete().entities(events);

    // Dispatch deletion via Live Server.
    CommentResponse deleted = comment.toResponse().newBuilder()
        .deleted(true)
        .content(null)
        .comment("*deleted*")
        .build();
    try {
      dispatchComment(Users.current(), comment.group.get().uuid(), deleted).join();
    } catch (InterruptedException e) {
      logger.warn("Dispatching deletion interrupted.", e);
    }

    group.log(GroupLog.Entry.Type.DELETE_COMMENT);

    return new Empty();
  }

  @Override public Empty muteGroup(MuteGroupRequest request) {
    GroupView view = Groups.findByUuid(request.groupId)
        .getOrCreateView(Users.current());
    view.mute().save();
    return EMPTY;
  }

  @Override public Empty unMuteGroup(UnmuteGroupRequest request) {
    GroupView view = Groups.findByUuid(request.groupId)
        .getOrCreateView(Users.current());
    view.unmute().save();
    return EMPTY;
  }

  @Override public FindLiveServerResponse findLiveServer(FindLiveServerRequest request) {
    Group container = Groups.findByUuid(request.groupId);
    if (container instanceof Group) {
      Group group = (Group) container;
      User current = Users.current(false);
      group.checkContentAccess(current);
      if (current != null) group.log(GroupLog.Entry.Type.OPEN);
    }
    return findLiveServer(request.groupId);
  }

  private FindLiveServerResponse findLiveServer(String groupId) {
    // Port comes from LiveServer.DEFAULT_PORT.
    if (isProduction()) return new FindLiveServerResponse("live.present.co", 8888);
    //if (isDevelopment()) return new FindLiveServerResponse("local.present.co", 8888);
    return new FindLiveServerResponse("live.staging.present.co", 8888);
  }

  @Override public JoinedGroupsResponse getJoinedGroups(JoinedGroupsRequest request) {
    String userId = request.userId;
    User current = Users.current();
    User user = userId != null ? User.get(userId) : current;
    Iterable<String> mutedGroups = null;
    boolean sameUser = current == user;

    // Query saved groups and container views in parallel.
    if (sameUser) {
      // Only load a user's own muted groups
      mutedGroups = Groups.getMutedGroups(current);
    }

    List<Group> joinedGroups = user.joinedGroups();
    if (!joinedGroups.isEmpty()) {
      // Load group views into session cache.
      GroupViews.viewsFor(current, joinedGroups);
      // Show non-discoverable groups?
      boolean showAll = sameUser || current.isAdmin();
      // Show private groups if the user is already a member or the client supports them.
      boolean showPrivate = sameUser || Clients.current().supports(Feature.PRIVATE_GROUPS);
      List<GroupResponse> groups = joinedGroups.stream()
          .filter(Objects::nonNull)
          .filter(g -> showAll || g.discoverable)
          .filter(g -> showPrivate || g.preapprove == GroupMemberPreapproval.ANYONE)
          .filter(g -> !g.isDeleted())
          // TODO: Hide private groups the current user shouldn't see.
          // Men shouldn't see circles in the women-only space.
          .filter(g -> current.canAccess(g.space()))
          .filter(g -> current.canSee(g.owner.get()))
          .map(group -> group.toResponseFor(current))
          .sorted((g1, g2) -> Long.compare(g2.lastCommentTime, g1.lastCommentTime))
          .collect(toList());
      if (sameUser && Clients.current().platform() == Platform.IOS) {
        // Since we just loaded most of what we need, refresh the user's unread counts.
        current.computeUnreadState();
      }
      return new JoinedGroupsResponse(groups,
          mutedGroups == null ? null : new MutedGroupsResponse(Lists.newArrayList(mutedGroups)));
    } else {
      return new JoinedGroupsResponse(Collections.emptyList(),
          new MutedGroupsResponse(Collections.emptyList()));
    }
  }

  @Override public JoinGroupResponse joinGroup(JoinGroupRequest request) {
    User user = Users.current();
    Group group = Groups.findByUuid(request.groupId);
    if (group == null) throw new ClientException("Not found");
    if (group.isDeleted()) {
      if (Wire.get(request.ignoreDeletions, false)) {
        return new JoinGroupResponse(GroupMembershipState.NONE);
      } else {
        throw new ClientException("Can't join deleted group.");
      }
    }
    return new JoinGroupResponse(group.join(user));
  }

  @Override public Empty leaveGroup(LeaveGroupRequest request) {
    User user = Users.current();
    Group group = Groups.findByUuid(request.groupId);
    group.leave(user);
    return Protos.EMPTY;
  }

  @Override public Empty addMembers(MembersRequest request) throws IOException {
    User host = Users.current();
    Group group = Groups.findByUuid(request.groupId);
    group.addMembers(host, usersFrom(request));
    return Protos.EMPTY;
  }

  @Override public Empty removeMembers(MembersRequest request) throws IOException {
    User host = Users.current();
    Group group = Groups.findByUuid(request.groupId);
    group.removeMembers(host, usersFrom(request));
    return Protos.EMPTY;
  }

  private static Iterable<User> usersFrom(MembersRequest request) {
    Map<Key<User>, User> byId = Users.load(request.userIds);
    Map<String, User> byPhone = Users.getOrCreateByPhone(request.phoneNumbers);
    return Iterables.concat(byId.values(), byPhone.values());
  }

  @Override
  public MembershipRequestsResponse getMembershipRequests(MembershipRequestsRequest request) {
    Group group = Groups.findByUuid(request.groupId);

    User host = Users.current();
    if (!host.isAdmin() && !group.isMember(host)) {
      throw new ClientException("Not authorized.");
    }

    List<MembershipRequest> requests = GroupMembership.requestsFor(group)
        .stream()
        .map(GroupMembership::toMembershipRequest)
        .collect(toList());
    return new MembershipRequestsResponse(requests);
  }

  @Override public GroupMembersResponse getGroupMembers(GroupMembersRequest request) {
    // TODO: Handle direct groups.
    return Users.allowInvisibleUsers(() ->
        new GroupMembersResponse(Groups.findByUuid(request.groupId).getMembersResponse(100))
    );
  }

  @Override public GroupResponse getDirectGroup(DirectGroupRequest request) {
    throw new UnsupportedOperationException("Implement!");
  }

  @Override public DirectGroupsResponse getDirectGroups(Empty request) {
    throw new UnsupportedOperationException("Implement!");
  }

  /** Sends a comment to the datastore and a live server concurrently. */
  private class SendGroupComment {

    private final PutCommentRequest request;
    private final Coordinates location;
    private final User author;
    private final String commentId;
    private Thread dispatchComment;
    private Comment comment;
    private Group group;

    private SendGroupComment(PutCommentRequest request, Coordinates location) {
      this.request = request;
      this.commentId = request.uuid;
      this.location = location;
      this.author = Users.current();
    }

    public void run() {
      this.group = Groups.findActiveByUuid(request.groupId);

      // Kick off queries necessary to send notifications.
      Notifier notifier;
      if (request.ignoreMuting == Boolean.TRUE) { // could be null
        if (!author.isAdmin()) throw new ClientException("Admins only");
        logger.info("Ignoring muting.");
        notifier = Notifier.from(author).toKeys(group.memberKeys());
      } else {
        notifier = group.notifierFrom(author);
      }

      // Automatically save (join) the group if needed when the users posts.
      // TODO: Send join notification, too? We should at least add an event.
      author.join(group);

      // Determine if comment with the same text already exists.
      boolean exists = Comments.textExists(request.comment);

      // Save the comment.
      boolean newComment = ofy().transact(() -> this.saveComment(exists));

      // If this is a duplicate request, don't re-send notifications.
      if (!newComment) return;

      // TODO: Move this into Notifier.
      this.dispatchComment = dispatchComment(author, request.groupId, Comments.toResponse(comment));

      // Record the comment in the group log.
      group.log(GroupLog.Entry.Type.COMMENT);

      // Increment badge counts.
      notifier.unreadStates().markGroupUnread(group.uuid());

      // Send the notification.
      notifier.send(CommentNotifications.create(comment));

      try {
        this.dispatchComment.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    /** Saved the comment. Returns true if the comment was created. */
    private boolean saveComment(boolean exists) {

      // Load the group again within the transaction because we are going to increment its sequence
      this.group = group.reload();

      // Create the new comment
      Key<Comment> key = Key.create(group.getKey(), Comment.class, commentId);
      comment = ofy().load().key(key).now();

      if (comment != null) {
        logger.info("Comment already stored: " + commentId);
        return false;
      }

      // Create a new comment.
      comment = new Comment();
      comment.group = Ref.create(group);
      comment.uuid = commentId;
      comment.author = Ref.create(this.author);
      comment.text = request.comment;
      comment.location = this.location;

      if (request.content != null) comment.contentRef = Content.refFor(request.content);

      comment.sequence = group.nextCommentIndex();
      group.activeComments++;

      // Move the group to the current month's bucket if necessary.
      group.lastUpdateMonth = Time.epochMonth();
      if (comment.isSignificant(exists)) {
        group.lastSignificantComment = comment.getRef();
      }

      ofy().save().entities(comment, group);

      // Mark the message we just sent as read.
      group.getOrCreateView(this.author).markAsRead().save();

      return true;
    }
  }

  /** Sends this comment to a live server. */
  private Thread dispatchComment(final User author, final String groupId,
      final CommentResponse comment) {
    final String clientUuid = RequestHeaders.current().clientUuid; // depends on thread context

    // Load the blocked user bloom filter for this author, if any
    final @Nullable BloomFilter<Ref<User>> blockedUsersFilter =
        BlockedUsersFilter.getFilterFor(author);

    Thread thread = ThreadManager.createThreadForCurrentRequest(() -> {
      try {
        FindLiveServerResponse flsr = findLiveServer(groupId);
        String url = "https://" + flsr.host + ":" + flsr.port + "/api";
        LiveService liveService = InternalLiveClient.connectTo(url);
        ByteString encodedComment = ByteString.of(CommentResponse.ADAPTER.encode(comment));
        @Nullable ByteString encodedFilter = BloomFilters.toByteString(blockedUsersFilter);
        liveService.dispatchComment(
            new DispatchCommentRequest(clientUuid, groupId, encodedComment, encodedFilter));
      } catch (Exception e) {
        logger.error("Error dispatching comment.", e);
      }
    });
    thread.start();
    return thread;
  }

  @Override public Empty inviteFriends(InviteFriendsRequest request) throws IOException {
    User current = Users.current();

    // TODO: Make sure these are friends.
    List<Key<User>> userKeys =
        request.userIds.stream().map(id -> Key.create(User.class, id)).collect(toList());

    Notifier notifier = Notifier.create().toKeys(userKeys);

    Iterable<User> recipients = notifier.recipients();

    Group group = Groups.findByUuid(request.groupId);

    // TODO: Should we require the user to be a member?

    group.log(GroupLog.Entry.Type.INVITE);

    // Create group referral
    for (User recipient : recipients) {
      if (!current.equals(recipient)) {
        GroupReferrals.getOrCreate(current, recipient, group);
      }
    }

    // Send email
    new PresentEmail.Builder()
        .type(PresentEmail.Type.FRIEND_INVITED_YOU_TO_GROUP)
        .user(current)
        .group(group)
        .build()
        .sendTo(recipients);

    // Send notifications
    String message = current.firstName + " invited you to join";
    notifier.send(new Notification()
        .title(group.title)
        .body(message)
        .put(group)
        .event(ActivityType.USER_INVITED_TO_GROUP, message + " '" + group.title + "'",
            group.getRef()));

    return Protos.EMPTY;
  }

  @Override public Empty markRead(MarkReadRequest request) {
    User user = Users.current();
    Group group = Groups.findByUuid(request.groupId);
    GroupView view = group.getOrCreateView(user);
    // The index of the last read comment should only move forward.
    int lastRead = view.lastRead();
    lastRead = Math.max(lastRead, request.lastRead);
    view.lastRead = lastRead;
    view.save().now();
    if (lastRead == group.lastCommentIndex()) {
      // The group is fully read.
      // If the user is the owner and the group has pending join requests, keep it unread.
      if (!user.equals(group.owner.get()) || group.joinRequests <= 0) {
        UnreadState state = UnreadStates.markGroupRead(user, request.groupId);
        Notifier.sendBadgeCounts(user, state);
      }
    }
    return Protos.EMPTY;
  }

  @Override public CitiesResponse getCities(Empty request) {
    return new CitiesResponse(Cities.all());
  }

  @Override public CountGroupReferralsResponse countGroupReferrals(Empty request) {
    int referred = (int) GroupReferrals.countReferrals(Users.current());
    return new CountGroupReferralsResponse(referred);
  }

  @Override public GroupReferralsResponse getGroupReferrals(Empty request) {
    List<GroupReferral> referrals = GroupReferrals.getReferrals(Users.current());
    List<GroupReferralResponse> referralResponses = referrals.stream()
        .map(g -> new GroupReferralResponse(g.to().toResponse(), g.group().toResponseFor(Users.current()))).collect(
            toList());
    return new GroupReferralsResponse(referralResponses);
  }

  @Override public GroupResponse reassignGroup(ReassignGroupRequest request) {
    User user = Users.current();
    if (!user.isAdmin()) throw new ClientException("Not allowed.");
    Group group = Groups.findByUuid(request.groupId);
    User owner = User.get(request.ownerId);
    return ofy().transact(() -> {
      Group latest = group.reload();
      latest.owner = owner.getRef();
      latest.save();
      owner.join(group);
      return latest;
    }).toResponseFor(user);
  }

  @Override public HtmlResponse getExploreHtml(ExploreHtmlRequest request) {
    return GroupsHtml.getHome(Space.selected(request.spaceId), location());
  }

  @Override public HtmlResponse getFeedHtml(FeedHtmlRequest request) {
    return GroupsHtml.getFeedHtml(Space.selected(request.spaceId), location());
  }

  @Override public FeedResponse getFeed(FeedRequest request) throws IOException {
    User user = Users.current(false);
    List<FeedEntry> entries = GroupSearch
        .near(location())
        .space(Space.selected(request.spaceId))
        .using(GroupRanker.NEARBY_FEED_BY_TIME)
        .limit(100)
        .run()
        .stream()
        .map(RankedGroup::group)
        .map(g -> toEntry(user, g))
        .collect(toList());
    return new FeedResponse(entries);
  }

  private static FeedEntry toEntry(User user, Group group) {
    GroupResponse groupResponse = group.toResponseFor(user);

    if (group.lastSignificantComment == null) {
      return new FeedEntry.Builder().groupCreation(groupResponse).build();
    }

    // We currently return just the last comment. In the future, we can return multiple comments.
    List<CommentResponse> comments = Collections.singletonList(
        group.lastSignificantComment.get().toResponse());
    GroupComments groupComments = new GroupComments(groupResponse, comments);
    return new FeedEntry.Builder().groupComments(groupComments).build();
  }
}
