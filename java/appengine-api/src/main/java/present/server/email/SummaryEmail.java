package present.server.email;

import com.github.mustachejava.Mustache;
import com.google.common.base.MoreObjects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.ActivityType;
import present.proto.EmailRequest;
import present.server.Mustaches;
import present.server.Time;
import present.server.environment.Environment;
import present.server.model.activity.Event;
import present.server.model.comment.GroupView;
import present.server.model.comment.GroupViews;
import present.server.model.group.Group;
import present.server.model.user.User;

import static present.server.Time.epochWeek;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Summarizes activity for a user.
 *
 * @author Bob Lee (bob@present.co)
 */
public class SummaryEmail {

  private static final Logger logger = LoggerFactory.getLogger(SummaryEmail.class);

  private static final Mustache template = Mustaches.compileResource("/new-summary-inline.html");

  static final String PLACEHOLDER_IMAGE = "https://"
      + (Environment.isProduction() ? "" : "staging.")
      + "present.co/email/img/no-photo@2x.png";

  // Generate an ID based on the date in case we accidentally send a duplicate or need to retry.
  private final String id = "Daily Summary: " + MonthDay.now();

  // Total number of summary entries allowed
  // Smaller size avoids clipping in certain email clients, keeps enqueued tasks below size limit
  private final int MAX_SECTION_ENTRIES = 10;
  private final int MAX_TOTAL_ENTRIES = 40;

  public final String subject;
  public final Quote quote;

  public final User user;

  // List of activity for each section
  public List<GroupSummary> groupSummaries;
  public List<FriendSummary> friendSummaries;
  public List<InviteSummary> inviteSummaries;
  public List<JoinSummary> joinSummaries;

  // Flags determining whether to show "See More" for each category
  public final boolean hasMoreGroupSummaries;
  public final boolean hasMoreFriendSummaries;
  public final boolean hasMoreInviteSummaries;
  public final boolean hasMoreJoinSummaries;

  public SummaryEmail(User user, long since) {
    this.subject = subjectForToday();
    this.quote = Quote.today();
    this.user = user;

    // Everything we need is in the activity feed!
    List<Event> events = ofy().load()
        .type(Event.class)
        .filter("user", user)
        .filter("createdTimeIndex >=", since)
        .list();

    Multimap<ActivityType, Event> byType = Multimaps.index(events, e -> e.type);
    this.friendSummaries = byType.get(ActivityType.FRIEND_JOINED_PRESENT).stream()
        .sorted((e1, e2) -> Long.compare(e2.createdTime, e1.createdTime))
        .map(e -> new FriendSummary(e))
        .collect(Collectors.toList());
    this.groupSummaries = groupSummaries(user);
    this.inviteSummaries = byType.get(ActivityType.USER_INVITED_TO_GROUP).stream()
        .sorted((e1, e2) -> Long.compare(e2.createdTime, e1.createdTime))
        .map(e -> new InviteSummary((Group) e.defaultTarget.get()))
        .collect(Collectors.toList());
    this.joinSummaries = joinSummaries(byType.get(ActivityType.USER_JOINED_GROUP));

    // Flags for displaying "show more" for each section
    this.hasMoreFriendSummaries = this.friendSummaries.size() > MAX_SECTION_ENTRIES;
    this.hasMoreInviteSummaries = this.inviteSummaries.size() > MAX_SECTION_ENTRIES;
    this.hasMoreJoinSummaries = this.joinSummaries.size() > MAX_SECTION_ENTRIES;

    // Cap list size
    if (this.hasMoreFriendSummaries) {
      this.friendSummaries = this.friendSummaries.subList(0, MAX_SECTION_ENTRIES);
    }
    if (this.hasMoreInviteSummaries) {
      this.inviteSummaries = this.inviteSummaries.subList(0, MAX_SECTION_ENTRIES);
    }
    if (this.hasMoreJoinSummaries) {
      this.joinSummaries = this.joinSummaries.subList(0, MAX_SECTION_ENTRIES);
    }

    // Cap group summaries last to allow them to fill remaining space in email
    this.hasMoreGroupSummaries = this.groupSummaries.size() > groupSummarySize();
    if (this.hasMoreGroupSummaries) {
      this.groupSummaries = this.groupSummaries.subList(0, groupSummarySize());
    }
  }

  public int groupSummarySize() {
    return MAX_TOTAL_ENTRIES - (friendSummaries.size() + inviteSummaries.size() + joinSummaries.size());
  }

  public SummaryEmail(User user) {
    this(user, lastActive(user));
  }

  /** Sends a summary email to the given user. */
  public static void sendTo(User user) {
    new SummaryEmail(user).send();
  }

  /** Returns a time up to a week ago since the user was last active. */
  private static long lastActive(User user) {
    long since = user.lastActiveTime;
    long oneWeekAgo = System.currentTimeMillis() - Time.WEEK_IN_MILLIS;
    return Math.max(since, oneWeekAgo);
  }

  private static List<GroupSummary> groupSummaries(User user) {
    List<Group> groups = user.joinedGroups();
    Map<Group, GroupView> views = GroupViews.viewsFor(user, groups);
    return views.entrySet().stream()
        .filter(e -> e.getValue().unreadCount() > 0)
        .sorted((e1, e2) -> Long.compare(e2.getKey().lastCommentTime, e1.getKey().lastCommentTime))
        .map(e -> new GroupSummary(e.getKey(), e.getValue().unreadCount()))
        .collect(Collectors.toList());
  }

  private static List<JoinSummary> joinSummaries(Collection<Event> joins) {
    Multiset<Group> byGroup = HashMultiset.create(
        Iterables.transform(joins, e -> (Group) e.defaultTarget.get()));
    return byGroup.entrySet().stream()
        .sorted((e1, e2) -> Long.compare(e2.getElement().updatedTime, e1.getElement().updatedTime))
        .map(e -> new JoinSummary(e.getElement(), e.getCount()))
        .collect(Collectors.toList());
  }

  // Methods used to render titles in summary email
  public boolean hasGroupSummaries() {
    return !groupSummaries.isEmpty();
  }

  public boolean hasFriendSummaries() {
    return !friendSummaries.isEmpty();
  }

  public boolean hasInviteSummaries() {
    return !inviteSummaries.isEmpty();
  }

  public boolean hasJoinSummaries() {
    return !joinSummaries.isEmpty();
  }

  public String toHtml() {
    return Mustaches.toString(template, this);
  }

  /** Sends a summary email to the associated user. */
  public void send() {
    if (user.email == null) {
      logger.info("Missing email address for {}.", user);
      return;
    }
    try {
      if (isEmpty()) {
        logger.info("Nothing to send.");
        return;
      }
      EmailRequest email = Emails.to(user, id)
          .subject(subject)
          .html(toHtml())
          .unsubscribeGroup(3823)
          .build();
      Emails.service.send(email);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String staging() {
    return Environment.isProduction() ? "" : "staging.";
  }

  private boolean isEmpty() {
    return groupSummaries.isEmpty()
      && joinSummaries.isEmpty()
      && inviteSummaries.isEmpty()
      && friendSummaries.isEmpty();
  }

  @Override public String toString() {
    long bytes = 0;
    try {
      bytes = this.toHtml().getBytes("UTF-8").length;
    } catch (UnsupportedEncodingException e) {
      logger.info(e.getMessage());
    }
    return MoreObjects.toStringHelper(this)
        .add("to", this.user)
        .add("email size (KB)", bytes/1024)
        .add("# friendSummaries", friendSummaries.size())
        .add("# groupSummaries", groupSummaries.size())
        .add("# inviteSummaries", inviteSummaries.size())
        .add("# joinSummaries", joinSummaries.size())
        .toString();
  }

  public static final List<DayOfWeek> EMAIL_DAYS = ImmutableList.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
  private static final int[] OFFSETS_BY_DAY = { 0 /* Monday */, 0, 1, 1, 2, 2, 2 };

  public static int indexForToday(int listSize) {
    long epochWeek = epochWeek();
    int base = (int) (epochWeek * EMAIL_DAYS.size()) % listSize;
    int offset = OFFSETS_BY_DAY[LocalDate.now().getDayOfWeek().getValue() - 1];
    return base + offset;
  }

  public static String subjectForToday() {
    return SUBJECTS.get(indexForToday(SUBJECTS.size()));
  }

  private static final List<String> SUBJECTS = ImmutableList.of(
      "Today On Present",
      "Hey Babe!",
      "Slay The Day",
      "Hey Girl, this one’s for you",
      "Need Some Motivation? We Got You",
      "Your daily dose of sparkle ✨",
      "We Got You Babe!",
      "Hey You! Here’s What’s New!",
      "Hey Doll!",
      "Hey Babe! We’re Here For You!",
      "Babes Unite 👯",
      "Here’s Some GRL PWR For Your Day",
      "Calling All Wonder Women!",
      "The Future is Female",
      "#BadassBabes 💪",
      "Badass Beauties, We Got You",
      "Your daily dose of Girl Power",
      "Keep Calm & Empower Women",
      "Feminist Forever… am I right ladies?",
      "Girrrrl Power!! 😍",
      "Sistas Unite & Fight",
      "Join our #GirlSquad",
      "Woman Warriors! We’re Here For You!",
      "Slay All Day 💅🏽",
      "Some Morning Motivation",
      "Ready to #Slay!",
      "Women are here to #slay",
      "Hey Phenomenal Women! 💜",
      "Where are my Woman Warriors at??",
      "Nasty Women Unite 👯",
      "We’re Feminists… What’s Your Superpower?",
      "Calling all Boss Babes! 💪",
      "Our Present is Female",
      "OMG - Start Your Day With Badass Babes",
      "Slay the day Ladies! 💅🏽",
      "Start Your Day the Present Way!",
      "Nasty Women Make (HER)story 💜",
      "Break That Glass Ceiling Today!",
      "Who Run The World? GIRLS!",
      "Fight Like a Girl Today 💪",
      "Girl Power, Today and Every Day",
      "Where Are Our Lady Bosses At?",
      "Go Out and Kick Some Ass Today! 💜",
      "Take a Stand With the #BadassBabes",
      "Slay in Your Lane 💅🏽",
      "Women, Rise Up!",
      "OMG Babe! We’ve Got You!",
      "Hello to the Wondrous Womankind!",
      "Hey Boss Babes! 💪",
      "Hey Ladies! Here’s What’s New!",
      "You Gotta Get With My Friends 👯"
  );
}
